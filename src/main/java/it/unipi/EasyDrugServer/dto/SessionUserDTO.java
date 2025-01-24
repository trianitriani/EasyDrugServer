package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionUserDTO {
    private UserType type;
    private String identifyCode;
    private String name;
}
