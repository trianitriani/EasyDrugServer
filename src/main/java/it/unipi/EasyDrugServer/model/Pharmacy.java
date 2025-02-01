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
    private String VATnumber;
    private String address;
    private String city;
    private String district;
    private String region;
    private String ownerTaxCode;
}


