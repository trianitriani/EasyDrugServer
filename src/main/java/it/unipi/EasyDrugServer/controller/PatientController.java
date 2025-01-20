package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.dto.ValidResponse;
import it.unipi.EasyDrugServer.service.PatientService;
import it.unipi.EasyDrugServer.model.Patient;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.EntityResponse;

import javax.swing.text.html.parser.Entity;
import java.time.LocalDateTime;
import java.util.HashMap;
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

    /**
     * Inserisce un farmaco all'interno del carrello di farmaci da comprare
     * @param patientCode
     * @param drug
     * @return
     */
    @PostMapping("/{patientCode}/purchases/pending/drugs")
    public ResponseEntity<ValidResponse> savePurchaseDrug(@PathVariable String patientCode,
                                                                  @RequestBody PurchaseDrugDTO drug){
        PurchaseDrugDTO purchaseDrug = patientService.savePurchaseDrug(patientCode, drug);
        ValidResponse response = new ValidResponse(HttpStatus.CREATED, purchaseDrug);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Restituisce il carrello dei farmaci da comprare
     * @param patientCode
     * @return
     */
    @GetMapping("/{patientCode}/purchases/pending")
    public ResponseEntity<ValidResponse> getPurchaseCart(@PathVariable String patientCode){
        List<PurchaseDrugDTO> purchaseCart = patientService.getPurchaseCart(patientCode);
        ValidResponse response = new ValidResponse(HttpStatus.OK, purchaseCart);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Conferma il pagamento, cancella tutti i farmaci prescritti, controlla quale di questi sono anche
     * parte di qualche prescrizione e la segna come acquistata.
     * Deve anche inserire le informazioni nel document
     * @param patientCode
     * @return
     */
    @PostMapping("/{patientCode}/purchases")
    public ResponseEntity<ValidResponse> confirmPurchase(@PathVariable String patientCode){
        List<PurchaseDrugDTO> purchaseCart = patientService.confirmPurchaseCart(patientCode);
        ValidResponse response = new ValidResponse(HttpStatus.OK, purchaseCart);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Modifica la quantità di un determinato farmaco che è inserito nel carrello degli
     * acquisti.
     * @param patientCode
     * @param idDrug
     * @param quantity
     * @return
     */
    @PatchMapping("/{patientCode}/drugsToPurchase/{idDrug}")
    public ResponseEntity<ValidResponse> modifyPurchaseDrugQuantity(@PathVariable String patientCode, @PathVariable int idDrug,
                                                                    @RequestBody int quantity){
        PurchaseDrugDTO purchaseDrug = patientService.modifyPurchaseDrugQuantity(patientCode, idDrug, quantity);
        ValidResponse response = new ValidResponse(HttpStatus.OK, purchaseDrug);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Rimuove dal carrello degli acquisti un determinato farmaco
     * @param patientCode
     * @param idDrug
     * @return
     */
    @DeleteMapping("/{patientCode}/drugsToPurchase/{idDrug}")
    public ResponseEntity<ValidResponse> deletePurchaseDrug(@PathVariable String patientCode, @PathVariable int idDrug){
        PurchaseDrugDTO purchaseDrug = patientService.deletePurchaseDrug(patientCode, idDrug);
        ValidResponse response = new ValidResponse(HttpStatus.OK, purchaseDrug);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Mostra al paziente le sue prescrizioni attive, usata quando entra nella sua home.
     * @param patientCode codice del paziente
     * @return ResponseEntity
     */
    @GetMapping("/{patientCode}/prescriptions/active")
    public ResponseEntity<ValidResponse> getAllPrescriptions(@PathVariable String patientCode){
        List<PrescriptionDTO> prescriptions = patientService.getAllPrescriptions(patientCode);
        ValidResponse response = new ValidResponse(HttpStatus.OK, prescriptions);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
