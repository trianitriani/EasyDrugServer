package it.unipi.EasyDrugServer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(name = "indication", description = "Medical indication for a drug.", type = "String", example = "arthritis")
    private String indication;
    @Schema(name = "drugCount", description = "Number of drugs which have that indication.", type = "int", example = "555")
    private int drugCount;
    @Schema(name = "drugNames", description = "List of drugs which have that indication", type = "List<String>")
    private List<String> drugNames;
}
