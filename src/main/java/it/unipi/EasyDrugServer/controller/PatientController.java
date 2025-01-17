package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
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

    @GetMapping("/{codPatient}")
    public Patient getPatient(@PathVariable String codPatient){
        return patientService.getPatient(codPatient);
    }

    @PostMapping("/{codPatient}/drugsToPurchase")
    public String saveDrugIntoPurchaseCart(@PathVariable String codPatient, @RequestBody PurchaseDrugDTO drug){
        return patientService.saveDrugIntoPurchaseCart(codPatient, drug);
    }

    @GetMapping("/{codPatient}/purchases")
    public List<PurchaseDrugDTO> getPurchaseCart(@PathVariable String codPatient){
        return patientService.getPurchaseCart(codPatient);
    }

    @PostMapping("/{codPatient}/purchases")
    public int confirmPurchase(@PathVariable String codPatient){
        return patientService.confirmPurchase(codPatient);
    }
}
