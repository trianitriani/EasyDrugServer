package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Data
public class PurchaseCart {
    private List<Drug> drugs;

    public PurchaseCart(){

    }
}
