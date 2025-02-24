package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Data
public class PrescriptionDTO {
    @Schema(name = "", description = ".", type = "int", example = "")
    private int idPres;
    @Schema(name = "", description = ".", type = "String", example = "")
    private LocalDateTime timestamp;
    @Schema(name = "", description = ".", type = "String", example = "")
    private List<PrescribedDrugDTO> prescribedDrugs;

    public PrescriptionDTO(){
        prescribedDrugs = new ArrayList<>();
    }

    public void addPrescribedDrug(PrescribedDrugDTO drug){
        prescribedDrugs.add(drug);
    }
}
