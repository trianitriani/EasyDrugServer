package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Data
public class LatestPurchase {
    private LocalDateTime timestamp;
    private List<LatestDrug> drugs = new ArrayList<>();
}




