package it.unipi.EasyDrugServer.utility;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;

@Data
@Setter
@Getter
public class MongoHelper {

    @Autowired
    private MongoOperations mongoOperations;
    private String id;
    private long sequence;

    public long generateSequence(String seqName) {
        MongoCounter counter = mongoOperations.findAndModify(
                new Query(Criteria.where("_id").is(seqName)),
                new Update().inc("sequence", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                MongoCounter.class
        );

        return (counter != null) ? getSequence() : 1;
    }

}
