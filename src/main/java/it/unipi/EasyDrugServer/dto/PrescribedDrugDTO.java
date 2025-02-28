package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Setter
@Getter
public class PrescribedDrugDTO {
    @Schema(name = "idPresDrug", description = ".", type = "int", example = "")
    private int idPresDrug;
    @Schema(name = "idDrug", description = ".", type = "String", example = "")
    private String idDrug;
    @Schema(name = "name", description = ".", type = "String", example = "")
    private String name;
    @Schema(name = "price", description = ".", type = "double", example = "")
    private double price;
    @Schema(name = "quantity", description = ".", type = "int", example = "")
    private int quantity;
    @Schema(name = "purchased", description = ".", type = "boolean", example = "")
    private boolean purchased;

    public PrescribedDrugDTO() {

    }

}
