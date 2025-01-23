package it.unipi.EasyDrugServer.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DoctorRepository extends MongoRepository<DoctorRepository, String> {

}
