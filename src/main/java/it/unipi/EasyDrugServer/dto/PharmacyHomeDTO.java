package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PharmacyHomeDTO {
    private List<PurchaseCartDrugDTO> purchaseCart;
    private List<PrescriptionDTO> prescriptions;

    public PharmacyHomeDTO() {

    }
}
