package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.*;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PharmacyRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import it.unipi.EasyDrugServer.utility.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

@Service
@RequiredArgsConstructor
public class PharmacyService {
    private final UserService userService;
    private final PurchaseCartRedisRepository purchaseCartRedisRepository;
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PurchaseRepository purchaseRepository;
    private final PatientRepository patientRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public PharmacyHomeDTO viewPharmacyHome(String id_pat) {
        PharmacyHomeDTO pharmacyHomeDTO = new PharmacyHomeDTO();
        // Mostra il carrello degli acquisti del paziente
        pharmacyHomeDTO.setPurchaseCart(purchaseCartRedisRepository.getPurchaseCart(id_pat));
        // Mostra tutte le prescrizioni attive del paziente
        pharmacyHomeDTO.setPrescriptions(prescriptionRedisRepository.getAllActivePrescriptions(id_pat));
        return pharmacyHomeDTO;
    }

    public PurchaseCartDrugDTO savePurchaseDrug(String id_pat, PurchaseCartDrugDTO drug, List<String> alreadyInsertedIdDrugs) {
        if(Objects.equals(drug.getName(), ""))
            throw new BadRequestException("Name of the drug can not be null");
        if(drug.getQuantity() < 1)
            throw new BadRequestException("Quantity can not be lower than one");
        // se non deriva da una prescrizione controlliamo che non si trovi di già all'interno del carrello
        if (drug.getPrescriptionTimestamp() == null){
            for (String idDrug : alreadyInsertedIdDrugs){
                if(Objects.equals(idDrug, drug.getIdDrug()))
                    throw new ForbiddenException("Drug " + drug.getIdDrug() + " is already into the purchase cart");
            }
        }
        return purchaseCartRedisRepository.insertPurchaseDrug(id_pat, drug);
    }

    public PurchaseCartDrugDTO deletePurchaseDrug(String id_pat, int id_purch_drug) {
        return purchaseCartRedisRepository.deletePurchaseDrug(id_pat, id_purch_drug);
    }

    public PurchaseCartDrugDTO modifyPurchaseDrugQuantity(String id_pat, int id_purch_drug, int quantity) {
        if(quantity == 0)
            return purchaseCartRedisRepository.deletePurchaseDrug(id_pat, id_purch_drug);
        else if(quantity < 0)
            throw new BadRequestException("Quantity can not be lower that zero.");
        return purchaseCartRedisRepository.modifyPurchaseDrugQuantity(id_pat, id_purch_drug, quantity);
    }

