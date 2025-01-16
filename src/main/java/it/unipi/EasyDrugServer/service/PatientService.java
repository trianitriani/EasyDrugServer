package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.repository.redis.PatientRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRedisRepository patientRedisRepository;

    public Patient getPatient(String codePatient){
        return patientRedisRepository.findByCode(codePatient);
    }

    public String saveDrugToPurchase(String codPatient, Drug drug) {
        patientRedisRepository.saveDrugToPurchase(codPatient, drug);
        return null;
    }

    public List<Drug> getCart(String patientCode){
        return patientRedisRepository.getCart(patientCode);
    }

}
