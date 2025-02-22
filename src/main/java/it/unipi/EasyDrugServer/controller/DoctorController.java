package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.*;
import it.unipi.EasyDrugServer.model.Doctor;
import it.unipi.EasyDrugServer.model.LatestPurchase;
import it.unipi.EasyDrugServer.service.DoctorService;
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
import java.net.SocketException;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final GlobalExceptionHandler exceptionHandler;

    @Operation(summary = "Get doctor by id", description = "Return doctor private area if exist: id, password, city, district, region, name, surname, date of birth, gender, tax code, doctor register code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getDoctorById(@PathVariable("id") @Parameter(name = "id", description = "Doctor identify code.", example = "DRSSMTN75E43F205M") String id){
        try {
            Doctor doctor = doctorService.getDoctorById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, doctor);
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
    @Operation(summary = "Modify doctor", description = "Update doctor private area: password, city, district, region.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyDoctor(@RequestBody @Parameter(name = "", description = "", example = "") Doctor doctor){
        try {
            Doctor doctor_ = doctorService.modifyDoctor(doctor);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, doctor_);
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
    @Operation(summary = "Delete doctor", description = "Delete doctor account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteDoctor(@PathVariable("id") @Parameter(name = "id", description = "", example = "DRSSMTN75E43F205M") String id){
        try {
            Doctor doctor = doctorService.deleteDoctor(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, doctor);
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
    @Operation(summary = "Get next prescriptions", description = "Returns next group of 5 prescriptions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id_doc}/patients/{id_pat}/prescriptions/from/{n_uploaded}")
    public ResponseEntity<ResponseDTO> getNextPrescriptions(@PathVariable("id_doc") @Parameter(name = "", description = "", example = "DRSSMTN75E43F205M") String id_doc,
                                                            @PathVariable("id_pat") @Parameter(name = "id_pat", description = "", example = "") String id_pat,
                                                            @PathVariable("n_uploaded") @Parameter(name = "n_uploaded", description = "", example = "") Integer n_uploaded){
        try {
            List<LatestPurchase> purchases = doctorService.getNextPrescriptionDrugs(id_doc, id_pat, n_uploaded);
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
    @Operation(summary = "Get patient by surname", description = "Return a list doctor patient which have a specific surname.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}/patients/surname/{pat_surname}")
    public ResponseEntity<ResponseDTO> getOwnPatientsBySurname(@PathVariable("id") @Parameter(name = "id", description = "", example = "DRSSMTN75E43F205M") String id,
                                                               @PathVariable("pat_surname") @Parameter(name = "pat_surname", description = "", example = "") String pat_surname){
        try {
            List<SimplePatientDTO> simplePatientDTO = doctorService.getOwnPatientsBySurname(id, pat_surname);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, simplePatientDTO);
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
     * ## DOCTOR ##
     * Method to view both the cart of prescription and a list of active prescriptions
     * related to a specific patient
     * @param id_pat code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "View doctor home", description = "Return the cart of prescriptions and a list of active prescriptions related to a patient.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "401", description = "Client doesn't have access rights to the content (unauthorized)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/home/patients/{id_pat}")
    public ResponseEntity<ResponseDTO> viewDoctorHome(@PathVariable("id_pat") @Parameter(name = "id_pat", description = "id_pat", example = "") String id_pat){
        try {
            DoctorHomeDTO doctorHomeDTO = new DoctorHomeDTO();
            // Ottenere il carrello della prescrizione
            doctorHomeDTO.setInactivePrescription(doctorService.getPrescriptionCart(id_pat));
            // Ottenere la lista delle prescrizioni attive
            doctorHomeDTO.setActivePrescriptions(patientService.getAllActivePrescriptions(id_pat));
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, doctorHomeDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UnauthorizedException e){
            return exceptionHandler.handleUnauthorizedException(e);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch(JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## DOCTOR ##
     * Method to insert a new drug into a new prescription that is still invalid
     * @param id_pat code of patient
     * @param drug drug to insert into new prescription
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "Insert drug into cart", description = "Insert a new drug (not inserted before) into the prescription cart of the patient.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New resource created successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "403", description = "Server refuse client request because violate business logic."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PostMapping("/patients/{id_pat}/cart/drugs")
    public ResponseEntity<ResponseDTO> saveDrugIntoPrescriptionCart(@PathVariable("id_pat") @Parameter(name = "id_pat", description = "", example = "") String id_pat,
                                                                    @RequestBody @Parameter(name = "id_cart", description = "", example = "") int id_cart,
                                                                    @RequestBody @Parameter(name = "drug", description = "", example = "") PrescribedDrugDTO drug,
                                                                    @RequestBody @Parameter(name = "alreadyInsertedIdDrugs", description = "", example = "") List<String> alreadyInsertedIdDrugs){
        try {
            PrescriptionDTO prescriptionDTO = doctorService.saveDrugIntoPrescriptionCart(id_pat, id_cart, drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, prescriptionDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ForbiddenException e){
            return exceptionHandler.handleForbiddenException(e);
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

    /**
     * ## DOCTOR ##
     * Method for the doctor to delete a specific drug into a new prescription that is
     * still invalid
     * @param id_pat code of patient
     * @param id_pres_drug id of prescribed drug
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "Delete drug from cart", description = "Remove a specified drug from the prescription cart of the patient.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "404", description = "Server cannot find the requested resource (valid endpoint but resource doesn't exist)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/patients/{id_pat}/cart/{id_cart}/drugs/{id_drug}")
    public ResponseEntity<ResponseDTO> deleteDrugIntoPrescriptionCart(@PathVariable("id_pat") @Parameter(name = "id_pat", description = "", example = "") String id_pat,
                                                                      @PathVariable("id_cart") @Parameter(name = "id_cart", description = "", example = "") int id_cart,
                                                                      @PathVariable("id_pres_drug") @Parameter(name = "id_pres_drug", description = "", example = "") int id_pres_drug){
        try {
            PrescribedDrugDTO prescribedDrugDTO = doctorService.deleteDrugIntoPrescriptionCart(id_pat, id_cart, id_pres_drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescribedDrugDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotFoundException e){
            return exceptionHandler.handleNotFoundException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## DOCTOR ##
     * Method for the doctor to modify quantity of a specific drug related to a specific patient
     * @param id_pat code of patient
     * @param id_cart id of prescription
     * @param id_pres_drug id of prescribed drug
     * @param quantity new quantity
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "Update drug quantity", description = "Update quantity of a drug inside the prescription cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Server cannot find the requested resource (valid endpoint but resource doesn't exist)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PatchMapping("/patients/{id_pat}/cart/{id_cart}/drugs/{id_pres_drug}")
    public ResponseEntity<ResponseDTO> modifyDrugQuantityIntoPrescriptionCart(@PathVariable("id_pat") @Parameter(name = "", description = "", example = "") String id_pat,
                                                                              @PathVariable("id_cart") @Parameter(name = "", description = "", example = "") int id_cart,
                                                                              @PathVariable("id_pres_drug") @Parameter(name = "", description = "", example = "") int id_pres_drug,
                                                                              @RequestBody @Parameter(name = "", description = "", example = "") int quantity){
        try {
            PrescribedDrugDTO prescribedDrugDTO = doctorService.modifyDrugQuantityIntoPrescriptionCart(id_pat, id_cart, id_pres_drug, quantity);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescribedDrugDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (NotFoundException e){
            return exceptionHandler.handleNotFoundException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## DOCTOR ##
     * Active a new prescription, after this the patient and the pharmacist can see it,
     * an active prescription it lasts a month.
     * @param id_pat code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @Operation(summary = "Activate prescription", description = "Activate a prescription of drugs, removing them from the prescription cart and adding in the active prescriptions cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "403", description = "Server refuse client request because violate business logic."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PatchMapping("/patients/{id_pat}/cart/{id_cart}/activate")
    public ResponseEntity<ResponseDTO> activatePrescriptionCart(@PathVariable("id_pat") @Parameter(name = "", description = "", example = "") String id_pat,
                                                                @PathVariable("id_cart") @Parameter(name = "", description = "", example = "") int id_cart){
        try {
            PrescriptionDTO prescriptionDTO = doctorService.activatePrescriptionCart(id_pat, id_cart);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescriptionDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ForbiddenException e){
            return exceptionHandler.handleForbiddenException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
