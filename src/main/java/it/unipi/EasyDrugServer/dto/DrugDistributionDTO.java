package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class DrugDistributionDTO {
    @Schema(name ="region", description="Place where the statistic is calculated.", type="String",example = "toscana")
    private String region;
    @Schema(name ="numberOfSoldDrugs", description="Number of occurrances of a specified drug in range of period in that region.", type="int",example = "12300")
    private int numberOfSoldDrugs;
    @Schema(name ="percentage", description="Percentage considering occurrances in any region.", type="double",example = "23.3")
    private double percentage;
}
