package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(name ="id", description="Doctor's identify code, composed of 'D' followed by the Tax Code.", type="String",example = "DRSSMTN75E43F205M")
    private String id;
    @Schema(name = "password", description="Doctor's hashed password using bcrypt.",type="String",example = "$2a$12$s0FkuQwL2awh/FU7HEsudOGfW.pVzrvwZ97lVDEWJLt1f4up/cBIq")
    private String password;
    @Schema(name = "city", description="city of residence",type="String",example = "milano")
    private String city;
    @Schema(name = "district",description="District within the city.",type="String", example = "milano")
    private String district;
    @Schema(name = "region",description="Region of residence.",type="String", example = "Lombardia")
    private String region;
    @Schema(name = "name", description="Doctor's first name.",type="String",example = "martina")
    private String name;
    @Schema(name = "surname", description="Doctor's last name.",type="String",example = "rossi")
    private String surname;
    @Schema(name = "dateOfBirth",description="Doctor's date of birth (YYYY-MM-DD).",type="String", example = "1975-05-17")
    private String dateOfBirth;
    @Schema(name = "gender", description="Doctor's gender (e.g., 'm' for male, 'f' for female).",type="String",example = "f")
    private String gender;
    @Schema(name = "taxCode", description="Doctor's tax identification code.",type="String",example = "RSSMTN75E43F205M")
    private String taxCode;
    @Schema(name = "doctorRegisterCode", description="Doctor's registration number in the official medical register.",type="String",example = "70598")
    private String doctorRegisterCode;
}
