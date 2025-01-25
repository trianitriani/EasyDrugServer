package it.unipi.EasyDrugServer.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;

@Getter
@Setter
public class Purchase {

    @Id
    private Integer id;
    private String drugName;
    private int quantity;
    private LocalDate purchaseDate;
    private String region;
    private String patientCode;
    private LocalDate prescriptionDate;
    private double price;
}
