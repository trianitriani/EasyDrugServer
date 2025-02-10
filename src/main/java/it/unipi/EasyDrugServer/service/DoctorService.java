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
    private final int N_TO_VIEW = 10;

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
            throw new BadRequestException("Quantity can not be lower that zero.");
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
            if(!Objects.equals(doctor.getPassword(), ""))
                doctor_.setPassword(PasswordHasher.hash(doctor.getPassword()));
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
    public List<LatestPurchase> getNextPrescriptionDrugs(String id_doc, String id_pat, int nAlreadyViewed) {
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

        int startIndex = prescriptionsId.size() - nAlreadyViewed;
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
        HashMap<LocalDateTime, LatestPurchase> hashPurchases = new HashMap<>();
        for(int i=prescriptions.size()-1; i>=0; i--){
            LatestDrug drug = new LatestDrug();
            Purchase purch = prescriptions.get(i);
            drug.setDrugId(purch.getId());
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

    public List<SimplePatientDTO> getOwnPatients(String id, String patSurname) {
        if(!doctorRepository.existsById(id))
            throw new NotFoundException("Doctor "+id+" does not exist");

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
