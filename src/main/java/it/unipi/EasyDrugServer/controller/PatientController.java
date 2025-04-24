package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.AccountPatientDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
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

    @Operation(summary = "Get patient by id", description = "Fetch the patient's private information using their unique identify code: identify code, password, city, district, region, name, surname, date of birth, gender, tax code, doctor identify code, latest purchase, purchase id list, prescription id list.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: patient data retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Patient not found."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getPatientById(@PathVariable("id") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PDRSNGL17C43E239B") String id){
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

    @Operation(summary = "Get patient account by id.", description = "Similar to Get patient by id, but retrieve only valuable information for the user: name, surname, password,region, city, district, tax code, date of birth, doctor identify code, gender.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: patient data retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Patient not found."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}/profile")
    public ResponseEntity<ResponseDTO> getAccountPatientById(@PathVariable("id") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PDRSNGL17C43E239B") String id){
        try {
            AccountPatientDTO patient = patientService.getAccountPatientById(id);
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
    @Operation(summary = "Update patient information", description = "Modify the doctor's private information, including city, district, region, doctor identify code and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: patient private area modified."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyPatient(@RequestBody @Parameter(name = "Patient struct", description = "Patient private area") AccountPatientDTO patient){
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
    @Operation(summary = "Delete patient account", description = "Permanently remove a patient's account from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: patient account successfully deleted."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePatient(@PathVariable("id") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PDRSNGL17C43E239B") String id){
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
    @Operation(summary = "Get latest purchases", description = "Retrieve a list of the last 10 purchases of a specified patient.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: list of last purchases returned."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}/purchases/latest")
    public ResponseEntity<ResponseDTO> getLatestPurchases(@PathVariable("id") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PDRSNGL17C43E239B") String id){
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
    @Operation(summary = "Get next purchases", description = "Retrieve next group of 5 purchases of a specified patient.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: list of purchases returned."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}/purchases/from/{n_uploaded}")
    public ResponseEntity<ResponseDTO> getNextPurchases(@PathVariable("id") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PDRSNGL17C43E239B") String id,
                                                        @PathVariable("n_uploaded") @Parameter(name = "Index of elements", description = "Index of the last element showed", example = "0") int n_uploaded){
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
    @Operation(summary = "Get active prescriptions", description = "Retrieve the list of active prescriptions of a specified patient.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: list of active prescriptions returned."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{patCode}/prescriptions/active")
    public ResponseEntity<ResponseDTO> getAllActivePrescriptions(@PathVariable("patCode") @Parameter(name = "Identify code", description = "Patient identify code.", example = "PDRSNGL17C43E239B") String patCode){
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
