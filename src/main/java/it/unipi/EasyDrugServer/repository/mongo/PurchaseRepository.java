package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.dto.DrugDistributionDTO;
import it.unipi.EasyDrugServer.model.Purchase;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
@Repository
public interface PurchaseRepository extends MongoRepository<Purchase, ObjectId> {

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
    List<DrugDistributionDTO> getDistributionByDrug(@Param("drugId") ObjectId drugId, @Param("order") int order);

    String id(String id);
}
