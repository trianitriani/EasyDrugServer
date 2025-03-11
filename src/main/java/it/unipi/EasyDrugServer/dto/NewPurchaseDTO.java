package it.unipi.EasyDrugServer.dto;

import it.unipi.EasyDrugServer.model.LatestPurchase;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class NewPurchaseDTO {
    LatestPurchase latestPurchase;
    List<String> purchaseIds;
}
