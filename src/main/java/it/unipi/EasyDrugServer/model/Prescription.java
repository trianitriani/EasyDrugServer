package it.unipi.EasyDrugServer.model;

import lombok.Data;

import java.security.Timestamp;
import java.util.List;

@Data
public class Prescription {
    private Timestamp timestamp;
    private List<PrescriptedDrug> prescriptedDrugs;

    public Prescription(Timestamp timestamp, List<PrescriptedDrug> prescriptedDrugs){
        this.timestamp = timestamp;
        this.prescriptedDrugs = prescriptedDrugs;
    }
}
