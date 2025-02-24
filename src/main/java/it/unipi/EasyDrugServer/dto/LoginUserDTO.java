package it.unipi.EasyDrugServer.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserDTO {
    @Schema(name ="id", description="Identify code, composed of 'D','P','R' or 'Ph' followed by the Tax Code (or the VAT number for pharmacy).", type="String",example = "DRSSMTN75E43F205M")
    private String identifyCode;
    @Schema(name = "password", description="Hashed password using bcrypt.",type="String",example = "$2a$12$s0FkuQwL2awh/FU7HEsudOGfW.pVzrvwZ97lVDEWJLt1f4up/cBIq")
    private String password;
}
