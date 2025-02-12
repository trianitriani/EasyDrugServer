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
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Data
@Document(collection = "drugs")
@CompoundIndex(def = "{'drugName': 1, 'onPrescription': 1}")
public class Drug {

    @Id
    private String id;
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

