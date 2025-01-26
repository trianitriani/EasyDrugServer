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
import lombok.RequiredArgsConstructor;
// import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import it.unipi.EasyDrugServer.exception.BadRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DoctorService extends UserService {
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final DoctorRepository doctorRepository;
    private final PurchaseRepository purchaseRepository;
    private final PatientRepository patientRepository;

    public PrescriptionDTO getInactivePrescription(String doctorCode, String patientCode) {
        return prescriptionRedisRepository.getInactivePrescription(doctorCode, patientCode);
    }

    public PrescribedDrugDTO saveInactivePrescribedDrug(String doctorCode, String patientCode, PrescribedDrugDTO drug) {
        if(Objects.equals(drug.getName(), ""))
            throw new BadRequestException("Name of the drug can not be null");
        if(drug.getQuantity() < 1)
            throw new BadRequestException("Quantity can not be lower than one");
        return prescriptionRedisRepository.saveInactivePrescribedDrug(doctorCode, patientCode, drug);
    }

    public PrescribedDrugDTO deleteInactivePrescribedDrug(String doctorCode, String patientCode, int idDrug) {
        return prescriptionRedisRepository.deleteInactivePrescribedDrug(doctorCode, patientCode, idDrug);
    }

    public PrescribedDrugDTO modifyInactivePrescribedDrugQuantity(String doctorCode, String patientCode, int idDrug, int quantity) {
        if(quantity == 0)
            return prescriptionRedisRepository.deleteInactivePrescribedDrug(doctorCode, patientCode, idDrug);
        else if(quantity < 0)
            throw new BadRequestException("Quantity can not lower that zero.");
        return prescriptionRedisRepository.modifyInactivePrescribedDrugQuantity(doctorCode, patientCode, idDrug, quantity);
    }

    public PrescriptionDTO activatePrescription(String doctorCode, String patientCode) {
        return prescriptionRedisRepository.activatePrescription(doctorCode, patientCode);
    }

    public Doctor getDoctorById(String id) {
        return (Doctor) getUserIfExists(id, UserType.DOCTOR);
    }

    public void modifyDoctor(Doctor doctor) {
        if(doctorRepository.existsById(doctor.getIdentifyCode())) {
            doctorRepository.save(doctor);
        } else throw new NotFoundException("Doctor "+doctor.getIdentifyCode()+" does not exists");
    }

    public Doctor deleteDoctor(String id) {
        Doctor doctor = getDoctorById(id);
        doctorRepository.deleteById(id);
        return doctor;
    }

    public List<PrescriptionDTO> getLatestPrescriptions(String id_doc, String id_pat) {
        if(!doctorRepository.existsById(id_doc))
            throw new NotFoundException("Doctor "+id_doc+" does not exists");

        Patient patient = (Patient) getUserIfExists(id_pat, UserType.PATIENT);
        if(!Objects.equals(patient.getIdentifyCodeDoctor(), id_doc))
            throw new UnauthorizedException("You are not authorized to access this patient");

        List<LatestPurchase> latestPurchased = patient.getLatestPurchased();
        HashMap<LocalDateTime, PrescriptionDTO> prescriptionsHash = new HashMap<>();
        for(LatestPurchase latestPurchase : latestPurchased) {
            for(LatestDrug latestDrug : latestPurchase.getDrugs()){
                LocalDateTime timestamp = latestDrug.getPrescribedTimestamp();
                if(timestamp == null) continue;
                PrescribedDrugDTO drug = new PrescribedDrugDTO();
                drug.setId(latestDrug.getId());
                drug.setName(latestDrug.getName());
                drug.setPrice(latestDrug.getPrice());
                drug.setPurchased(true);
                drug.setQuantity(latestDrug.getQuantity());
                if(!prescriptionsHash.containsKey(timestamp)){
                    PrescriptionDTO prescriptionDTO = new PrescriptionDTO();
                    prescriptionDTO.setTimestamp(timestamp);
                    List<PrescribedDrugDTO> prescribedDrugs = new ArrayList<>();
                    prescribedDrugs.add(drug);
                    prescriptionDTO.setPrescriptedDrugs(prescribedDrugs);
                    prescriptionsHash.put(timestamp, prescriptionDTO);
                } else {
                    prescriptionsHash.get(timestamp).getPrescriptedDrugs().add(drug);
                }
            }
        }
        return (List<PrescriptionDTO>) prescriptionsHash.values();
    }

    public List<PrescriptionDTO> getPrescriptionsFromTo(String id_doc, String id_pat, LocalDate from, LocalDate to) {
        if(!doctorRepository.existsById(id_doc))
            throw new NotFoundException("Doctor "+id_doc+" does not exists");

        Patient patient = (Patient) getUserIfExists(id_pat, UserType.PATIENT);
        if(!Objects.equals(patient.getIdentifyCodeDoctor(), id_doc))
            throw new UnauthorizedException("You are not authorized to access this patient");

        LocalDateTime fromTime = from.atStartOfDay();
        LocalDateTime toTime = to.atTime(23, 59, 59);
        List<Purchase> purchases = purchaseRepository.findByPatientCodeAndPurchaseDateBetween(id_pat, fromTime, toTime);;
        HashMap<LocalDateTime, PrescriptionDTO> hashPurchases = new HashMap<>();
        // Analizzare tutti gli acquisti e ottenere una hashmap con chiave timestamp di acquisto e
        // farmaci acquistati
        for(Purchase purch : purchases) {
            if(purch.getPrescriptionTimestamp() == null) continue;
            PrescribedDrugDTO drug = new PrescribedDrugDTO();
            drug.setId(purch.getDrugId());
            drug.setName(purch.getDrugName());
            drug.setQuantity(purch.getQuantity());
            drug.setPrice(purch.getPrice());
            if(!hashPurchases.containsKey(purch.getPurchaseTimestamp())){
                PrescriptionDTO prescriptionDTO = new PrescriptionDTO();
                List<PrescribedDrugDTO> drugs = new ArrayList<>();
                drugs.add(drug);
                prescriptionDTO.setTimestamp(purch.getPurchaseTimestamp());
                prescriptionDTO.setPrescriptedDrugs(drugs);
                hashPurchases.put(purch.getPurchaseTimestamp(), prescriptionDTO);
            } else {
                hashPurchases.get(purch.getPurchaseTimestamp()).getPrescriptedDrugs().add(drug);
            }
        }
        return (List<PrescriptionDTO>) hashPurchases.values();
    }

    public List<SimplePatientDTO> getOwnPatients(String id) {
        if(!doctorRepository.existsById(id))
            throw new NotFoundException("Doctor "+id+" does not exists");

        List<Patient> patients = patientRepository.findByIdentifyCodeDoctor(id);
        List<SimplePatientDTO> patientDTOs = new ArrayList<>();
        for(Patient patient : patients){
            SimplePatientDTO patientDTO = new SimplePatientDTO();
            patientDTO.setId(patient.getIdentifyCode());
            patientDTO.setName(patient.getName());
            patientDTO.setSurname(patient.getSurname());
            patientDTOs.add(patientDTO);
        }
        return patientDTOs;
    }
}
