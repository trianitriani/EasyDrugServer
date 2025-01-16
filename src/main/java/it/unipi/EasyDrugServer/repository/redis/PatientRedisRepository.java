package it.unipi.EasyDrugServer.repository.redis;

import it.unipi.EasyDrugServer.model.Patient;
import org.springframework.stereotype.Repository;

@Repository
public class PatientRedisRepository {

    public Patient findByCode(String codePatient) {
       return null;
    }

}
