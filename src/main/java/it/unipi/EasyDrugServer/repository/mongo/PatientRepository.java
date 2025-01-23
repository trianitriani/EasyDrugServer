package it.unipi.EasyDrugServer.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Patient;

public interface PatientRepository extends MongoRepository<Patient, String> {

}
