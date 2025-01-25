package it.unipi.EasyDrugServer.controller;


import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.service.DrugService;
import it.unipi.EasyDrugServer.service.ResearcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {
    private final DrugService drugService;
    private final GlobalExceptionHandler exceptionHandler;

    @PostMapping
    public ResponseEntity<ResponseDTO> insertDrug(Drug drug){
        try {
            drugService.insertDrug(drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, drug);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyDrug(Drug drug){
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

    @DeleteMapping()
    public ResponseEntity<ResponseDTO> deleteDrug(Drug drug){
        try {
            drugService.deleteDrug(drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, drug);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getDrugById(int id){
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
}
