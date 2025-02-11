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
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import it.unipi.EasyDrugServer.exception.BadRequestException;

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
    private final int N_TO_VIEW = 5;

    public PrescriptionDTO getPrescriptionCart(String id_pat) {
        return prescriptionRedisRepository.getPrescriptionCart(id_pat);
    }

    public PrescribedDrugDTO saveDrugIntoPrescriptionCart(String id_pat, PrescribedDrugDTO drug) {
        if(Objects.equals(drug.getName(), ""))
            throw new BadRequestException("Name of the drug can not be null");
        if(drug.getQuantity() < 1)
            throw new BadRequestException("Quantity can not be lower than one");
        return prescriptionRedisRepository.saveDrugIntoPrescriptionCart(id_pat, drug);
    }

    public PrescribedDrugDTO deleteDrugIntoPrescriptionCart(String id_pat, String id_drug) {
        return prescriptionRedisRepository.deleteDrugIntoPrescriptionCart(id_pat, id_drug);
    }

    public PrescribedDrugDTO modifyDrugQuantityIntoPrescriptionCart(String id_pat, String id_drug, int quantity) {
        if(quantity == 0)
            return prescriptionRedisRepository.deleteDrugIntoPrescriptionCart(id_pat, id_drug);
        else if(quantity < 0)
            throw new BadRequestException("Quantity can not be lower that zero.");
        return prescriptionRedisRepository.modifyDrugQuantityIntoPrescriptionCart(id_pat, id_drug, quantity);
    }

    public PrescriptionDTO activatePrescriptionCart(String id_pat) {
        return prescriptionRedisRepository.activatePrescriptionCart(id_pat);
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
            if(doctor.getPassword() != null){
                String hash = PasswordHasher.hash(doctor.getPassword());
                doctor_.setPassword(hash);
            }
            doctorRepository.save(doctor_);
            return doctor_;
        } else throw new NotFoundException("Doctor "+doctor.getId()+" does not exist");
    }

    public Doctor deleteDoctor(String id) {
        Doctor doctor = getDoctorById(id);
        doctorRepository.deleteById(id);
        return doctor;
    }

    // mostra le successive prescrizioni già concluse dopo la mAlreadyViews-esima divise per prescrizione
    public List<LatestPurchase> getNextPrescriptionDrugs(String id_doc, String id_pat, int n_uploaded) {
        if(!doctorRepository.existsById(id_doc))
            throw new NotFoundException("Doctor "+id_doc+" does not exist");

        Patient patient = (Patient) userService.getUserIfExists(id_pat, UserType.PATIENT);
        if(!Objects.equals(patient.getDoctorCode(), id_doc))
            throw new UnauthorizedException("You are not authorized to access this patient");

        Optional<Patient> optPatient = patientRepository.findById(id_pat);
        List<String> prescriptionsId = new ArrayList<>();
        List<Purchase> prescriptions = new ArrayList<>();
        if(optPatient.isPresent())
            prescriptionsId = optPatient.get().getPrescriptions();

        int startIndex = prescriptionsId.size() - n_uploaded;
        if(startIndex <= 0)
            return new ArrayList<>();

        int endIndex = startIndex - N_TO_VIEW;
        if(endIndex < 0) endIndex = 0;
        // id of purchased drugs that interest us
        List<String> idToView = prescriptionsId.subList(endIndex, startIndex);

        // retrieve information of any id
        for(String prescId: idToView){
            Optional<Purchase> optPurch = purchaseRepository.findById(prescId);
            optPurch.ifPresent(prescriptions::add);
        }

        // salvo tutti i farmaci prescritti, in ordine inverso, perché almeno l'utente li vede dal più recente al meno recente
        Map<LocalDateTime, LatestPurchase> hashPurchases = new LinkedHashMap<>();
        for(int i=prescriptions.size()-1; i>=0; i--){
            LatestDrug drug = new LatestDrug();
            Purchase purch = prescriptions.get(i);

            ObjectId objectId = new ObjectId(purch.getId());
            drug.setDrugId(objectId);
            drug.setDrugName(purch.getName());
            drug.setQuantity(purch.getQuantity());
            drug.setPrice(purch.getPrice());
            drug.setPrescriptionDate(purch.getPrescriptionDate());
            if(!hashPurchases.containsKey(purch.getPurchaseDate())){
                LatestPurchase latestPurchase = new LatestPurchase();
                List<LatestDrug> drugs = new ArrayList<>();
                drugs.add(drug);
                latestPurchase.setTimestamp(purch.getPurchaseDate());
                latestPurchase.setDrugs(drugs);
                hashPurchases.put(purch.getPurchaseDate(), latestPurchase);
            } else
                hashPurchases.get(purch.getPurchaseDate()).getDrugs().add(drug);
        }
        return new ArrayList<>(hashPurchases.values());
    }

    public List<SimplePatientDTO> getOwnPatientsBySurname(String id, String pat_surname) {
        if(!doctorRepository.existsById(id))
            throw new NotFoundException("Doctor "+id+" does not exist");

        List<Patient> patients = patientRepository.findByDoctorCodeAndSurnameStarting(id, pat_surname.toLowerCase());
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
