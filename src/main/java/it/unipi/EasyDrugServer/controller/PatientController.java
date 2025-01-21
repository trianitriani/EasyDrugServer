package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;
    private final GlobalExceptionHandler exceptionHandler;

    /**
     * ## PATIENT ##
     * View to specific patient all him active prescriptions
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @GetMapping("/{patCode}/prescriptions/active")
    public ResponseEntity<ResponseDTO> getAllActivePrescriptions(@PathVariable String patCode){
        List<PrescriptionDTO> prescriptions = patientService.getAllActivePrescriptions(patCode);
        ResponseDTO response = new ResponseDTO(HttpStatus.OK, prescriptions);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
