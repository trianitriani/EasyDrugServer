package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimplePatientDTO {
    private String id;
    private String name;
    private String surname;
}
