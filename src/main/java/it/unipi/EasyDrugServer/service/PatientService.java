package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.UserType;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.LatestDrug;
import it.unipi.EasyDrugServer.model.LatestPurchase;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.model.Purchase;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService extends UserService {
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final PatientRepository patientRepository;
    private final PurchaseRepository purchaseRepository;

    public List<PrescriptionDTO> getAllActivePrescriptions(String patientCode){
        return prescriptionRedisRepository.getAllActivePrescriptions(patientCode);
    }

    public Patient getPatientById(String id) {
        return (Patient) getUserIfExists(id, UserType.PATIENT);
    }

    public void modifyPatient(Patient patient) {
        if(patientRepository.existsById(patient.getIdentifyCode())) {
            patientRepository.save(patient);
        } else throw new NotFoundException("Patient "+patient.getIdentifyCode()+" does not exists");
    }

    public Patient deletePatient(String id) {
        Patient patient = (Patient) getUserIfExists(id, UserType.PATIENT);
        patientRepository.deleteById(id);
        return patient;
    }

    public List<LatestPurchase> getLatestPurchases(String id) {
        return ((Patient) getUserIfExists(id, UserType.PATIENT)).getLatestPurchased();
    }

    public List<LatestPurchase> getPurchasesFromTo(String id, LocalDate from, LocalDate to) {
        if(!patientRepository.existsById(id))
            throw new NotFoundException("Patient "+id+" does not exists");

        LocalDateTime fromTime = from.atStartOfDay();
        LocalDateTime toTime = to.atTime(23, 59, 59);
        List<Purchase> purchases = purchaseRepository.findByPatientCodeAndPurchaseDateBetween(id, fromTime, toTime);;
        HashMap<LocalDateTime, List<LatestDrug>> hashPurchases = new HashMap<>();
        List<LatestPurchase> latestPurchases = new ArrayList<>();
        // Analizzare tutti gli acquisti e ottenere una hashmap con chiave timestamp di acquisto e
        // farmaci acquistati
        for(Purchase purch : purchases) {
            LatestDrug drug = new LatestDrug();
            drug.setId(purch.getDrugId());
            drug.setName(purch.getDrugName());
            drug.setPrescribedTimestamp(purch.getPrescriptionTimestamp());
            drug.setQuantity(purch.getQuantity());
            drug.setPrice(purch.getPrice());
            if(!hashPurchases.containsKey(purch.getPurchaseTimestamp())){
                List<LatestDrug> latestDrugs = new ArrayList<>();
                latestDrugs.add(drug);
                hashPurchases.put(purch.getPurchaseTimestamp(), latestDrugs);
            } else {
                hashPurchases.get(purch.getPurchaseTimestamp()).add(drug);
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
    }
}
