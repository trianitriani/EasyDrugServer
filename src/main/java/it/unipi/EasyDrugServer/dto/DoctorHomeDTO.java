package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DoctorHomeDTO {
    @Schema(name = "prescriptionCart", description = ".", type = "PrescriptionDTO", example = "")
    private PrescriptionDTO prescriptionCart;
    @Schema(name = "activePrescriptions", description = ".", type = "List<PrescriptionDTO>", example = "")
    private List<PrescriptionDTO> activePrescriptions;

    public DoctorHomeDTO() {

    }
}
