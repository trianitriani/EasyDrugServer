package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.dto.SimpleDrugDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {
    private final DrugService drugService;
    private final GlobalExceptionHandler exceptionHandler;

    //cambiare tutti gli how to handle da ho to handle
    @Operation(summary = "Insert drug", description = "Insert a new drug in the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New resource created successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PostMapping
    public ResponseEntity<ResponseDTO> insertDrug(@RequestBody @Parameter(name = "", description = "", example = "") Drug drug){
        try {
            drugService.insertDrug(drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, drug);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
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
    @Operation(summary = "Update drug by id", description = "Update drug details: drug name, price, IUPAC, SMILES, list of indiations, list of side effects, company, active ingredient, prescribable.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyDrug(@RequestBody @Parameter(name = "", description = "", example = "") Drug drug){
        try {
            drugService.modifyDrug(drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, drug);
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
    @Operation(summary = "Delete drug by id", description = "Delete drug from the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteDrug(@PathVariable("id") @Parameter(name = "", description = "", example = "") String id){
        try {
            Drug drug = drugService.deleteDrug(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, drug);
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
    @Operation(summary = "Get drug by id", description = "Returns information about a specified drug.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getDrugById(@PathVariable("id") @Parameter(name = "", description = "", example = "") String id){
        try {
            Drug drug = drugService.getDrugById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, drug);
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
    //qui come in molte atre manca la not found exception

    @Operation(summary = "Get drugs by name", description = "Returns a list of drug, which start with a specified name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/search/{name}")
    public ResponseEntity<ResponseDTO> getDrugThatStartWith(@PathVariable("name") @Parameter(name = "", description = "", example = "") String name){
        try {
            List<SimpleDrugDTO> drugsDTOs = drugService.getDrugThatStartWith(name);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, drugsDTOs);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
    @Operation(summary = "Get OTP drugs by name", description = "Returns a list of OTP drug, which start with a specified name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/search/{name}/otp")
    public ResponseEntity<ResponseDTO> getOPTDrugsThatStartWith(@PathVariable("name") @Parameter(name = "", description = "", example = "") String name){
        try {
            List<SimpleDrugDTO> drugsDTOs = drugService.getOPTDrugsThatStartWith(name);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, drugsDTOs);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
    @Operation(summary = "Get prescribable drug by name", description = "Returns a list of prescribable drug, which start with a specified name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/search/{name}/on-prescription")
    public ResponseEntity<ResponseDTO> getOnPrescriptionDrugsThatStartWith(@PathVariable("name") @Parameter(name = "", description = "", example = "") String name){
        try {
            List<SimpleDrugDTO> drugsDTOs = drugService.getOnPrescriptionDrugsThatStartWith(name);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, drugsDTOs);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
