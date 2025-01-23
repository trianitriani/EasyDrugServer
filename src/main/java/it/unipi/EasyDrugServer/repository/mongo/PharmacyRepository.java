package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.model.Pharmacy;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PharmacyRepository extends MongoRepository<Pharmacy, String> {

}
