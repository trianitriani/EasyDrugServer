package it.unipi.EasyDrugServer.model;

import java.security.Timestamp;
import java.util.List;

public class Prescription {
    private Timestamp timestamp;
    private List<PrescriptedDrug> prescriptedDrugs;

    public Prescription(){

    }
}
