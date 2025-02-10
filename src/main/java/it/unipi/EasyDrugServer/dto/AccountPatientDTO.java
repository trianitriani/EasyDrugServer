package it.unipi.EasyDrugServer.dto;

import it.unipi.EasyDrugServer.model.LatestPurchase;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Getter
@Setter
@Data
public class AccountPatientDTO {
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
}
