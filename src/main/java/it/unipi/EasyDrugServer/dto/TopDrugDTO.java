package it.unipi.EasyDrugServer.dto;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
@Data
public class TopDrugDTO {
    private ObjectId drugId;
    private String name;
    private int totalQuantity;
}
