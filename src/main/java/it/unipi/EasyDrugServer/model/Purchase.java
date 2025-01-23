package it.unipi.EasyDrugServer.model;

import org.springframework.data.annotation.Id;

import java.time.LocalDate;

public class Purchase {

    @Id
    private int id;
    private int drugId;
    private String drugName;
    private int quantity;
    private LocalDate purchaseDate;
    private String region;
    private String patientCode;
    private LocalDate prescriptionDate;
    private double price;
}
