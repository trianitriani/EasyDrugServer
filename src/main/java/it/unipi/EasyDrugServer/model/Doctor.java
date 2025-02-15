package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Data
@Document(collection = "doctors")
public class Doctor {

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
    private String doctorRegisterCode;
}

