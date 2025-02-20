package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Data
public class LatestPurchase {
    @Schema(name = "timestamp", description = "Timestamp of the latest purchase.", type = "LocalDateTime", example = "2024-08-10T13:04:00")
    private LocalDateTime timestamp;

    @Schema(name = "drugs", description = "List of drugs included in the latest purchase.", type = "List<LatestDrug>")
    private List<LatestDrug> drugs = new ArrayList<>();
}
