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

import java.util.List;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {
    private final DrugService drugService;
    private final GlobalExceptionHandler exceptionHandler;

    @PostMapping
    public ResponseEntity<ResponseDTO> insertDrug(@RequestBody Drug drug){
        try {
            drugService.insertDrug(drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, drug);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyDrug(@RequestBody Drug drug){
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteDrug(@PathVariable int id){
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

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getDrugById(@PathVariable int id){
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
    
    @GetMapping("/search/{name}")
    public ResponseEntity<ResponseDTO> getDrugsThatContain(@PathVariable String name){
        try {
            List<SimpleDrugDTO> drugsDTOs = drugService.getDrugsThatContain(name);
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

    @GetMapping("/search/{name}/purchasable")
    public ResponseEntity<ResponseDTO> getDrugsPurchasableThatContain(@PathVariable String name){
        try {
            List<SimpleDrugDTO> drugsDTOs = drugService.getDrugsPurchasableThatContain(name);
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

    @GetMapping("/search/{name}/on-prescription")
    public ResponseEntity<ResponseDTO> getDrugsOnPrescriptionThatContain(@PathVariable String name){
        try {
            List<SimpleDrugDTO> drugsDTOs = drugService.getDrugsOnPrescriptionThatContain(name);
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

    @GetMapping("/indications/{name}")
    public ResponseEntity<ResponseDTO> getDrugsByIndication(@PathVariable String name) {
        try {
            List<SimpleDrugDTO> drugsDTOs = drugService.getDrugsByIndication(name);
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
