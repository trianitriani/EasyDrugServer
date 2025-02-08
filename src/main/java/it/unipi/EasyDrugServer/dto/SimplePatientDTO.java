package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
public class SimplePatientDTO {
    private String id;
    private String name;
    private String surname;
}
