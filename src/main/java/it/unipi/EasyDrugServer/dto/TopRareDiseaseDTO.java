package it.unipi.EasyDrugServer.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Data
public class TopRareIndicationDTO {
    private String indicationName;
    private int drugCount;
    private List<String> drugNames;
}
