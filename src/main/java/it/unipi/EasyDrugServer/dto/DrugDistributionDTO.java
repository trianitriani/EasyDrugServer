package it.unipi.EasyDrugServer.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class DrugDistributionDTO {
    private String region;
    private int numberOfSoldDrugs;
    private double percentage;
}
