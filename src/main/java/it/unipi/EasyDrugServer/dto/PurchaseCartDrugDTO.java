package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class PurchaseCartDrugDTO {
    @Schema(name = "idPurchDrug", description = "Unique identifier for the purchase in the cart.", type = "int", example = "")
    private int idPurchDrug;
    @Schema(name = "idDrug", description = "Unique identifier for the drug purchase.", type = "String", example = "")
    private String idDrug;
    @Schema(name = "name", description = "Commercial name of the drug.", type = "String", example = "acido acetilsalicilico l.f.m. 500 mg compressa")
    private String name;
    @Schema(name = "price", description = "Price of the drug (euros).", type = "double", example = "6.56")
    private double price;
    @Schema(name = "quantity", description = "Amount of unit purchased.", type = "int", example = "2")
    private int quantity;
    @Schema(name = "idPres", description = "Unique identifier for the prescription related to the drug.", type = "Integer", example = "")
    private Integer idPres;
    @Schema(name = "", description = ".", type = "String", example = "")
    private Integer idPresDrug;
    @Schema(name = "prescriptionTimestamp", description = "If present, it's the timestamp when the drug was prescribed.", type = "LocalDateTime", example = "")
    private LocalDateTime prescriptionTimestamp;

    public PurchaseCartDrugDTO() {

    }
}
