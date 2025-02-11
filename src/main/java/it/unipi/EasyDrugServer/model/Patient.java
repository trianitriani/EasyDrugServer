package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.annotation.Collation;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Data
@Document(collection = "patients")
@CompoundIndex(def = "{'doctorCode': 1, 'surname': 1}")
public class Patient {

    @Id
    private String id;
    private String password;
    private String city;
    private String district;
    private String region;
    private String name;

    @Indexed
    private String surname;

    private String dateOfBirth;
    private String gender;
    private String taxCode;
    private String doctorCode;

    @Field("latestPurchasedDrugs")
    private List<LatestPurchase> latestPurchasedDrugs;

    private List<String> purchases;
    private List<String> prescriptions;
}
