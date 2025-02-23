package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.PharmacyHomeDTO;
import it.unipi.EasyDrugServer.dto.PurchaseCartDrugDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.LatestPurchase;
import it.unipi.EasyDrugServer.model.Pharmacy;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pharmacies")
@RequiredArgsConstructor
public class PharmacyController {
    private final PharmacyService pharmacyService;
    private final GlobalExceptionHandler exceptionHandler;

    /**
     * ## PHARMACIST ##
     * View a patient's cart and his active prescriptions
     * @param id_pat code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "View pharmacy home", description = "Retrieve the patient's purchase cart along with a list of its active prescriptions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: home successfully loaded."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/home/patients/{id_pat}")
    public ResponseEntity<ResponseDTO> viewPharmacyHome(@PathVariable("id_pat") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PBRNNCL54B03F205J") String id_pat){
        try {
            PharmacyHomeDTO pharmacyHomeDTO = pharmacyService.viewPharmacyHome(id_pat);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, pharmacyHomeDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## PHARMACIST ##
     * Insert into redis db information related to a specific drug that is insert into a cart
     * of a specific patient by a pharmacist
     * @param id_pat code of patient
     * @param drug drug insert into a cart
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "Insert drug into purchase cart", description = "Add a new drug into the patient's purchase cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New drug added to purchase cart."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "403", description = "Server refuse client request because violate business logic: only one copy of a drug can be included in the cart."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PostMapping("/patients/{id_pat}/cart/drugs")
    public ResponseEntity<ResponseDTO> savePurchaseDrug(@PathVariable("id_pat") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PBRNNCL54B03F205J") String id_pat,
                                                        @RequestBody @Parameter(name = "Cart id", description = "Purchase cart id.", example = "") PurchaseCartDrugDTO drug){
        try {
            PurchaseCartDrugDTO purchaseDrug = pharmacyService.savePurchaseDrug(id_pat, drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, purchaseDrug);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (ForbiddenException e){
            return exceptionHandler.handleForbiddenException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }

    }

    /**
     * ## PHARMACIST ## Test senza mettere errori: OK
     * Remove from purchase cart a specific drug
     * @param id_pat code of patient
     * @param id_purch_drug code of purchase to delete
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "Delete drug from cart", description = "Delete a specific drug from the patient's purchase cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: drug removed from purchase cart."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Purchase cart or drug not found."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/patients/{id_pat}/cart/drugs/{id_purch_drug}")
    public ResponseEntity<ResponseDTO> deletePurchaseDrug(@PathVariable("id_pat") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PBRNNCL54B03F205J") String id_pat,
                                                          @PathVariable("id_purch_drug") @Parameter(name = "Cart id", description = "Purchase cart id.", example = "") int id_purch_drug){
        try {
            PurchaseCartDrugDTO purchaseDrug = pharmacyService.deletePurchaseDrug(id_pat, id_purch_drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchaseDrug);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (NotFoundException e) {
            return exceptionHandler.handleNotFoundException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## PHARMACIST ##
     * Modify the quantity of the selected drug that is into the purchase cart of patient
     * @param id_pat code of patient
     * @param id_purch_drug code of purchase to delete
     * @param quantity new quantity
     * @return ResponseEntity<?>
     */
    @Operation(summary = "Update drug quantity", description = "Modify the quantity of a drug in the patient's purchase cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: quantity modified."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Purchase cart or drug not found."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PatchMapping("/patients/{id_pat}/cart/drugs/{id_purch_drug}")
    public ResponseEntity<ResponseDTO> modifyPurchaseDrugQuantity(@PathVariable("id_pat") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PBRNNCL54B03F205J") String id_pat,
                                                                  @PathVariable("id_purch_drug") @Parameter(name = "Drug id", description = "OTP drug id.", example = "") int id_purch_drug,
                                                                  @RequestBody @Parameter(name = "Quantity", description = "Drug quantity.", example = "1") int quantity){
        try {
            PurchaseCartDrugDTO purchaseDrug = pharmacyService.modifyPurchaseDrugQuantity(id_pat, id_purch_drug, quantity);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchaseDrug);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (NotFoundException e) {
            return exceptionHandler.handleNotFoundException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## PHARMACIST ##
     * Conferma il pagamento, cancella tutti i farmaci prescritti, controlla quale di questi sono anche
     * parte di qualche prescrizione e la segna come acquistata.
     * Deve anche inserire le informazioni nel document
     * @param id_pat code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "Confirm purchase", description = "Confirm purchase of the cart, removing drugs from it and from prescription cart (prescribed drugs).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: purchase confirmed."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "403", description = "Server refuse client request because violate business logic : patient has no items in the cart."),
            @ApiResponse(responseCode = "404", description = "Purchase cart not found."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PatchMapping("/patients/{id_pat}/cart/checkout")
    public ResponseEntity<ResponseDTO> confirmPurchase(@PathVariable("id_pat") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PBRNNCL54B03F205J") String id_pat,
                                                       @RequestBody @Parameter(name = "Region", description = "Region where is located the pharmacy.", example = "") String pharmacyRegion){
        try {
            LatestPurchase latestPurchase = pharmacyService.confirmPurchase(id_pat, pharmacyRegion);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, latestPurchase);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (ForbiddenException e) {
            return exceptionHandler.handleForbiddenException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
    @Operation(summary = "Update pharmacy information", description = "Modify the doctor's private information: password and owner tax code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: pharmacy private are modified."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyPharmacy(@RequestBody @Parameter(name = "Pharmacy struct", description = "Pharmacy private area.", example = "") Pharmacy pharmacy){
        try {
            Pharmacy pharmacy_ = pharmacyService.modifyPharmacy(pharmacy);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, pharmacy_);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
    @Operation(summary = "Delete pharmacy account", description = "Permanently remove a pharmacy's account from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: pharmacy account successfully deleted."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePharmacy(@PathVariable("id") @Parameter(name = "Identify code", description = "Pharmacy identify code.", example = "Ph40819114812") String id){
        try {
            Pharmacy pharmacy = pharmacyService.deletePharmacy(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, pharmacy);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @Operation(summary = "Get pharmacy by id", description = "Fetch the doctor's private information using their unique identify code: identify code, password, city, region, district, VAT number, name, address, owner tax code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: pharmacy data retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Pharmacy not found."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getPharmacyById(@PathVariable("id") @Parameter(name = "Identify code", description = "Pharmacy identify code.", example = "Ph40819114812") String id){
        try {
            Pharmacy pharmacy = pharmacyService.getPharmacyById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, pharmacy);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (NotFoundException e) {
            return exceptionHandler.handleNotFoundException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
