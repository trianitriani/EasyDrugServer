package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class LatestDrug {
    private int id;
    private String name;
    private int quantity;
    private double price;
    private boolean prescribed;
}
