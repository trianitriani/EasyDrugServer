package it.unipi.EasyDrugServer.dto;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class TopDrugDTO {
    private int drugId;
    private String name;
    private int totalQuantity;
}
