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
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

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
    private final int N_TO_VIEW = 10;

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

    public List<LatestPurchase> getNextPurchaseDrugs(String id_pat, int nAlreadyViewed) {
        if(!patientRepository.existsById(id_pat))
            throw new NotFoundException("Patient "+id_pat+" does not exist");

        Optional<Patient> optPatient = patientRepository.findById(id_pat);
        List<ObjectId> purchasesId = new ArrayList<>();
        List<Purchase> purchases = new ArrayList<>();
        if(optPatient.isPresent())
            purchasesId = optPatient.get().getPurchases();

        int startIndex = purchasesId.size() - 1 - nAlreadyViewed;
        int endIndex = startIndex - N_TO_VIEW;
        // id of purchased drugs that interest us
        List<ObjectId> idToView = purchasesId.subList(endIndex, startIndex);

        for(ObjectId purchId: idToView){
            Optional<Purchase> optPurch = purchaseRepository.findById(purchId);
            optPurch.ifPresent(purchases::add);
        }

        // salvo tutti i farmaci prescritti, in ordine inverso, perché almeno l'utente li vede dal più recente al meno recente
        HashMap<LocalDateTime, LatestPurchase> hashPurchases = new HashMap<>();
        for(int i=purchases.size()-1; i>=0; i--){
            LatestDrug latestDrug = new LatestDrug();
            Purchase purch = purchases.get(i);
            latestDrug.setDrugId(purch.getId());
            latestDrug.setDrugName(purch.getName());
            latestDrug.setQuantity(purch.getQuantity());
            latestDrug.setPrice(purch.getPrice());
            latestDrug.setPrescriptionDate(purch.getPrescriptionDate());
            if(!hashPurchases.containsKey(purch.getPurchaseDate())){
                LatestPurchase latestPurchase = new LatestPurchase();
                List<LatestDrug> drugs = new ArrayList<>();
                drugs.add(latestDrug);
                latestPurchase.setTimestamp(purch.getPurchaseDate());
                latestPurchase.setDrugs(drugs);
                hashPurchases.put(purch.getPurchaseDate(), latestPurchase);
            } else
                hashPurchases.get(purch.getPurchaseDate()).getDrugs().add(latestDrug);
        }
        return (List<LatestPurchase>) hashPurchases.values();
    }
}
