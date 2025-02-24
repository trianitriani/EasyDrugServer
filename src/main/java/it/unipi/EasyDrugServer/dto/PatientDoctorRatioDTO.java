package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class PatientDoctorRatioDTO {
    @Schema(name ="city", description="Place where the statistic is calculated.", type="String",example = "pisa")
    private String city;
    @Schema(name ="ratio", description="Number of patient to doctor who live in the city.", type="double",example = "0.21")
    private double ratio;
}
