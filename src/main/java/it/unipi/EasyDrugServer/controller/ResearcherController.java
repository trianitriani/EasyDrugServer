package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.DrugDistributionDTO;
import it.unipi.EasyDrugServer.dto.Order;
import it.unipi.EasyDrugServer.dto.PatientDoctorRatioDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.service.PatientService;
import it.unipi.EasyDrugServer.service.ResearcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteResearcher(String id){
        try {
            Researcher researcher = researcherService.deleteResearcher(id);
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

    @GetMapping("/ratios/patients-to-doctors/order/{order}")
    public ResponseEntity<ResponseDTO> getPatientsToDoctorsRatio(@PathVariable Order order){
        try {
            List<PatientDoctorRatioDTO> list = researcherService.getPatientsToDoctorsRatio(order);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, list);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
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
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
