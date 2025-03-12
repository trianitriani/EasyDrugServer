package it.unipi.EasyDrugServer.utility;


import it.unipi.EasyDrugServer.model.CommitLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import it.unipi.EasyDrugServer.repository.mongo.CommitLogRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;

import java.util.List;

@Component
public class RollbackProcessor {

    private final CommitLogRepository commitLogRepository;
    private final PurchaseRepository purchaseRepository;


    public RollbackProcessor(CommitLogRepository commitLogRepository, PurchaseRepository purchaseRepository) {
        this.commitLogRepository = commitLogRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void processRollbackOnStartup() {
        System.out.println("Searching for rollbacks into commit-log...");
        List<CommitLog> pendingRollbacks = commitLogRepository.findByProcessedFalse();

        for (CommitLog log : pendingRollbacks) {
            try {
                if ("DELETE".equals(log.getOperationType())) {
                    for(String purc:log.getPurchaseIds()){
                        purchaseRepository.deleteById(purc); //elimino tutti gli acquisti nella lista
                    }
                }
                log.setProcessed(true);
                commitLogRepository.save(log);
            } catch (Exception e) {
                throw new RuntimeException("Error during Mongo rollback phase: " + log.getId(), e);
            }
        }
        System.out.println("MongoDb is now consistent.");
    }
}
