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

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final GlobalExceptionHandler exceptionHandler;

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getDoctorById(@PathVariable String id){
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

    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyDoctor(@RequestBody Doctor doctor){
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteDoctor(@PathVariable String id){
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

    /*
    @GetMapping("/{id_doc}/patients/{id_pat}/prescriptions/latest")
    public ResponseEntity<ResponseDTO> getLatestPrescriptions(@PathVariable String id_doc,
                                                              @PathVariable String id_pat){
        try {
            List<PrescriptionDTO> prescriptions = doctorService.getLatestPrescriptions(id_doc, id_pat);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescriptions);
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
    }*/

    @GetMapping("/{id_doc}/patients/{id_pat}/prescriptions/from/{nAlreadyViewed}")
    public ResponseEntity<ResponseDTO> getNextPrescriptions(@PathVariable String id_doc,
                                                            @PathVariable String id_pat,
                                                            @PathVariable Integer nAlreadyViewed){
        try {
            List<LatestPurchase> purchases = doctorService.getNextPrescriptionDrugs(id_doc, id_pat, nAlreadyViewed);
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

    @GetMapping("/{id}/patients/surname/{patSurname}")
    public ResponseEntity<ResponseDTO> getOwnPatients(@PathVariable String id,
                                                      @PathVariable String patSurname){
        try {
            List<SimplePatientDTO> simplePatientDTO = doctorService.getOwnPatients(id, patSurname);
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
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @GetMapping("/home/patients/{patCode}")
    public ResponseEntity<ResponseDTO> viewDoctorHome(@PathVariable String patCode){
        try {
            DoctorHomeDTO doctorHomeDTO = new DoctorHomeDTO();
            // Ottenere il carrello della prescrizione inattiva
            doctorHomeDTO.setInactivePrescription(doctorService.getInactivePrescription(patCode));
            // Ottenere la lista delle prescrizioni attive
            doctorHomeDTO.setActivePrescriptions(patientService.getAllActivePrescriptions(patCode));
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, doctorHomeDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UnauthorizedException e){
            return exceptionHandler.handleUnauthorizedException(e);
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

    /**
     * ## DOCTOR ##
     * Method to insert a new drug into a new prescription that is still invalid
     * @param patCode code of patient
     * @param drug drug to insert into new prescription
     * @return ResponseEntity<ResponseDTO>
     */
    @PostMapping("/patients/{patCode}/cart/drugs")
    public ResponseEntity<ResponseDTO> saveInactivePrescribedDrug(@PathVariable String patCode,
                                                                  @RequestBody PrescribedDrugDTO drug){
        try {
            PrescribedDrugDTO prescribedDrugDTO = doctorService.saveInactivePrescribedDrug(patCode, drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, prescribedDrugDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ForbiddenException e){
            return exceptionHandler.handleForbiddenException(e);
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

    /**
     * ## DOCTOR ##
     * Method for the doctor to delete a specific drug into a new prescription that is
     * still invalid
     * @param patCode code of patient
     * @param drugId id of drug
     * @return ResponseEntity<ResponseDTO>
     */
    @DeleteMapping("/patients/{patCode}/cart/drugs/{drugId}")
    public ResponseEntity<ResponseDTO> deleteInactivePrescribedDrug(@PathVariable String patCode,
                                                                    @PathVariable String drugId){
        try {
            PrescribedDrugDTO prescribedDrugDTO = doctorService.deleteInactivePrescribedDrug(patCode, drugId);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescribedDrugDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotFoundException e){
            return exceptionHandler.handleNotFoundException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## DOCTOR ##
     * Method for the doctor to modify quantity of a specific drug related to a specific patient
     * @param patCode code of patient
     * @param drugId id of drug
     * @param quantity new quantity
     * @return ResponseEntity<ResponseDTO>
     */
    @PatchMapping("/patients/{patCode}/cart/drugs/{drugId}")
    public ResponseEntity<ResponseDTO> modifyInactivePrescribedDrugQuantity(@PathVariable String patCode,
                                                                            @PathVariable String drugId,
                                                                            @RequestBody int quantity){
        try {
            PrescribedDrugDTO prescribedDrugDTO = doctorService.modifyInactivePrescribedDrugQuantity(patCode, drugId, quantity);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescribedDrugDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (NotFoundException e){
            return exceptionHandler.handleNotFoundException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    /**
     * ## DOCTOR ##
     * Active a new prescription, after this the patient and the pharmacist can see it,
     * an active prescription it lasts a month.
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @PatchMapping("/patients/{patCode}/cart/activate")
    public ResponseEntity<ResponseDTO> activatePrescription(@PathVariable String patCode){
        try {
            PrescriptionDTO prescriptionDTO = doctorService.activatePrescription(patCode);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescriptionDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ForbiddenException e){
            return exceptionHandler.handleForbiddenException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
