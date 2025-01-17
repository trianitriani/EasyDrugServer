package it.unipi.EasyDrugServer.dto;

import it.unipi.EasyDrugServer.model.Drug;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.security.Timestamp;
import java.util.List;

@Setter
@Getter
@Data
public class PrescriptionDTO {
    private Timestamp timestamp;
    private boolean active;
    private List<PrescriptedDrugsDTO> prescriptedDrugs;

    public PrescriptionDTO(){

    }
}
