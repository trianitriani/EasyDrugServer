package it.unipi.EasyDrugServer.controller;


import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.model.Doctor;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Purchase;
import it.unipi.EasyDrugServer.service.DrugService;
import it.unipi.EasyDrugServer.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final GlobalExceptionHandler exceptionHandler;

    @PostMapping
    public ResponseEntity<ResponseDTO> insertPurchase(Purchase purchase){
        try {
            purchaseService.insertPurchase(purchase);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, purchase);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getPurchaseById(String id){
        try {
            Purchase purchase = purchaseService.getPurchaseById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchase);
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

    @PutMapping
    public ResponseEntity<ResponseDTO> modifyPurchase(Purchase purchase){
        try {
            purchaseService.modifyPurchase(purchase);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchase);
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
    public ResponseEntity<ResponseDTO> deletePurchase(String id){
        try {
            Purchase purchase = purchaseService.deletePurchase(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchase);
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
