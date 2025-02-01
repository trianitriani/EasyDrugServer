package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.annotation.Collation;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Data
@Document(collection = "patients")
public class Patient {

    @Id
    private String identifyCode;
    private String password;
    private String city;
    private String district;
    private String region;
    private String name;
    private String surname;
    private Date dateOfBirth;
    private String gender;
    private String taxCode;
    private String familyDoctorCode;
    private List<LatestPurchase> latestPurchasedDrugs;
}
