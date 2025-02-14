package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.LatestDrug;
import it.unipi.EasyDrugServer.model.LatestPurchase;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Purchase;
import it.unipi.EasyDrugServer.repository.mongo.DoctorRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.utility.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final UserService userService;
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final PatientRepository patientRepository;
    private final PurchaseRepository purchaseRepository;
    private final DoctorRepository doctorRepository;
    private final int N_TO_VIEW = 5;

    public List<PrescriptionDTO> getAllActivePrescriptions(String patientCode){
        System.out.println("Tentativo di recupero prescrizioni per: " + patientCode);
        return prescriptionRedisRepository.getAllActivePrescriptions(patientCode);
    }

    public Patient getPatientById(String id) {
        Object obj = userService.getUserIfExists(id, UserType.PATIENT);
        return (Patient) obj;
    }

    public AccountPatientDTO getAccountPatientById(String id) {
        Patient patient = getPatientById(id);
        AccountPatientDTO accountPatientDTO = new AccountPatientDTO();
        accountPatientDTO.setId(patient.getId());
        accountPatientDTO.setName(patient.getName());
        accountPatientDTO.setSurname(patient.getSurname());
        accountPatientDTO.setCity(patient.getCity());
        accountPatientDTO.setDistrict(patient.getDistrict());
        accountPatientDTO.setRegion(patient.getRegion());
        accountPatientDTO.setDateOfBirth(patient.getDateOfBirth());
        accountPatientDTO.setGender(patient.getGender());
        accountPatientDTO.setTaxCode(patient.getTaxCode());
        accountPatientDTO.setDoctorCode(patient.getDoctorCode());
        return accountPatientDTO;
    }

    public AccountPatientDTO modifyPatient(AccountPatientDTO patient) {
        if(patientRepository.existsById(patient.getId())) {
            Patient patient_ = getPatientById(patient.getId());
            patient_.setDistrict(patient.getDistrict());
            patient_.setCity(patient.getCity());
            patient_.setRegion(patient.getRegion());
            if(doctorRepository.findById(patient.getDoctorCode()).isPresent())
                patient_.setDoctorCode(patient.getDoctorCode());
            else patient.setDoctorCode(patient_.getDoctorCode());
            if(patient.getPassword() != null) {
                String hash = PasswordHasher.hash(patient.getPassword());
                patient_.setPassword(hash);
                patient.setPassword(hash);
            }
            patientRepository.save(patient_);
            return patient;
        } else throw new NotFoundException("Patient "+patient.getId()+" does not exist");
    }

    public Patient deletePatient(String id) {
        Patient patient = (Patient) userService.getUserIfExists(id, UserType.PATIENT);
        patientRepository.deleteById(id);
        return patient;
    }

    public List<LatestPurchase> getLatestPurchases(String id) {
        return ((Patient) userService.getUserIfExists(id, UserType.PATIENT)).getLatestPurchasedDrugs();
    }

    public List<LatestPurchase> getNextPurchaseDrugs(String id_pat, int n_uploaded) {
        if(!patientRepository.existsById(id_pat))
            throw new NotFoundException("Patient "+id_pat+" does not exist");

        Optional<Patient> optPatient = patientRepository.findById(id_pat);
        List<String> purchasesId = new ArrayList<>();
        List<Purchase> purchases = new ArrayList<>();
        if(optPatient.isPresent()){
            purchasesId = optPatient.get().getPurchases();
        }

        int startIndex = purchasesId.size() - n_uploaded;
        if(startIndex <= 0)
            return new ArrayList<>();

        int endIndex = startIndex - N_TO_VIEW;
        if(endIndex < 0) endIndex = 0;
        // id of purchased drugs that interest us
        List<String> idToView = purchasesId.subList(endIndex, startIndex);

        for(String purchId: idToView){
            Optional<Purchase> optPurch = purchaseRepository.findById(purchId);
            optPurch.ifPresent(purchases::add);
        }

        // salvo tutti i farmaci prescritti, in ordine inverso, perché almeno l'utente li vede dal più recente al meno recente
        Map<LocalDateTime, LatestPurchase> hashPurchases = new LinkedHashMap<>();
        for(int i=purchases.size()-1; i>=0; i--){
            LatestDrug latestDrug = new LatestDrug();
            Purchase purch = purchases.get(i);

            latestDrug.setDrugId(purch.getDrugId());
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
        return new ArrayList<>(hashPurchases.values());
    }
}
