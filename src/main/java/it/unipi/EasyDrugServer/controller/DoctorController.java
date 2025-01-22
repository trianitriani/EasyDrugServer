package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.DoctorHomeDTO;
import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.*;
import it.unipi.EasyDrugServer.service.DoctorService;
import it.unipi.EasyDrugServer.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final GlobalExceptionHandler exceptionHandler;

    /**
     * ## DOCTOR ##
     * Method to view both the cart of new prescription and a list of active prescriptions
     * related to a specific patient
     * @param docCode code of doctor
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @GetMapping("/{docCode}/home/patients/{patCode}")
    public ResponseEntity<ResponseDTO> viewDoctorHome(@PathVariable String docCode,
                                                      @PathVariable String patCode){
        try {
            DoctorHomeDTO doctorHomeDTO = new DoctorHomeDTO();
            // Ottenere il carrello della prescrizione inattiva
            doctorHomeDTO.setInactivePrescription(doctorService.getInactivePrescription(docCode, patCode));
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
     * @param docCode code of doctor
     * @param patCode code of patient
     * @param drug drug to insert into new prescription
     * @return ResponseEntity<ResponseDTO>
     */
    @PostMapping("/{docCode}/patients/{patCode}/cart/drugs")
    public ResponseEntity<ResponseDTO> saveInactivePrescribedDrug(@PathVariable String docCode, @PathVariable String patCode,
                                                                  @RequestBody PrescribedDrugDTO drug){
        try {
            PrescribedDrugDTO prescribedDrugDTO = doctorService.saveInactivePrescribedDrug(docCode, patCode, drug);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, prescribedDrugDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
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
     * Method for the doctor to delete a specific drug into a new prescription that is
     * still invalid
     * @param docCode code of doctor
     * @param patCode code of patient
     * @param drugId id of drug
     * @return ResponseEntity<ResponseDTO>
     */
    @DeleteMapping("/{docCode}/patients/{patCode}/cart/drugs/{drugId}")
    public ResponseEntity<ResponseDTO> deleteInactivePrescribedDrug(@PathVariable String docCode,
                                                                    @PathVariable String patCode,
                                                                    @PathVariable int drugId){
        try {
            PrescribedDrugDTO prescribedDrugDTO = doctorService.deleteInactivePrescribedDrug(docCode, patCode, drugId);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescribedDrugDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UnauthorizedException e){
            return exceptionHandler.handleUnauthorizedException(e);
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
     * @param docCode code of doctor
     * @param patCode code of patient
     * @param drugId id of drug
     * @param quantity new quantity
     * @return ResponseEntity<ResponseDTO>
     */
    @PatchMapping("/{docCode}/patients/{patCode}/cart/drugs/{drugId}")
    public ResponseEntity<ResponseDTO> modifyInactivePrescribedDrugQuantity(@PathVariable String docCode,
                                                                            @PathVariable String patCode,
                                                                            @PathVariable int drugId,
                                                                            @RequestBody int quantity){
        try {
            PrescribedDrugDTO prescribedDrugDTO = doctorService.modifyInactivePrescribedDrugQuantity(docCode, patCode, drugId, quantity);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescribedDrugDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (UnauthorizedException e){
            return exceptionHandler.handleUnauthorizedException(e);
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
     * @param docCode code of doctor
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @PatchMapping("/{docCode}/patients/{patCode}/cart/activate")
    public ResponseEntity<ResponseDTO> activatePrescription(@PathVariable String docCode,
                                                            @PathVariable String patCode){
        try {
            PrescriptionDTO prescriptionDTO = doctorService.activatePrescription(docCode, patCode);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescriptionDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ForbiddenException e){
            return exceptionHandler.handleForbiddenException(e);
        } catch (UnauthorizedException e){
            return exceptionHandler.handleUnauthorizedException(e);
        } catch (JedisConnectionException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JedisException e){
            return exceptionHandler.handleRedisException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }

    }





}
