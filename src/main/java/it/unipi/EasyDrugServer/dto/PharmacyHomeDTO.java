package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PharmacyHomeDTO {
    @Schema(name = "", description = ".", type = "String", example = "")
    private List<PurchaseCartDrugDTO> purchaseCart;
    @Schema(name = "", description = ".", type = "String", example = "")
    private List<PrescriptionDTO> prescriptions;

    public PharmacyHomeDTO() {

    }
}
