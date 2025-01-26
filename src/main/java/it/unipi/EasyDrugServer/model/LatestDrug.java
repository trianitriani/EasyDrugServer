package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class LatestDrug {
    private String id;
    private String name;
    private int quantity;
    private double price;
    private LocalDateTime prescribedTimestamp;
}
