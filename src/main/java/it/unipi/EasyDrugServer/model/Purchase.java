package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Document(collection = "purchases")
public class Purchase {

    @Id
    private String id;      // campo _id di MongoDB
    private String drugId;
    private String name;
    private int quantity;
    private LocalDateTime purchaseDate;
    private String region;
    private String patientCode;
    private LocalDateTime prescriptionDate;
    private double price;
}

