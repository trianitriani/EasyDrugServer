package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.dto.DrugDistributionDTO;
import it.unipi.EasyDrugServer.model.Purchase;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseRepository extends MongoRepository<Purchase, String> {
    // Query per trovare i Purchase di un paziente tra due date
    List<Purchase> findByPatientCodeAndPurchaseTimestampBetween(String patientCode, LocalDateTime startDate, LocalDateTime endDate);

    @Aggregation(pipeline = {
            "{ $match: { drugId: ?0 } }",
            "{ $group: { _id: '$region', numberOfSaledDrugs: { $sum: '$quantity' } } }",
            "{ $group: { " +
                    "_id: null, " +
                    "regionalSales: { $push: { region: '$_id', numberOfSaledDrugs: '$numberOfSaledDrugs' } }, " +
                    "numberOfSaledDrugsInItaly: { $sum: '$numberOfSaledDrugs' } } }",
            "{ $unwind: '$regionalSales' }",
            "{ $project: { " +
                    "_id: 0, " +
                    "region: '$regionalSales.region', " +
                    "totalQuantity: '$regionalSales.numberOfSaledDrugs', " +
                    "percentage: { $multiply: [ { $divide: ['$regionalSales.numberOfSaledDrugs', '$numberOfSaledDrugsInItaly'] }, 100 ] } } }",
            "{ $sort: { percentage: ?1 } }"
    })
    List<DrugDistributionDTO> getDistributionByDrug(@Param("drugId") String drugId, @Param("order") int order);

    String id(String id);
}
