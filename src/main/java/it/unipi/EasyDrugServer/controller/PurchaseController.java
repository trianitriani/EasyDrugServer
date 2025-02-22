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
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final GlobalExceptionHandler exceptionHandler;

    @Operation(summary = "Insert purchase", description = "Insert a purchase in purchases collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New resource created successfully."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PostMapping
    public ResponseEntity<ResponseDTO> insertPurchase(@RequestBody @Parameter(name = "", description = "", example = "") Purchase purchase){
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
    @Operation(summary = "Get purchase by id", description = "Returns information about a specified purchase in purchases collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getPurchaseById(@PathVariable("id") @Parameter(name = "", description = "", example = "") String id){
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
    @Operation(summary = "Update purchase by id", description = "Update purchase information in the purchases collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping
    public ResponseEntity<ResponseDTO> modifyPurchase(@RequestBody @Parameter(name = "", description = "", example = "") Purchase purchase){
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
    @Operation(summary = "Delete purchase by id", description = "Delete a specified purchase from purchases collection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePurchase(@PathVariable("id") @Parameter(name = "", description = "", example = "") String id){
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
