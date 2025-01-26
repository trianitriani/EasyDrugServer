package it.unipi.EasyDrugServer.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Drug;

import java.util.List;


public interface DrugRepository extends MongoRepository<Drug, Integer> {

    List<Drug> findByNameContainingIgnoreCase(String name);

    List<Drug> findByIndicationsName(String name);
}
