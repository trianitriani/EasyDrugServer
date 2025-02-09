package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class LatestDrug {
    private String drugId;
    private String drugName;
    private int quantity;
    private double price;
    private LocalDateTime prescriptionDate;
}

