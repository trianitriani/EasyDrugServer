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
     * View a patient's cart and him active prescriptions
     * @param id_pat code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @GetMapping("/home/patients/{id_pat}")
    public ResponseEntity<ResponseDTO> viewPharmacyHome(@PathVariable String id_pat){
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
    @PostMapping("/patients/{id_pat}/cart/drugs")
    public ResponseEntity<ResponseDTO> savePurchaseDrug(@PathVariable String id_pat,
                                                        @RequestBody PurchaseCartDrugDTO drug,
                                                        @RequestBody List<String> alreadyInsertedIdDrugs){
        try {
            PurchaseCartDrugDTO purchaseDrug = pharmacyService.savePurchaseDrug(id_pat, drug, alreadyInsertedIdDrugs);
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
    @DeleteMapping("/patients/{id_pat}/cart/drugs/{id_purch_drug}")
    public ResponseEntity<ResponseDTO> deletePurchaseDrug(@PathVariable String id_pat,
                                                          @PathVariable int id_purch_drug){
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
    @PatchMapping("/patients/{id_pat}/cart/drugs/{id_purch_drug}")
    public ResponseEntity<ResponseDTO> modifyPurchaseDrugQuantity(@PathVariable String id_pat,
                                                                  @PathVariable int id_purch_drug,
                                                                  @RequestBody int quantity){
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
    @PatchMapping("/patients/{id_pat}/cart/checkout")
    public ResponseEntity<ResponseDTO> confirmPurchase(@PathVariable String id_pat,
                                                       @RequestBody List<Integer> id_purch_drugs,
                                                       @RequestBody String pharmacyRegion){
        try {
            LatestPurchase latestPurchase = pharmacyService.confirmPurchase(id_pat, id_purch_drugs, pharmacyRegion);
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

    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyPharmacy(@RequestBody Pharmacy pharmacy){
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePharmacy(@PathVariable String id){
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

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getPharmacyById(@PathVariable String id){
        try {
            Pharmacy pharmacy = pharmacyService.getPharmacyById(id);
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
}
