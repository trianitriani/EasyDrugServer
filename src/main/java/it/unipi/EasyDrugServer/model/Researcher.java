package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Date;


@Getter
@Setter
@Data
public class Researcher {

    @Id
    private String identifyCode;
    private String password;
    private String municipality;
    private String province;
    private String region;
    private String name;
    private String surname;
    private Date dateOfBirth;
    private String taxCode;
    private String doctoralCertificate ;
}
