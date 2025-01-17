package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.PurchaseCart;
import it.unipi.EasyDrugServer.service.PatientService;
import it.unipi.EasyDrugServer.model.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    /*
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

     */

    @GetMapping("/{codPatient}")
    public Patient getPatient(@PathVariable String codPatient){
        return patientService.getPatient(codPatient);
    }

    @PostMapping("/{codPatient}/drugsToPurchase")
    public String saveDrugIntoPurchaseCart(@PathVariable String codPatient, @RequestBody Drug drug){
        return patientService.saveDrugIntoPurchaseCart(codPatient, drug);
    }

    @GetMapping("/{codPatient}/purchases")
    public PurchaseCart getPurchaseCart(@PathVariable String codPatient){
        return patientService.getPurchaseCart(codPatient);
    }

    @PostMapping("/{codPatient}/purchases")
    public int confirmPurchase(@PathVariable String codPatient){
        return patientService.confirmPurchase(codPatient);
    }
}
