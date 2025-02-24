package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SignupUserDTO {
    // for all
    @Schema(name ="type", description="A user can register an account of the following type : PATIENT, DOCTOR, RESEARCHER, PHARMACY.", type="UserType",example = "PATIENT")
    private UserType type;
    @Schema(name = "password", description="Hashed password using bcrypt.",type="String",example = "$2a$12$s0FkuQwL2awh/FU7HEsudOGfW.pVzrvwZ97lVDEWJLt1f4up/cBIq")
    private String password;
    @Schema(name = "city", description="city of residence",type="String",example = "milano")
    private String city;
    @Schema(name = "district",description="District within the city.",type="String", example = "milano")
    private String district;
    @Schema(name = "region",description="Region of residence.",type="String", example = "Lombardia")
    private String region;
    @Schema(name = "name", description="First name of the account (or name of the pharmacy).",type="String",example = "martina")
    private String name;

    // for patient, doctor, researcher
    @Schema(name = "surname", description="Doctor,patient or researcher's last name.",type="String",example = "rossi")
    private String surname;
    @Schema(name = "dateOfBirth",description="Doctor,patient or researcher's date of birth (YYYY-MM-DD).",type="String", example = "1975-05-17")
    private String dateOfBirth;
    @Schema(name = "taxCode", description="Doctor,patient or researcher's tax identification code.",type="String",example = "RSSMTN75E43F205M")
    private String taxCode;
    @Schema(name = "gender", description="Doctor,patient or researcher's gender (e.g., 'm' for male, 'f' for female).",type="String",example = "f")
    private String gender;

    // for doctor, researcher
    @Schema(name = "doctorRegisterCode", description="Doctor's registration number in the official medical register.",type="String",example = "70598")
    private String doctorRegisterCode;
    @Schema(name = "researcherRegisterCode", description = "Researcher's registration number in the research professionals' register.", type = "String", example = "16825")
    private String researcherRegisterCode;

    // for patient
    @Schema(name = "doctorCode", description = "Identify code of the patient's assigned doctor.", type = "String", example = "DCRSGDI99C43E239E")
    private String doctorCode;

    // for pharmacy
    @Schema(name = "vatNumber", description = "Pharmacy's VAT (Value Added Tax) number.", type = "String", example = "50323764625")
    private String vatNumber;
    @Schema(name = "address", description = "Pharmacy's physical address.", type = "String", example = "via roma 54")
    private String address;
    @Schema(name = "ownerTaxCode", description = "Tax code of the pharmacy owner.", type = "String", example = "NLSKLN00R03H224F")
    private String ownerTaxCode;

}
