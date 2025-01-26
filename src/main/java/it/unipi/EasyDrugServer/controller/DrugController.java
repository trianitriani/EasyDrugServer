package it.unipi.EasyDrugServer.controller;


import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.dto.SimpleDrugDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.DrugRepository;
import it.unipi.EasyDrugServer.service.DrugService;
import it.unipi.EasyDrugServer.service.ResearcherService;
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
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