    // funzione usata per inserire un acquisto di farmaci all'interno di mongo db
    private LatestPurchase insertPurchases(String patientCode, String pharmacyRegion, List<PurchaseCartDrugDTO> purchasedDrugs){
        List<ObjectId> prescribedDrugsId =  new ArrayList<>();
        List<ObjectId> purchaseDrugsId =  new ArrayList<>();
        LocalDateTime currentTimestamp = LocalDateTime.now();
        LatestPurchase latestPurchase = new LatestPurchase();

        for(PurchaseCartDrugDTO purchaseDrugDTO : purchasedDrugs){
            // creo, per ogni farmaco acquistato, il documento da inserire nella collezione purchases
            Purchase purchase = new Purchase();

            ObjectId objectIdDrug = new ObjectId(purchaseDrugDTO.getIdDrug());
            purchase.setDrugId(objectIdDrug);

            purchase.setName(purchaseDrugDTO.getName());
            purchase.setQuantity(purchaseDrugDTO.getQuantity());
            purchase.setPrice(purchaseDrugDTO.getPrice());
            purchase.setPrescriptionDate(purchaseDrugDTO.getPrescriptionTimestamp());
            purchase.setPurchaseDate(currentTimestamp);
            purchase.setRegion(pharmacyRegion);

            // inserisco nella collezione purchase il farmaco acquistato
            String idPurchase = purchaseRepository.save(purchase).getId();

            ObjectId purchObjectId = new ObjectId(idPurchase);
            purchaseDrugsId.add(purchObjectId);
            if(purchase.getPrescriptionDate() != null) prescribedDrugsId.add(purchObjectId);

            // creo, per ogni farmaco acquistato, il documento da inserire nella collezione patients
            LatestDrug latestDrug = new LatestDrug();

            ObjectId objectIdPurchase = new ObjectId(purchaseDrugDTO.getIdDrug());
            latestDrug.setDrugId(objectIdPurchase);

            latestDrug.setDrugName(purchaseDrugDTO.getName());
            latestDrug.setQuantity(purchaseDrugDTO.getQuantity());
            latestDrug.setPrice(purchaseDrugDTO.getPrice());
            latestDrug.setPrescriptionDate(purchaseDrugDTO.getPrescriptionTimestamp());
            latestPurchase.getDrugs().add(latestDrug);
        }

        latestPurchase.setTimestamp(currentTimestamp);

        // seleziono il paziente col codice in esame
        Query query = new Query(Criteria.where("_id").is(patientCode));

        // Rimuove l'ultimo elemento e aggiunge il nuovo in prima posizione se ha già 5 farmaci
        Optional<Patient> optPatient = patientRepository.findById(patientCode);
        Patient patient;
        int nDrugs = 0;

        if(optPatient.isPresent()) {
            patient = optPatient.get();

            System.out.println("Latest Purchased Drugs Before Update: " + patient.getLatestPurchasedDrugs());   // DEBUG

            nDrugs = patient.getLatestPurchasedDrugs().size();
        }

        if (nDrugs >= 5) {
            // PRIMO UPDATE: Rimuove l'ultimo elemento
            Update popUpdate = new Update().pop("latestPurchasedDrugs", Update.Position.LAST);
            System.out.println("Executing Update: " + popUpdate);  // DEBUG
            mongoTemplate.updateFirst(query, popUpdate, Patient.class);
        }

        Update pushUpdate = new Update().push("latestPurchasedDrugs").atPosition(0).value(latestPurchase);
        System.out.println("Executing Update: " + pushUpdate);  // DEBUG
        mongoTemplate.updateFirst(query, pushUpdate, Patient.class);

        // aggiorno le liste "purchases" e "prescriptions"
        Update updateLists = new Update()
                .push("purchases").each(purchaseDrugsId.toArray())
                .push("prescriptions").each(prescribedDrugsId.toArray());

        System.out.println("Executing Update: " + updateLists);  // DEBUG
        mongoTemplate.updateFirst(query, updateLists, Patient.class);
        return latestPurchase;
    }

    @Retryable(
            retryFor = { DataAccessException.class, TransactionSystemException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    @Transactional
    public LatestPurchase confirmPurchase(String id_pat, String pharmacyRegion) {
        Transaction transaction = null;
        try {
            ConfirmPurchaseCartDTO confirmPurchaseCartDTO = purchaseCartRedisRepository.confirmPurchaseCart(id_pat);
            List<PurchaseCartDrugDTO> purchasedDrugs = confirmPurchaseCartDTO.getPurchaseDrugs();

            // eseguiamo la transazione di MongoDB
            LatestPurchase latestPurchase = insertPurchases(id_pat, pharmacyRegion, purchasedDrugs);

            // eseguiamo la transazione di Redis
            transaction = confirmPurchaseCartDTO.getTransaction();
            List<Object> result = transaction.exec();
            if (result == null)
                throw new JedisException("Error in the transaction");

            return latestPurchase;
        } catch (JedisException e) {
            if (transaction != null)
                transaction.discard();
            throw new TransactionSystemException("Jedis error!");
        } catch(Exception e) {
            if (transaction != null)
                transaction.discard();
            throw e;
        }
    }

    public Pharmacy getPharmacyById(String id) {
        return (Pharmacy) userService.getUserIfExists(id, UserType.PHARMACY);
    }

    public Pharmacy modifyPharmacy(Pharmacy pharmacy) {
        if(pharmacyRepository.existsById(pharmacy.getId())) {
            Pharmacy pharmacy_ = getPharmacyById(pharmacy.getId());
            pharmacy_.setOwnerTaxCode(pharmacy.getOwnerTaxCode());
            if(pharmacy.getPassword() != null){
                String hash = PasswordHasher.hash(pharmacy.getPassword());
                pharmacy_.setPassword(hash);
            }
            pharmacyRepository.save(pharmacy_);
            return pharmacy_;
        } else throw new NotFoundException("Pharmacy "+pharmacy.getId()+" does not exist");
    }

    public Pharmacy deletePharmacy(String id) {
        Pharmacy pharmacy = getPharmacyById(id);
        pharmacyRepository.deleteById(id);
        return pharmacy;
    }

}
