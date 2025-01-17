package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class PurchaseCart {
    private List<Drug> drugs;

    public PurchaseCart(){

    }

    public PurchaseCart(List<Drug> drugs){
        this.drugs = drugs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PurchaseCart:\n");

        if (drugs == null || drugs.isEmpty()) {
            sb.append("No drugs in the cart.");
        } else {
            for (Drug drug : drugs) {
                sb.append("Drug ID: ").append(drug.getId()).append(", ")
                        .append("Name: ").append(drug.getName()).append(", ")
                        .append("Price: ").append(drug.getPrice()).append(", ")
                        .append("Prescription Timestamp: ").append(drug.getPrescriptionTimestamp()).append(", ")
                        .append("Quantity: ").append(drug.getQuantity()).append("\n");
            }
        }

        return sb.toString();
    }
}
