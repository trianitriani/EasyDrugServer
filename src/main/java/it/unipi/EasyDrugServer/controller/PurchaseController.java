package it.unipi.EasyDrugServer.controller;


import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.exception.NotFoundException;
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

    @Operation(summary = "Insert purchase", description = "Add a new drug purchase in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New purchase created successfully."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PostMapping
    public ResponseEntity<ResponseDTO> insertPurchase(@RequestBody @Parameter(name = "Purchase struct", description = "Purchase details.") Purchase purchase){
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

    @Operation(summary = "Get purchase by id", description = "Fetch information about a purchase using their unique id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: purchase data retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Purchase not found."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getPurchaseById(@PathVariable("id") @Parameter(name = "Purchase id", description = "Purchase id.", example = "67aba93a5da6705a000d803b") String id){
        try {
            Purchase purchase = purchaseService.getPurchaseById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchase);
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
    @Operation(summary = "Update purchase information", description = "Update purchase details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: purchase details modified."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping
    public ResponseEntity<ResponseDTO> modifyPurchase(@RequestBody @Parameter(name = "Purchase struct", description = "Purchase details.", example = "") Purchase purchase){
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
    @Operation(summary = "Delete purchase by id", description = "Permanently remove a purchase from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: purchase successfully deleted."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePurchase(@PathVariable("id") @Parameter(name = "Purchase id", description = "Purchase id.", example = "67aba93a5da6705a000d803b") String id){
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
