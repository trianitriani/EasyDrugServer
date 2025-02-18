package it.unipi.EasyDrugServer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@Setter
@Data
@Document(collection = "pharmacies")
public class Pharmacy {

    @Id
    @Schema(name = "id", description = "Pharmacy's identify code, composed of 'Ph' followed by the VAT number.", type = "String", example = "Ph50323764625")
    private String id;

    @Schema(name = "name", description = "Pharmacy's name.", type = "String", example = "dr max")
    private String name;

    @Schema(name = "password", description = "Pharmacy's hashed password using bcrypt.", type = "String",example = "$2a$12$sKGbkJ37M3MS.FwE31IKmOMpQHtVJj5I4ozm1Us.zOIz21s9GWZD6")
    private String password;

    @Schema(name = "VATnumber", description = "Pharmacy's VAT (Value Added Tax) number.", type = "String", example = "50323764625")
    private String VATnumber;

    @Schema(name = "address", description = "Pharmacy's physical address.", type = "String", example = "via roma 54")
    private String address;

    @Schema(name = "city", description = "City where the pharmacy is located.", type = "String", example = "reggio di calabria")
    private String city;

    @Schema(name = "district", description = "District within the city.", type = "String", example = "reggio di calabria")
    private String district;

    @Schema(name = "region", description = "Region where the pharmacy is located.", type = "String", example = "calabria")
    private String region;

    @Schema(name = "ownerTaxCode", description = "Tax code of the pharmacy owner.", type = "String", example = "NLSKLN00R03H224F")
    private String ownerTaxCode;
}


