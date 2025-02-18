package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class LatestDrug {
    @Schema(name = "drugId", description = "Unique identifier for the drug.", type = "ObjectId",example = "67aba9215da6705a000d4050")
    private ObjectId drugId;

    @Schema(name = "drugName", description = "Commercial name of the drug.", type = "String", example = "froben 100 mg compressa rivestita")
    private String drugName;

    @Schema(name = "quantity", description = "Quantity of the drug purchased.", type = "int", example = "1")
    private int quantity;

    @Schema(name = "price", description = "Price of the purchased drug.", type = "double", example = "22.15")
    private double price;

    @Schema(name = "prescriptionDate", description = "Date when the drug was prescribed (if not OTP drug).", type = "LocalDateTime", example = "2024-08-03T13:35:00")
    private LocalDateTime prescriptionDate;
}

