package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Data
@Document(collection = "commit_log")
public class CommitLog {
    @Id
    private String id;
    private String patientId;
    private String operationType;
    private List<String> purchaseIds;
    private LocalDateTime timestamp;

    @Indexed
    private boolean processed;  // Indica se il rollback è stato eseguito

    public CommitLog(){
        this.purchaseIds = new ArrayList<>();
    }
}
