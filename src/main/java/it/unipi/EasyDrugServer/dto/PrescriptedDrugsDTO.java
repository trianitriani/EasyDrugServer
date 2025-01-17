package it.unipi.EasyDrugServer.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class PrescriptedDrugsDTO {
    private int id;
    private String name;
    private double price;
    private int quantity;

    public PrescriptedDrugsDTO() {

    }
}
