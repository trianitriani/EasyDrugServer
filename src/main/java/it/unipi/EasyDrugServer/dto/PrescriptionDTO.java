package it.unipi.EasyDrugServer.dto;

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
    private List<PrescribedDrugDTO> prescriptedDrugs;

    public PrescriptionDTO(){

    }

    public void addPrescribedDrug(PrescribedDrugDTO drug){
        prescriptedDrugs.add(drug);
    }

    public boolean isEmpty(){
        return prescriptedDrugs.isEmpty();
    }
}
