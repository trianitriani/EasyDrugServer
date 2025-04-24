package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;

import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Setter
public class ConfirmPurchaseCartDTO {
    List<PurchaseCartDrugDTO> purchaseDrugs;
    LinkedHashMap<Integer, List<Integer>> presToDelete;
    LinkedHashMap<Integer, List<Integer>> presToModify;
    LinkedHashMap<Integer, Integer> newToPurchase;
    Jedis jedis;

}
