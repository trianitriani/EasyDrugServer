package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleDrugDTO {
    @Schema(name = "id", description = "Unique identifier for the drug.", type = "String", example = "67aba9215da6705a000d3f45")
    private String id;
    @Schema(name = "drugName", description = "Commercial name of the drug.", type = "String", example = "acido acetilsalicilico l.f.m. 500 mg compressa")
    private String name;
    @Schema(name = "company", description = "Pharmaceutical company producing the drug.", type = "String", example = "laboratorio farmacologico milanese s.r.l.")
    private String company;
}
