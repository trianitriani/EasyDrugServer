package it.unipi.EasyDrugServer.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class PrescriptedDrugDTO {
    private int id;
    private String name;
    private double price;
    private int quantity;
    private boolean purchased;

    public PrescriptedDrugDTO() {

    }
}
