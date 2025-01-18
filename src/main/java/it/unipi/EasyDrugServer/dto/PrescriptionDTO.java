package it.unipi.EasyDrugServer.dto;

import it.unipi.EasyDrugServer.model.Drug;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Data
public class PrescriptionDTO {
    private LocalDateTime timestamp;
    private List<PrescriptedDrugDTO> prescriptedDrugs;

    public PrescriptionDTO(){

    }
}
