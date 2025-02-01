package it.unipi.EasyDrugServer.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Doctor;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends MongoRepository<Doctor, String> {

}
