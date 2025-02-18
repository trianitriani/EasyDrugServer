package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(name = "id", description = "Unique identifier for the drug.", type = "String", example = "67aba9215da6705a000d3f45")
    private String id;

    @Schema(name = "drugName", description = "Commercial name of the drug.", type = "String", example = "acido acetilsalicilico l.f.m. 500 mg compressa")
    private String drugName;

    @Schema(name = "price", description = "Price of the drug (euros).", type = "double", example = "6.56")
    private double price;

    @Schema(name = "company", description = "Pharmaceutical company producing the drug.", type = "String", example = "laboratorio farmacologico milanese s.r.l.")
    private String company;

    @Schema(name = "activeIngredient", description = "Active ingredient of the drug.", type = "String", example = "acido acetilsalicilico")
    private String activeIngredient;

    @Schema(name = "IUPAC", description = "IUPAC (International Union of Pure and Applied Chemistry) name of the active ingredient.", type = "String", example = "2-acetyloxybenzoic acid")
    private String IUPAC;

    @Schema(name = "SMILES", description = "SMILES (Simplified Molecular Input Line Entry System) representation of the chemical structure.", type = "String", example = "CC(=O)OC1=CC=CC=C1C(=O)O")
    private String SMILES;

    @Schema(name = "onPrescription", description = "Indicates if the drug requires a medical prescription (true) or it's an OTP drug (false).", type = "boolean", example = "false")
    private boolean onPrescription;

    @Field("indications")
    @Schema(name = "indications", description = "List of medical indications for the drug.", type = "List<String>", example = "[\"angina unstable\", \"arthritis\",\"rheumatoid arthritis\",\"bursitis\",\"musculoskeletal disorder\",\"myocardial infarction\",\"osteoarthritis\",\"rheumatism\",\"rheumatic fever\",\"spondylitis\"]")
    private List<String> indications;

    @Field("sideEffects")
    @Schema(name = "sideEffects", description = "List of possible side effects.", type = "List<SideEffect>",example="")
    private List<SideEffect> sideEffects;
}

