package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class SideEffect {
    private String sideEffectId;
    private String sideEffectName;
    private double frequence;
    private String emaDocument;
}


