package it.unipi.EasyDrugServer.dto;

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
    private int idPres;
    private LocalDateTime timestamp;
    private List<PrescribedDrugDTO> prescribedDrugs;

    public PrescriptionDTO(){
        prescribedDrugs = new ArrayList<>();
    }

    public void addPrescribedDrug(PrescribedDrugDTO drug){
        prescribedDrugs.add(drug);
    }
}
