package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Data
public class Indication {
    private String indicationId;
    private String indicationName;
    private String alternativeName;
    private String emaDocument;
}
