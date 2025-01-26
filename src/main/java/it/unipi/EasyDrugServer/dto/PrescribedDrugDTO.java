package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PrescribedDrugDTO {
    private String id;
    private String name;
    private double price;
    private int quantity;
    private boolean purchased;

    public PrescribedDrugDTO() {

    }

}
