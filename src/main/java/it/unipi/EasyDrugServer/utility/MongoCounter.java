package it.unipi.EasyDrugServer.utility;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class MongoCounter {
    private String id;
    private long sequence;

}
