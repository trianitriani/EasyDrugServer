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
    private String city;
    private String district;
    private String region;
    private String name;

    // for patient, doctor, researcher
    private String surname;
    private String dateOfBirth;
    private String taxCode;
    private String gender;

    // for doctor, researcher
    private String doctorRegisterCode;
    private String researcherRegisterCode;

    // for patient
    private String doctorCode;

    // for pharmacy
    private String vatNumber;
    private String address;
    private String ownerTaxCode;

}
