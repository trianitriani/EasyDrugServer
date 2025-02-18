package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Document(collection = "purchases")
public class Purchase {

    @Id
    @Schema(name = "id", description = "Unique identifier for the purchase transaction.", type = "String", example = "67aba9305da6705a000d6576")
    private String id;

    @Schema(name = "drugId", description = "Identifier of the purchased drug.", type = "ObjectId",example = "67aba9215da6705a000d4184")
    private ObjectId drugId;

    @Schema(name = "name", description = "Name of the purchased drug.", type = "String", example = "maalox nausea 5 mg granulato effervescente")
    private String name;

    @Schema(name = "quantity", description = "Quantity of the drug purchased.", type = "int", example = "1")
    private int quantity;

    @Indexed
    @Schema(name = "purchaseDate", description = "Date and time of the purchase.", type = "LocalDateTime", example = "2024-01-01T10:08:00")
    private LocalDateTime purchaseDate;

    @Schema(name = "region", description = "Region where the purchase was made.", type = "String", example = "friuli venezia giulia")
    private String region;

    @Schema(name = "prescriptionDate", description = "Date when the prescription was issued.", type = "LocalDateTime", example = "2023-12-27T13:34:00")
    private LocalDateTime prescriptionDate;

    @Schema(name = "price", description = "Cost of the purchase.", type = "double", example = "4.2")
    private double price;
}


