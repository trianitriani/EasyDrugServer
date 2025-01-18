package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PharmacistHomeDTO {
    private List<PurchaseDrugDTO> purchaseCart;
    private List<PrescriptionDTO> prescriptions;

    public PharmacistHomeDTO() {

    }
}
