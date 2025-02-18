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

import java.net.SocketException;
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

    @GetMapping("/{id_doc}/patients/{id_pat}/prescriptions/from/{n_uploaded}")
    public ResponseEntity<ResponseDTO> getNextPrescriptions(@PathVariable String id_doc,
                                                            @PathVariable String id_pat,
                                                            @PathVariable Integer n_uploaded){
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

    @GetMapping("/{id}/patients/surname/{pat_surname}")
    public ResponseEntity<ResponseDTO> getOwnPatientsBySurname(@PathVariable String id,
                                                      @PathVariable String pat_surname){
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
    @GetMapping("/home/patients/{id_pat}")
    public ResponseEntity<ResponseDTO> viewDoctorHome(@PathVariable String id_pat){
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
    @PostMapping("/patients/{id_pat}/cart/drugs")
    public ResponseEntity<ResponseDTO> saveDrugIntoPrescriptionCart(@PathVariable String id_pat,
                                                                    @RequestBody int id_cart,
                                                                    @RequestBody PrescribedDrugDTO drug,
                                                                    @RequestBody List<String> alreadyInsertedIdDrugs){
        try {
            PrescriptionDTO prescriptionDTO = doctorService.saveDrugIntoPrescriptionCart(id_pat, id_cart, drug, alreadyInsertedIdDrugs);
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
    @DeleteMapping("/patients/{id_pat}/cart/{id_cart}/drugs/{id_drug}")
    public ResponseEntity<ResponseDTO> deleteDrugIntoPrescriptionCart(@PathVariable String id_pat,
                                                                      @PathVariable int id_cart,
                                                                      @PathVariable int id_pres_drug){
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
    @PatchMapping("/patients/{id_pat}/cart/{id_cart}/drugs/{id_pres_drug}")
    public ResponseEntity<ResponseDTO> modifyDrugQuantityIntoPrescriptionCart(@PathVariable String id_pat,
                                                                              @PathVariable int id_cart,
                                                                              @PathVariable int id_pres_drug,
                                                                              @RequestBody int quantity){
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
    @PatchMapping("/patients/{id_pat}/cart/{id_cart}/activate")
    public ResponseEntity<ResponseDTO> activatePrescriptionCart(@PathVariable String id_pat,
                                                                @PathVariable int id_cart,
                                                                @RequestBody List<Integer> id_pres_drugs){
        try {
            PrescriptionDTO prescriptionDTO = doctorService.activatePrescriptionCart(id_pat, id_cart, id_pres_drugs);
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
