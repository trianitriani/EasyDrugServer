package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PharmacyHomeDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.dto.UserType;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.*;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PharmacyRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Service
@RequiredArgsConstructor
public class PharmacyService extends UserService {
    private final PurchaseCartRedisRepository purchaseCartRedisRepository;
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PurchaseRepository purchaseRepository;
    private final PatientRepository patientRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public PharmacyHomeDTO viewPharmacyHome(String patCode) {
        PharmacyHomeDTO pharmacyHomeDTO = new PharmacyHomeDTO();
        // Mostra il carrello degli acquisti del paziente
        pharmacyHomeDTO.setPurchaseCart(purchaseCartRedisRepository.getPurchaseCart(patCode));
        // Mostra tutte le prescrizioni attive del paziente
        pharmacyHomeDTO.setPrescriptions(prescriptionRedisRepository.getAllActivePrescriptions(patCode));
        return pharmacyHomeDTO;
    }

    public PurchaseDrugDTO savePurchaseDrug(String patientCode, PurchaseDrugDTO drug) {
        if(Objects.equals(drug.getName(), ""))
            throw new BadRequestException("Name of the drug can not be null");
        if(drug.getQuantity() < 1)
            throw new BadRequestException("Quantity can not be lower than one");
        return purchaseCartRedisRepository.insertPurchaseDrug(patientCode, drug);
    }

    public PurchaseDrugDTO deletePurchaseDrug(String patientCode, int idDrug, LocalDateTime prescriptionTimestamp) {
        return purchaseCartRedisRepository.deletePurchaseDrug(patientCode, idDrug, String.valueOf(prescriptionTimestamp));
    }

    public PurchaseDrugDTO modifyPurchaseDrugQuantity(String patientCode, int idDrug, int quantity) {
        if(quantity == 0)
            return purchaseCartRedisRepository.deletePurchaseDrug(patientCode, idDrug, "");
        else if(quantity < 0)
            throw new BadRequestException("Quantity can not lower that zero.");
        return purchaseCartRedisRepository.modifyPurchaseDrugQuantity(patientCode, idDrug, quantity);
    }

    @Retryable(
            retryFor = {Exception.class}, // Ritenta in caso di errore generico
            maxAttempts = 3, // Massimo 3 tentativi
            backoff = @Backoff(delay = 2000) // Attesa di 2 secondi tra i tentativi
    )
    public List<PurchaseDrugDTO> confirmPurchaseCart(String patientCode, String pharmacyRegion) {
        List<PurchaseDrugDTO> purchasedDrugs = purchaseCartRedisRepository.confirmPurchaseCart(patientCode);
        List<Purchase> purchasesList = new ArrayList<>();
        LocalDateTime currentTimestamp = LocalDateTime.now();
        LatestPurchase latestPurchase = new LatestPurchase();

        for(PurchaseDrugDTO purchaseDrugDTO : purchasedDrugs){
            // creo, per ogni farmaco acquistato, il documento da inserire nella collezione purchases
            Purchase purchase = new Purchase();
            purchase.setDrugId(String.valueOf(purchaseDrugDTO.getId()));
            purchase.setName(purchaseDrugDTO.getName());
            purchase.setQuantity(purchaseDrugDTO.getQuantity());
            purchase.setPrice(purchaseDrugDTO.getPrice());
            purchase.setPrescriptionDate(purchaseDrugDTO.getPrescriptionTimestamp());
            purchase.setPatientCode(patientCode);
            purchase.setPurchaseDate(currentTimestamp);
            purchase.setRegion(pharmacyRegion);

            // inserisco nella collezione purchase il farmaco acquistato
            purchaseRepository.save(purchase);

            // creo, per ogni farmaco acquistato, il documento da inserire nella collezione patients
            LatestDrug latestDrug = new LatestDrug();
            latestDrug.setDrugId(String.valueOf(purchaseDrugDTO.getId()));
            latestDrug.setDrugName(purchaseDrugDTO.getName());
            latestDrug.setQuantity(purchaseDrugDTO.getQuantity());
            latestDrug.setPrice(purchaseDrugDTO.getPrice());
            latestDrug.setPrescriptionDate(purchaseDrugDTO.getPrescriptionTimestamp());

            latestPurchase.getDrugs().add(latestDrug);
        }

        latestPurchase.setTimestamp(currentTimestamp);

        // inserisco nella collezione patient il farmaco acquistato
        Query query = new Query(Criteria.where("identifyCode").is(patientCode));

        // Rimuove l'ultimo elemento e aggiunge il nuovo in prima posizione
        Update update = new Update()
                .pop("latestPurchasedDrugs", Update.Position.LAST) // Rimuove l'ultimo elemento
                .push("latestPurchasedDrugs").atPosition(0).value(latestPurchase); // Aggiunge in prima posizione

        mongoTemplate.updateFirst(query, update, Patient.class);

        return purchasedDrugs;
    }

    public Pharmacy getPharmacyById(String id) {
        return (Pharmacy) getUserIfExists(id, UserType.PHARMACY);
    }

    public void modifyPharmacy(Pharmacy pharmacy) {
        if(pharmacyRepository.existsById(pharmacy.getIdentifyCode())) {
            pharmacyRepository.save(pharmacy);
        } else throw new NotFoundException("Researcher "+pharmacy.getIdentifyCode()+" does not exists");
    }

    public Pharmacy deletePharmacy(String id) {
        Pharmacy pharmacy = getPharmacyById(id);
        pharmacyRepository.deleteById(id);
        return pharmacy;
    }

}
