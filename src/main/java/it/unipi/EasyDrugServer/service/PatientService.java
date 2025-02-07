package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.dto.UserType;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.LatestDrug;
import it.unipi.EasyDrugServer.model.LatestPurchase;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Purchase;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.utility.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final UserService userService;
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final PatientRepository patientRepository;
    private final PurchaseRepository purchaseRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<PrescriptionDTO> getAllActivePrescriptions(String patientCode){
        return prescriptionRedisRepository.getAllActivePrescriptions(patientCode);
    }

    public Patient getPatientById(String id) {
        return (Patient) userService.getUserIfExists(id, UserType.PATIENT);
    }

    public Patient modifyPatient(Patient patient) {
        if(patientRepository.existsById(patient.getId())) {
            Patient patient_ = getPatientById(patient.getId());
            patient_.setDistrict(patient.getDistrict());
            patient_.setCity(patient.getCity());
            patient_.setRegion(patient.getRegion());
            patient_.setDoctorCode(patient.getDoctorCode());
            patient_.setPassword(PasswordHasher.hash(patient.getPassword()));
            patientRepository.save(patient_);
            return patient_;
        } else throw new NotFoundException("Patient "+patient.getId()+" does not exists");
    }

    public Patient deletePatient(String id) {
        Patient patient = (Patient) userService.getUserIfExists(id, UserType.PATIENT);
        patientRepository.deleteById(id);
        return patient;
    }

    public List<LatestPurchase> getLatestPurchases(String id) {
        return ((Patient) userService.getUserIfExists(id, UserType.PATIENT)).getLatestPurchasedDrugs();
    }

    public List<PurchaseDrugDTO> getNextPurchases(String id_pat, int lastViewedId) {
        if(!patientRepository.existsById(id_pat))
            throw new NotFoundException("Patient "+id_pat+" does not exist");

        Optional<Patient> optPatient = patientRepository.findById(id_pat);
        List<Integer> purchasesId = new ArrayList<>();
        List<Purchase> purchases = new ArrayList<>();
        if(optPatient.isPresent())
            purchasesId = optPatient.get().getPurchases();

        int startIndex = Math.max(0, lastViewedId - 10);
        int endIndex = Math.min(lastViewedId, purchasesId.size());
        List<Integer> idToView = purchasesId.subList(startIndex, endIndex);

        for(int purchId: idToView){
            Query query = new Query();
            query.addCriteria(
                    Criteria.where("id").is(purchId)
            );

            Purchase purchase = mongoTemplate.findOne(query, Purchase.class);
            Optional<Purchase> optPurch = Optional.ofNullable(purchase);
            if(optPurch.isPresent())
                purchases.add(optPurch.get());
        }

        // salvo tutti i farmaci prescritti, in ordine inverso, perché almeno l'utente li vede dal più recente al meno recente
        List<PurchaseDrugDTO> purchasedDrugs = new ArrayList<>();
        for(int i=purchases.size()-1; i>=0; i--){
            PurchaseDrugDTO purchasedDrugDTO = new PurchaseDrugDTO();
            purchasedDrugDTO.setId(purchases.get(i).getId());
            purchasedDrugDTO.setName(purchases.get(i).getName());
            purchasedDrugDTO.setQuantity(purchases.get(i).getQuantity());
            purchasedDrugDTO.setPrice(purchases.get(i).getPrice());
            purchasedDrugDTO.setPrescriptionTimestamp(purchases.get(i).getPrescriptionDate());
            purchasedDrugs.add(purchasedDrugDTO);
        }
        return purchasedDrugs;

        /*
        HashMap<LocalDateTime, List<LatestDrug>> hashPurchases = new HashMap<>();
        List<LatestPurchase> latestPurchases = new ArrayList<>();
        // Analizzare tutti gli acquisti e ottenere una hashmap con chiave timestamp di acquisto e
        // farmaci acquistati
        for(Purchase purch : purchases) {
            LatestDrug drug = new LatestDrug();
            drug.setDrugId(purch.getDrugId());
            drug.setDrugName(purch.getName());
            drug.setPrescriptionDate(purch.getPrescriptionDate());
            drug.setQuantity(purch.getQuantity());
            drug.setPrice(purch.getPrice());
            if(!hashPurchases.containsKey(purch.getPurchaseDate())){
                List<LatestDrug> latestDrugs = new ArrayList<>();
                latestDrugs.add(drug);
                hashPurchases.put(purch.getPurchaseDate(), latestDrugs);
            } else {
                hashPurchases.get(purch.getPurchaseDate()).add(drug);
            }
        }
        // Inserire tutti gli acquisti nell'ordine corretto come lo vuole il client
        for (HashMap.Entry<LocalDateTime, List<LatestDrug>> entry : hashPurchases.entrySet()) {
            LatestPurchase purchase = new LatestPurchase();
            purchase.setTimestamp(entry.getKey());
            purchase.setDrugs(entry.getValue());
            latestPurchases.add(purchase);
        }
        return latestPurchases;

         */
    }
}
