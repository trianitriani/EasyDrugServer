package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Data
public class Drug {

    @Id
    private int id;
    private String name;
    private double price;
    private String company;
    private String activeIngredient;
    private String iupac;
    private String smiles;
    private boolean onPrescription;
    private List<Indication> indications;
    private List<SideEffect> sideEffect;
}
