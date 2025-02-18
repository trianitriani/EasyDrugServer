package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class SideEffect {
    @Schema(name = "sideEffectName", description = "Name of the side effect.", type = "String", example = "abdominal pain")
    private String sideEffectName;

    @Schema(name = "frequency", description = "Percentage of occurrence.", type = "double", example = "1.48")
    private double frequency;
}


