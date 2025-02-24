package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Transaction;

import java.util.List;

@Getter
@Setter
public class ConfirmPurchaseCartDTO {
    @Schema(name = "purchaseDrugs", description = "List of item purchased.", type = "List<PurchaseCartDrugDTO>", example = "")
    List<PurchaseCartDrugDTO> purchaseDrugs;
    @Schema(name = "transaction", description = ".", type = "Transaction", example = "")
    Transaction transaction;
}
