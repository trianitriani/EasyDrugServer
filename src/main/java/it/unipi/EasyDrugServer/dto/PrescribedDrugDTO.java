package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Setter
@Getter
public class PrescribedDrugDTO {
    @Schema(name = "", description = ".", type = "String", example = "")
    private int idPresDrug;
    @Schema(name = "", description = ".", type = "String", example = "")
    private String idDrug;
    @Schema(name = "", description = ".", type = "String", example = "")
    private String name;
    @Schema(name = "", description = ".", type = "String", example = "")
    private double price;
    @Schema(name = "", description = ".", type = "String", example = "")
    private int quantity;
    @Schema(name = "", description = ".", type = "String", example = "")
    private boolean purchased;

    public PrescribedDrugDTO() {

    }

}
