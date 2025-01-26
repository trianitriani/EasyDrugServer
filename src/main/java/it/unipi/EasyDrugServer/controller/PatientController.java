package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.model.LatestPurchase;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;
    private final GlobalExceptionHandler exceptionHandler;

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getPatientById(@PathVariable String id){
        try {
            Patient patient = patientService.getPatientById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, patient);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyPatient(@RequestBody Patient patient){
        try {
            patientService.modifyPatient(patient);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, patient);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePatient(@PathVariable String id){
        try {
            Patient patient = patientService.deletePatient(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, patient);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @GetMapping("/{id}/purchases/latest")
    public ResponseEntity<ResponseDTO> getLatestPurchases(@PathVariable String id){
        try {
            List<LatestPurchase> purchases = patientService.getLatestPurchases(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchases);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @GetMapping("/{id}/purchases/from/{from}/to/{to}")
    public ResponseEntity<ResponseDTO> getPurchasesFromTo(@PathVariable String id,
                                                          @PathVariable LocalDate from, @PathVariable LocalDate to){
        try {
            List<LatestPurchase> purchases = patientService.getPurchasesFromTo(id, from, to);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchases);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## PATIENT ##
     * View to specific patient all him active prescriptions
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @GetMapping("/{patCode}/prescriptions/active")
    public ResponseEntity<ResponseDTO> getAllActivePrescriptions(@PathVariable String patCode){
        try {
            List<PrescriptionDTO> prescriptions = patientService.getAllActivePrescriptions(patCode);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescriptions);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }

    }
}
