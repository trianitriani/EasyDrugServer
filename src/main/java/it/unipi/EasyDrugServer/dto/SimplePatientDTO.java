package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimplePatientDTO {
    @Schema(name = "id", description = "Patient's identify code, composed of 'P' followed by the Tax Code.", type = "String", example = "PDRSNGL17C43E239B")
    private String id;
    @Schema(name = "name", description = "Patient's first name.", type = "String", example = "angela")
    private String name;
    @Schema(name = "surname", description = "Patient's last name.", type = "String", example = "de rossi")
    private String surname;
}
