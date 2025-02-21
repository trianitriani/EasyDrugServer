package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.AccountPatientDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.LatestPurchase;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;
    private final GlobalExceptionHandler exceptionHandler;

    @Operation(summary = "", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Server cannot find the requested resource (valid endpoint but resource doesn't exist)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getPatientById(@PathVariable String id){
        try {
            Patient patient = patientService.getPatientById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, patient);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
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
    @Operation(summary = "", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}/profile")
    public ResponseEntity<ResponseDTO> getAccountPatientById(@PathVariable String id){
        try {
            AccountPatientDTO patient = patientService.getAccountPatientById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, patient);
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
    @Operation(summary = "", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyPatient(@RequestBody AccountPatientDTO patient){
        try {
            AccountPatientDTO patient_ = patientService.modifyPatient(patient);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, patient_);
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
    @Operation(summary = "", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePatient(@PathVariable String id){
        try {
            Patient patient = patientService.deletePatient(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, patient);
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
    @Operation(summary = "", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}/purchases/latest")
    public ResponseEntity<ResponseDTO> getLatestPurchases(@PathVariable String id){
        try {
            List<LatestPurchase> purchases = patientService.getLatestPurchases(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchases);
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
    @Operation(summary = "", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}/purchases/from/{n_uploaded}")
    public ResponseEntity<ResponseDTO> getNextPurchases(@PathVariable String id,
                                                        @PathVariable int n_uploaded){
        try {
            List<LatestPurchase> purchases = patientService.getNextPurchaseDrugs(id, n_uploaded);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchases);
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

    /**
     * ## PATIENT ##
     * View to specific patient all him active prescriptions
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{patCode}/prescriptions/active")
    public ResponseEntity<ResponseDTO> getAllActivePrescriptions(@PathVariable String patCode){
        try {
            List<PrescriptionDTO> prescriptions = patientService.getAllActivePrescriptions(patCode);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescriptions);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }

    }
}
