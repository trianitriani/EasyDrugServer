package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.model.Purchase;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PurchaseRepository extends MongoRepository<Purchase, Integer> {

}
