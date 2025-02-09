package it.unipi.EasyDrugServer.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class PurchaseCartDrugDTO {
    private String id;
    private String name;
    private double price;
    private int quantity;
    private LocalDateTime prescriptionTimestamp;

    public PurchaseCartDrugDTO() {

    }
}
