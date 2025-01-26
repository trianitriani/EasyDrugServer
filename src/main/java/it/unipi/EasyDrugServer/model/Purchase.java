package it.unipi.EasyDrugServer.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class Purchase {

    @Id
    private String id;
    private String drugId;
    private String drugName;
    private int quantity;
    private LocalDateTime purchaseTimestamp;
    private String region;
    private String patientCode;
    private LocalDateTime prescriptionTimestamp;
    private double price;
}
