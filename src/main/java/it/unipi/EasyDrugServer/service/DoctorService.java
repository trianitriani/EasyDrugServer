package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.SimplePatientDTO;
import it.unipi.EasyDrugServer.dto.UserType;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.exception.UnauthorizedException;
import it.unipi.EasyDrugServer.model.*;
import it.unipi.EasyDrugServer.repository.mongo.DoctorRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.utility.PasswordHasher;
import lombok.RequiredArgsConstructor;
// import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import it.unipi.EasyDrugServer.exception.BadRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DoctorService {
    private final UserService userService;
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final DoctorRepository doctorRepository;
    private final PurchaseRepository purchaseRepository;
    private final PatientRepository patientRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public PrescriptionDTO getInactivePrescription(String patientCode) {
        return prescriptionRedisRepository.getInactivePrescription(patientCode);
    }

    public PrescribedDrugDTO saveInactivePrescribedDrug(String patientCode, PrescribedDrugDTO drug) {
        if(Objects.equals(drug.getName(), ""))
            throw new BadRequestException("Name of the drug can not be null");
        if(drug.getQuantity() < 1)
            throw new BadRequestException("Quantity can not be lower than one");
        return prescriptionRedisRepository.insertInactivePrescribedDrug(patientCode, drug);
    }

    public PrescribedDrugDTO deleteInactivePrescribedDrug(String patientCode, int idDrug) {
        return prescriptionRedisRepository.deleteInactivePrescribedDrug(patientCode, idDrug);
    }

    public PrescribedDrugDTO modifyInactivePrescribedDrugQuantity(String patientCode, int idDrug, int quantity) {
        if(quantity == 0)
            return prescriptionRedisRepository.deleteInactivePrescribedDrug(patientCode, idDrug);
        else if(quantity < 0)
            throw new BadRequestException("Quantity can not lower that zero.");
        return prescriptionRedisRepository.modifyInactivePrescribedDrugQuantity(patientCode, idDrug, quantity);
    }

    public PrescriptionDTO activatePrescription(String patientCode) {
        return prescriptionRedisRepository.activatePrescription(patientCode);
    }

    public Doctor getDoctorById(String id) {
        return (Doctor) userService.getUserIfExists(id, UserType.DOCTOR);
    }

    public Doctor modifyDoctor(Doctor doctor) {
        if(doctorRepository.existsById(doctor.getId())) {
            Doctor doctor_ = getDoctorById((doctor.getId()));
            doctor_.setCity(doctor.getCity());
            doctor_.setDistrict(doctor.getDistrict());
            doctor_.setRegion(doctor.getRegion());
            doctor_.setPassword(PasswordHasher.hash(doctor.getPassword()));
            doctorRepository.save(doctor_);
            return doctor_;
        } else throw new NotFoundException("Doctor "+doctor.getId()+" does not exists");
    }

    public Doctor deleteDoctor(String id) {
        Doctor doctor = getDoctorById(id);
        doctorRepository.deleteById(id);
        return doctor;
    }

    public List<PrescriptionDTO> getLatestPrescriptions(String id_doc, String id_pat) {
        if(!doctorRepository.existsById(id_doc))
            throw new NotFoundException("Doctor "+id_doc+" does not exists");

        Patient patient = (Patient) userService.getUserIfExists(id_pat, UserType.PATIENT);
        if(!Objects.equals(patient.getDoctorCode(), id_doc))
            throw new UnauthorizedException("You are not authorized to access this patient");

        List<LatestPurchase> latestPurchased = patient.getLatestPurchasedDrugs();
        HashMap<LocalDateTime, PrescriptionDTO> prescriptionsHash = new HashMap<>();
        for(LatestPurchase latestPurchase : latestPurchased) {
            for(LatestDrug latestDrug : latestPurchase.getDrugs()){
                LocalDateTime timestamp = latestDrug.getPrescriptionDate();
                if(timestamp == null) continue;
                PrescribedDrugDTO drug = new PrescribedDrugDTO();
                drug.setId(Integer.parseInt(String.valueOf(latestDrug.getDrugId())));
                drug.setName(latestDrug.getDrugName());
                drug.setPrice(latestDrug.getPrice());
                drug.setPurchased(true);
                drug.setQuantity(latestDrug.getQuantity());
                if(!prescriptionsHash.containsKey(timestamp)){
                    PrescriptionDTO prescriptionDTO = new PrescriptionDTO();
                    prescriptionDTO.setTimestamp(timestamp);
                    List<PrescribedDrugDTO> prescribedDrugs = new ArrayList<>();
                    prescribedDrugs.add(drug);
                    prescriptionDTO.setPrescribedDrugs(prescribedDrugs);
                    prescriptionsHash.put(timestamp, prescriptionDTO);
                } else {
                    prescriptionsHash.get(timestamp).getPrescribedDrugs().add(drug);
                }
            }
        }
        return (List<PrescriptionDTO>) prescriptionsHash.values();
    }

    public List<PrescribedDrugDTO> getNextPrescriptions(String id_doc, String id_pat, int lastViewedId) {
        if(!doctorRepository.existsById(id_doc))
            throw new NotFoundException("Doctor "+id_doc+" does not exist");

        Patient patient = (Patient) userService.getUserIfExists(id_pat, UserType.PATIENT);
        System.out.println(patient.getDoctorCode() + "  " + id_doc);
        if(!Objects.equals(patient.getDoctorCode(), id_doc))
            throw new UnauthorizedException("You are not authorized to access this patient");

        Optional<Patient> optPatient = patientRepository.findById(id_pat);
        List<Integer> prescriptionsId = new ArrayList<>();
        List<Purchase> prescriptions = new ArrayList<>();
        if(optPatient.isPresent())
            prescriptionsId = optPatient.get().getPrescriptions();

        int startIndex = Math.max(0, lastViewedId - 10);
        int endIndex = Math.min(lastViewedId, prescriptionsId.size());
        List<Integer> idToView = prescriptionsId.subList(startIndex, endIndex);

        for(int prescId: idToView){
            Query query = new Query();
            query.addCriteria(
                    Criteria.where("id").is(prescId)
            );

            Purchase purchase = mongoTemplate.findOne(query, Purchase.class);
            Optional<Purchase> optPurch = Optional.ofNullable(purchase);
            if(optPurch.isPresent())
                prescriptions.add(optPurch.get());
        }

        // salvo tutti i farmaci prescritti, in ordine inverso, perché almeno l'utente li vede dal più recente al meno recente
        List<PrescribedDrugDTO> prescribedDrugs = new ArrayList<>();
        for(int i=prescriptions.size()-1; i>=0; i--){
            PrescribedDrugDTO prescribedDrugDTO = new PrescribedDrugDTO();
            prescribedDrugDTO.setId(prescriptions.get(i).getId());
            prescribedDrugDTO.setName(prescriptions.get(i).getName());
            prescribedDrugDTO.setQuantity(prescriptions.get(i).getQuantity());
            prescribedDrugDTO.setPrice(prescriptions.get(i).getPrice());
            prescribedDrugDTO.setPurchased(true);
            prescribedDrugs.add(prescribedDrugDTO);
        }
        return prescribedDrugs;

        /*
        HashMap<LocalDateTime, PrescriptionDTO> hashPurchases = new HashMap<>();
        // Analizzare tutti gli acquisti e ottenere una hashmap con chiave timestamp di acquisto e
        // farmaci acquistati
        for(Purchase purch : prescriptions) {
            if(purch.getPrescriptionDate() == null) continue;
            PrescribedDrugDTO drug = new PrescribedDrugDTO();
            drug.setId(purch.getDrugId());
            drug.setName(purch.getName());
            drug.setQuantity(purch.getQuantity());
            drug.setPrice(purch.getPrice());
            if(!hashPurchases.containsKey(purch.getPurchaseDate())){
                PrescriptionDTO prescriptionDTO = new PrescriptionDTO();
                List<PrescribedDrugDTO> drugs = new ArrayList<>();
                drugs.add(drug);
                prescriptionDTO.setTimestamp(purch.getPurchaseDate());
                prescriptionDTO.setPrescribedDrugs(drugs);
                hashPurchases.put(purch.getPurchaseDate(), prescriptionDTO);
            } else {
                hashPurchases.get(purch.getPurchaseDate()).getPrescribedDrugs().add(drug);
            }
        }
        return (List<PrescriptionDTO>) hashPurchases.values();

         */
    }

    public List<SimplePatientDTO> getOwnPatients(String id, String patSurname) {
        if(!doctorRepository.existsById(id))
            throw new NotFoundException("Doctor "+id+" does not exists");

        List<Patient> patients = patientRepository.findBySurnameContainingIgnoreCaseAndDoctorCode(id, patSurname);
        List<SimplePatientDTO> patientDTOs = new ArrayList<>();
        for(Patient patient : patients){
            SimplePatientDTO patientDTO = new SimplePatientDTO();
            patientDTO.setId(patient.getId());
            patientDTO.setName(patient.getName());
            patientDTO.setSurname(patient.getSurname());
            patientDTOs.add(patientDTO);
        }
        return patientDTOs;
    }
}
