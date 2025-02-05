package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Transaction;

import java.util.List;

@Getter
@Setter
public class ConfirmPurchaseCartDTO {
    List<PurchaseDrugDTO> purchaseDrugs;
    Transaction transaction;
}
