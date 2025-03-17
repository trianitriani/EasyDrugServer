package it.unipi.EasyDrugServer.utility;

import com.mongodb.MongoException;
import it.unipi.EasyDrugServer.model.CommitLog;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Purchase;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import it.unipi.EasyDrugServer.repository.mongo.CommitLogRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RollbackProcessor {
    private final CommitLogRepository commitLogRepository;
    private final JedisSentinelPool jedisSentinelPool;
    private final int time = 60 * 60 * 6;

    @Autowired
    private MongoTemplate mongoTemplate;
    
    public RollbackProcessor(CommitLogRepository commitLogRepository, JedisSentinelPool jedisSentinelPool) {
        this.commitLogRepository = commitLogRepository;
        this.jedisSentinelPool = jedisSentinelPool;
    }

    @Scheduled(fixedRate = time)
    public void processRollback() {
        System.out.println("Searching for rollbacks into commit-log...");
        List<CommitLog> pendingRollbacks = commitLogRepository.findByProcessedFalse();

        try (Jedis jedis = jedisSentinelPool.getResource()) {
            for (CommitLog log : pendingRollbacks) {
                try {
                    if ("DELETE".equals(log.getOperationType())) {
                        // controllo se su Redis ho la conferma che il metodo è andato a buon fine
                        if(jedis.exists("log:"+log.getId()))
                            continue;
                        rollbackPurchases(log.getPurchaseIds(), log);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error during Mongo rollback phase: " + log.getId(), e);
                }
            }
        }
        System.out.println("MongoDB and Redis are now consistent.");
    }

    @Transactional
    public void rollbackPurchases(List<String> purchaseIds, CommitLog log) {
        if(purchaseIds.isEmpty()) return;

        // eliminazione degli acquisti dalla purchases
        for (String purchaseId : purchaseIds) {
            Query deleteQuery = new Query(Criteria.where("_id").is(new ObjectId(purchaseId)));
            mongoTemplate.remove(deleteQuery, Purchase.class);
        }

        // eliminazione dell'ultimo acquisto
        Query patientQuery = new Query(Criteria.where("_id").is(log.getPatientId()));
        Update removePurchaseUpdate = new Update().pop("latestPurchasedDrugs", Update.Position.FIRST);
        mongoTemplate.updateFirst(patientQuery, removePurchaseUpdate, Patient.class);

        // Converte la lista di String in una lista di ObjectId
        List<ObjectId> purchaseObjectIds = purchaseIds.stream().map(ObjectId::new).toList();

        throw new MongoException("Errore di prova su Mongo");
        /*
        // eliminazione degli acquisti dalla lista degli acquisti e dei farmaci prescritti
        Update removePurchasesUpdate = new Update().pullAll("purchases", purchaseObjectIds.toArray());
        mongoTemplate.updateFirst(patientQuery, removePurchasesUpdate, Patient.class);

        Update removePrescriptionsUpdate = new Update().pullAll("prescriptions", purchaseObjectIds.toArray());
        mongoTemplate.updateFirst(patientQuery, removePrescriptionsUpdate, Patient.class);

        // consideriamo gestito il rollback
        System.out.println("Rollback di: " + purchaseIds);
        log.setProcessed(true);
        commitLogRepository.save(log);

         */
    }
}
