package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final PatientRepository patientRepository;

    public List<PrescriptionDTO> getAllActivePrescriptions(String patientCode){
        return prescriptionRedisRepository.getAllActivePrescriptions(patientCode);
    }

    public Patient getPatientById(String id) {
        Optional<Patient> optPatient = patientRepository.findById(id);
        if(optPatient.isPresent()) return optPatient.get();
        throw new NotFoundException("Patient "+id+" does not exists");
    }

    public void modifyPatient(Patient patient) {
        if(patientRepository.existsById(patient.getIdentifyCode())) {
            patientRepository.save(patient);
        } else throw new NotFoundException("Researcher "+patient.getIdentifyCode()+" does not exists");
    }

    public void deletePatient(Patient patient) {
        if(patientRepository.existsById(patient.getIdentifyCode())) {
            patientRepository.delete(patient);
        } else throw new NotFoundException("Researcher "+patient.getIdentifyCode()+" does not exists");
    }
}
