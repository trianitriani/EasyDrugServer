package it.unipi.EasyDrugServer.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Getter
@Setter
@Data
public class TopRareIndicationDTO {
    @Field("_id")
    private String indication;
    private int drugCount;
    private List<String> drugNames;
}
