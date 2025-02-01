package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Data
@Document(collection = "drugs")
public class Drug {

    @Id
    private int drugId;
    private String drugName;
    private double price;
    private String company;
    private String activeIngredient;
    private String IUPAC;
    private String SMILES;
    private boolean onPrescription;

    @Field("indications")
    private List<Indication> indications;

    @Field("sideEffects")
    private List<SideEffect> sideEffect;
    private String family;
}

