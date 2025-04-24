package it.unipi.EasyDrugServer.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Data
public class TopDrugDTO {
    @Field("_id.drugId")
    @Schema(name = "drugId", description = "Unique identifier for the drug.", type = "String", example = "67aba9215da6705a000d3f45")
    private String drugId;

    @Field("_id.name")
    @Schema(name = "name", description = "Commercial name of the drug.", type = "String", example = "acido acetilsalicilico l.f.m. 500 mg compressa")
    private String name;

    @Schema(name = "totalQuantity", description = "Number of purchase involving that drug.", type = "int", example = "233445")
    private int totalQuantity;
}
