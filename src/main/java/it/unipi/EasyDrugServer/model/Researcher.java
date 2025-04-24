package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@Setter
@Data
@Document(collection = "researchers")
public class Researcher {

    @Id
    @Schema(name = "id", description = "Researcher's identify code, composed of 'R' followed by the Tax Code.", type = "String", example = "RBRTLRT80D03I460W")
    private String id;

    @Schema(name = "password", description = "Researcher's hashed password using bcrypt.", type = "String",example = "$2a$12$D9tNxlLTFEQcu27pucpo3u8OxVhgUjI83e8Fz0nddQQZksmF3S9eK")
    private String password;

    @Schema(name = "city", description = "City of residence.", type = "String", example = "sassofeltrio")
    private String city;

    @Schema(name = "district", description = "District within the city.", type = "String", example = "rimini")
    private String district;

    @Schema(name = "region", description = "Region of residence.", type = "String", example = "emilia romagna")
    private String region;

    @Schema(name = "name", description = "Researcher's first name.", type = "String", example = "alberto")
    private String name;

    @Schema(name = "surname", description = "Researcher's last name.", type = "String", example = "bertellotti")
    private String surname;

    @Schema(name = "dateOfBirth", description = "Researcher's date of birth (YYYY-MM-DD).", type = "String", example = "1980-04-21")
    private String dateOfBirth;

    @Schema(name = "gender", description = "Researcher's gender (e.g., 'm' for male, 'f' for female).", type = "String", example = "m")
    private String gender;

    @Schema(name = "taxCode", description = "Researcher's tax identification code.", type = "String", example = "BRTLRT80D03I460W")
    private String taxCode;

    @Schema(name = "researcherRegisterCode", description = "Researcher's registration number in the research professionals' register.", type = "String", example = "16825")
    private String researcherRegisterCode;
}

