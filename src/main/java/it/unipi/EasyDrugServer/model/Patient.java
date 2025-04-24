package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Getter
@Setter
@Data
@Document(collection = "patients")
@CompoundIndex(def = "{'doctorCode': 1, 'surname': 1}")
public class Patient {

    @Id
    @Schema(name = "id", description = "Patient's identify code, composed of 'P' followed by the Tax Code.", type = "String", example = "PDRSNGL17C43E239B")
    private String id;

    @Schema(name = "password", description = "Hashed password of the patient using bcrypt.", type = "String",example = "$2a$12$UWKqzP5bz/TZgVz6OKNhHOChozE3tUGWhlG51fA.AOG910GHq.l.O")
    private String password;

    @Schema(name = "city", description = "City of residence.", type = "String", example = "guardavalle")
    private String city;

    @Schema(name = "district", description = "District within the city.", type = "String", example = "catanzaro")
    private String district;

    @Schema(name = "region", description = "Region of residence.", type = "String", example = "calabria")
    private String region;

    @Schema(name = "name", description = "Patient's first name.", type = "String", example = "angela")
    private String name;

    @Schema(name = "surname", description = "Patient's last name.", type = "String", example = "de rossi")
    private String surname;

    @Schema(name = "dateOfBirth", description = "Patient's date of birth (YYYY-MM-DD).", type = "String", example = "2017-03-12")
    private String dateOfBirth;

    @Schema(name = "gender", description = "Patient's gender (e.g., 'm' for male, 'f' for female).", type = "String", example = "f")
    private String gender;

    @Schema(name = "taxCode", description = "Patient's tax identification code.", type = "String", example = "DRSNGL17C43E239B")
    private String taxCode;

    @Schema(name = "doctorCode", description = "Identify code of the patient's assigned doctor.", type = "String", example = "DCRSGDI99C43E239E")
    private String doctorCode;

    @Field("latestPurchasedDrugs")
    @Schema(name = "latestPurchasedDrugs", description = "List of the patient's most recent drug purchased.", type = "List<LatestPurchase>")
    private List<LatestPurchase> latestPurchasedDrugs;

    @Schema(name = "purchases", description = "List of purchase transaction IDs.", type = "List<String>")
    private List<String> purchases;

    @Schema(name = "prescriptions", description = "List of IDs of prescribed drugs purchased.", type = "List<String>")
    private List<String> prescriptions;
}
