package it.unipi.EasyDrugServer.repository.mongo;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Drug;

import java.util.List;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DrugRepository extends MongoRepository<Drug, String> {

    @Query("{ 'drugName' : { $regex: '^?0', $options: 'i' } }")
    List<Drug> findByDrugNameStartingWithIgnoreCase(String drugName);

    List<Drug> findByIndicationsContaining(String name);

    List<Drug> findByDrugNameContainingIgnoreCaseAndOnPrescriptionFalse(String name);

    List<Drug> findByDrugNameContainingIgnoreCaseAndOnPrescriptionTrue(String name);
}
