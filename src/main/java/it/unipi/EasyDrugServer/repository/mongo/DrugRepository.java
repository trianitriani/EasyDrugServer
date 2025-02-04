package it.unipi.EasyDrugServer.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Drug;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface DrugRepository extends MongoRepository<Drug, Integer> {

    List<Drug> findByDrugNameContainingIgnoreCase(String name);

    List<Drug> findByIndicationsIndicationName(String name);

    // boolean existsByDrugId(int drugId);

    // Optional<Drug> findByDrugId(Integer id);

    List<Drug> findByDrugNameContainingIgnoreCaseAndOnPrescriptionFalse(String name);

    List<Drug> findByDrugNameContainingIgnoreCaseAndOnPrescriptionTrue(String name);
}
