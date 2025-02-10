package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Document(collection = "purchases")
public class Purchase {
    @Id
    private String id;
    private String drugId;
    private String name;
    private int quantity;
    private LocalDateTime purchaseDate;
    private String region;
    private LocalDateTime prescriptionDate;
    private double price;
}

