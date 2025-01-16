package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.service.PatientService;
import it.unipi.EasyDrugServer.model.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @GetMapping("/{patientCode}")
    public Patient getPatient(@PathVariable String patientCode){
        return patientService.getPatient(patientCode);
    }

    @PostMapping("/{codPatient}/drugsToPurchase")
    public String saveDrugsToPurchase(@PathVariable String codPatient, @RequestBody Drug drug){
        return patientService.saveDrugToPurchase(codPatient, drug);
    }

    @GetMapping("/{codPatient}/purchases")
    public List<Drug> getCart(@PathVariable String patientCode){
        return patientService.getCart(patientCode);
    }
}
