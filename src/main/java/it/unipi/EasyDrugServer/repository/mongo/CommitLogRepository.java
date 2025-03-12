package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.model.CommitLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommitLogRepository extends MongoRepository<CommitLog, String> {
    List<CommitLog> findByProcessedFalse();
}
