package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Setter
@Getter
public class PrescribedDrugDTO {
    private int idPresDrug;
    private String idDrug;
    private String name;
    private double price;
    private int quantity;
    private boolean purchased;

    public PrescribedDrugDTO() {

    }

}
