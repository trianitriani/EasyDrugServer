package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.dto.DrugDistributionDTO;
import it.unipi.EasyDrugServer.dto.TopRareIndicationDTO;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Drug;

import java.util.List;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DrugRepository extends MongoRepository<Drug, String> {

    @Query("{ 'drugName' : { $regex: '^?0'} }")
    List<Drug> findByDrugNameStarting(String drugName);

    @Query("{ 'drugName' : { $regex: '^?0'}, 'onPrescription' : ?1 }")
    List<Drug> findByDrugNameStartingAndOnPrescription(String name, boolean onPrescription);

    @Aggregation(pipeline = {
            "{ $unwind: '$indications' }",
            "{ $group: { " +
                    "_id: '$indications', " +
                    "drugNames: { $addToSet: '$drugName' }, " +
                    "drugCount: { $sum: 1 } } }",
            "{ $sort: { drugCount: 1 } }",
            "{ $limit: ?0 }"
    })
    List<TopRareIndicationDTO> getIndicationsWithLessDrugs(@Param("top") int top);

    List<Drug> findByOnPrescriptionTrue();

    List<Drug> findByOnPrescriptionFalse();
}
