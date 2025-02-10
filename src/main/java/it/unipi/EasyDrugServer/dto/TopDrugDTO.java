package it.unipi.EasyDrugServer.dto;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Data
public class TopDrugDTO {
    @Field("_id.drugId")
    private String drugId;

    @Field("_id.name")
    private String name;

    private int totalQuantity;
}
