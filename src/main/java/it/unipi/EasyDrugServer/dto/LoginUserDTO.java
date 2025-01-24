package it.unipi.EasyDrugServer.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserDTO {
    private String identifyCode;
    private String password;
}
