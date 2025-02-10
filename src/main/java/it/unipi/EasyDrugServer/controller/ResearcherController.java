package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.service.ResearcherService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

import java.util.List;


@RestController
@RequestMapping("/api/researchers")
@RequiredArgsConstructor
public class ResearcherController {
    private final ResearcherService researcherService;
    private final GlobalExceptionHandler exceptionHandler;

    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyResearcher(Researcher researcher){
        try {
            Researcher researcher_ = researcherService.modifyResearcher(researcher);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, researcher_);
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
    public ResponseEntity<ResponseDTO> deleteResearcher(String id){
        try {
            Researcher researcher = researcherService.deleteResearcher(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, researcher);
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
    public ResponseEntity<ResponseDTO> getResearcherById(String id){
        try {
            Researcher researcher = researcherService.getResearcherById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, researcher);
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

    @GetMapping("/ratios/patients-to-doctors/order/{order}")
    public ResponseEntity<ResponseDTO> getPatientsToDoctorsRatio(@PathVariable Order order) {
        try {
            List<PatientDoctorRatioDTO> list = researcherService.getPatientsToDoctorsRatio(order);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, list);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @GetMapping("/purchases/top/{top}/from/{from}/to/{to}")
    public ResponseEntity<ResponseDTO> getTopPurchases(@PathVariable int top,
                                                       @PathVariable LocalDate from,
                                                       @PathVariable LocalDate to){
        try{
            List<TopDrugDTO> topDrugs = researcherService.getTopPurchases(top, from, to);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, topDrugs);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @GetMapping("/drugs/{id_drug}/distribution/order/{order}")
    public ResponseEntity<ResponseDTO> getDistributionByDrug(@PathVariable String id_drug,
                                                             @PathVariable Order order){
        try {
            List<DrugDistributionDTO> list = researcherService.getDistributionByDrug(id_drug, order);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, list);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotFoundException e) {
            return exceptionHandler.handleNotFoundException(e);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    // Il ricercatore può vedere per quali indicazioni ci sono meno farmaci (top 15) e
    // mostrare quanti farmaci e quali farmaci per ognuna.
    @GetMapping("/indications/less-drugs/top/{top}")
    public ResponseEntity<ResponseDTO> getIndicationsWithLessDrugs(@PathVariable int top){
        try{
            List<TopRareIndicationDTO> diseases = researcherService.getIndicationsWithLessDrugs(top);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, diseases);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
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
