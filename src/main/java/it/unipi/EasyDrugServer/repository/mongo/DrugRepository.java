package it.unipi.EasyDrugServer.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Drug;

import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public interface DrugRepository extends MongoRepository<Drug, Integer> {

    List<Drug> findByDrugNameContainingIgnoreCase(String name);

    List<Drug> findByIndicationsContaining(String name);

    List<Drug> findByDrugNameContainingIgnoreCaseAndOnPrescriptionFalse(String name);

    List<Drug> findByDrugNameContainingIgnoreCaseAndOnPrescriptionTrue(String name);
}
