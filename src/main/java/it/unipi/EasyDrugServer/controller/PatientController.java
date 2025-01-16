package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.service.PatientService;
import it.unipi.EasyDrugServer.model.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @GetMapping("/{patientCode}")
    public Patient getPatient(@PathVariable String patientCode){
        return patientService.getPatient(patientCode);
    }

}
