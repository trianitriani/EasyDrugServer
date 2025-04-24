package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class PurchaseDrugDTO {
    @Schema(name = "id", description = "Unique identifier for the drug purchase.", type = "String", example = "")
    private String id;
    @Schema(name = "name", description = "Commercial name of the drug.", type = "String", example = "acido acetilsalicilico l.f.m. 500 mg compressa")
    private String name;
    @Schema(name = "price", description = "Price of the drug (euros).", type = "double", example = "6.56")
    private double price;
    @Schema(name = "quantity", description = "Amount of unit purchased.", type = "int", example = "2")
    private int quantity;
    @Schema(name = "prescriptionTimestamp", description = "If present, it's the timestamp when the drug was prescribed.", type = "LocalDateTime", example = "")
    private LocalDateTime prescriptionTimestamp;
    
    public PurchaseDrugDTO() {

    }
}
