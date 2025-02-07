package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SignupUserDTO {
    // for all
    private UserType type;
    private String password;
    private String municipality;
    private String province;
    private String region;
    private String taxCode;
    private String name;

    // for patient, doctor, researcher
    private String surname;
    private String dateOfBirth;

    // for doctor, researcher
    private String certificate;

    // for pharmacy
    private String vatNumber;
    private String address;

}
