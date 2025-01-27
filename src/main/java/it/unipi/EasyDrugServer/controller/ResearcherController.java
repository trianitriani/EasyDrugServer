package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.dto.TopDrugDTO;
import it.unipi.EasyDrugServer.dto.TopRareDiseaseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.service.ResearcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
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
            researcherService.modifyResearcher(researcher);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, researcher);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @DeleteMapping()
    public ResponseEntity<ResponseDTO> deleteResearcher(Researcher researcher){
        try {
            researcherService.deleteResearcher(researcher);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, researcher);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
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
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @GetMapping("/purchases/top/{top}/from/{from}/to/{to}")
    public ResponseEntity<ResponseDTO> getTopPurchases(@PathVariable int top, @PathVariable LocalDate from,
                                                       @PathVariable LocalDate to){
        try{
            List<TopDrugDTO> topDrugs = researcherService.getTopPurchases(top, from, to);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, topDrugs);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    // Il ricercatore può vedere per quali malattie ci sono meno farmaci (top 15) e mostrare quanti farmaci e quali farmaci per ognuna.
    @GetMapping("/diseases/less-drugs/top/{top}")
    public ResponseEntity<ResponseDTO> getDiseasesWithLessDrugs(@PathVariable int top){
        try{
            List<TopRareDiseaseDTO> diseases = researcherService.getDiseasesWithLessDrugs(top);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, diseases);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
