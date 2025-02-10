package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.dto.DrugDistributionDTO;
import it.unipi.EasyDrugServer.dto.TopDrugDTO;
import it.unipi.EasyDrugServer.model.Purchase;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
@Repository
public interface PurchaseRepository extends MongoRepository<Purchase, String> {

    @Aggregation(pipeline = {
            "{ $match: { { purchaseDate: { $gte: '?2', $lte: '?3' } }, drugId: ?0 } }",
            "{ $group: { _id: '$region', numberOfSoldDrugs: { $sum: '$quantity' } } }",
            "{ $group: { " +
                    "_id: null, " +
                    "regionalSales: { $push: { region: '$_id', numberOfSoldDrugs: '$numberOfSoldDrugs' } }, " +
                    "numberOfSoldDrugsInItaly: { $sum: '$numberOfSoldDrugs' } } }",
            "{ $unwind: '$regionalSales' }",
            "{ $project: { " +
                    "region: '$regionalSales.region', " +
                    "numberOfSoldDrugs: '$regionalSales.numberOfSoldDrugs', " +
                    "percentage: { $multiply: [ " +
                    "{ $cond: { " +
                    "if: { $eq: ['$numberOfSoldDrugsInItaly', 0] }, " +
                    "then: 0, " +
                    "else: { $divide: [{ $toDouble: '$regionalSales.numberOfSoldDrugs' }, '$numberOfSoldDrugsInItaly'] } } }, " +
                    "100 ] } } }",
            "{ $sort: { percentage: ?1 } }"
    })
    List<DrugDistributionDTO> getDistributionByDrug(@Param("drugId") ObjectId drugId, @Param("order") int order, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Aggregation(pipeline = {
            "{ $match: { purchaseDate: { $gte: '?0', $lte: '?1' } } }",
            "{ $group: { _id: { drugId: '$drugId', name: '$name' }, totalQuantity: { $sum: '$quantity' } } }",
            "{ $sort: { totalQuantity: -1 } }",
            "{ $limit: ?2 }"
    })
    List<TopDrugDTO> getTopDrugs(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("top") int top);

    String id(String id);
}
