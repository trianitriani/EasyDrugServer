package it.unipi.EasyDrugServer.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;


@Getter
@Setter
@Data
public class Pharmacy {
    @Id
    private String identifyCode;
    private String name;
    private String password;
    private String address;
    private String municipality;
    private String province;
    private String region;
    private String vatNumber;
    private String ownerTaxCode;
}