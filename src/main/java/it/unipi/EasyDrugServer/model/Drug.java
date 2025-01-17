package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Data
public class Drug {
    private int id;                             // unique code of the product
    private String name;                        // name of the product
    private double price;                       // price of the product

    // [ only related to prescriptions or purchase ]
    private int quantity;                       // quantity of the product
    private Timestamp prescriptionTimestamp;    // if is not null, indicate the relative prescription

    // [ only related to prescriptions ]
    private boolean purchased;                  // Indicates that a specific prescribed drug is already purchased

    public Drug() {

    }

}
