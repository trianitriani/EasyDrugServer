package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Data
@Document(collection = "drugs")
@TypeAlias("Drug")
public class Drug {

    @Id
    private ObjectId id;

    @Indexed
    private String drugName;
    private double price;
    private String company;
    private String activeIngredient;
    private String IUPAC;
    private String SMILES;
    private boolean onPrescription;

    @Field("indications")
    private List<String> indications;

    @Field("sideEffects")
    private List<SideEffect> sideEffects;
}

