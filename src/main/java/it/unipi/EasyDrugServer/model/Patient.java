package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.annotation.Collation;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Data
@Document(collection = "patients")
public class Patient {

    @Id
    private String id;
    private String password;
    private String city;
    private String district;
    private String region;
    private String name;
    private String surname;
    private String dateOfBirth;
    private String gender;
    private String taxCode;
    private String doctorCode;

    @Field("latestPurchasedDrugs")
    private List<LatestPurchase> latestPurchasedDrugs;

    private List<Integer> purchases;
    private List<Integer> prescriptions;
}
