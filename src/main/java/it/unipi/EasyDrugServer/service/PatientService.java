package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.repository.redis.PatientRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRedisRepository patientRedisRepository;

    public Patient getPatient(String codePatient){
        return patientRedisRepository.findByCode(codePatient);
    }
}
