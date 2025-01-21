package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.PharmacyHomeDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacies")
@RequiredArgsConstructor
public class PharmacyController {
    private final PharmacyService pharmacyService;
    private final GlobalExceptionHandler exceptionHandler;

    /**
     * ## PHARMACIST ##
     * View a patient's cart and him active prescriptions
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @GetMapping("/home/patients/{patCode}")
    public ResponseEntity<ResponseDTO> viewPharmacyHome(@PathVariable String patCode){
        PharmacyHomeDTO pharmacyHomeDTO = pharmacyService.viewPharmacyHome(patCode);
        ResponseDTO response = new ResponseDTO(HttpStatus.OK, pharmacyHomeDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * ## PHARMACIST ## Test senza mettere errori: OK
     * Insert into redis db information related to a specific drug that is insert into a cart
     * of a specific patient by a pharmacist
     * @param patCode code of patient
     * @param drug drug insert into a cart
     * @return ResponseEntity<ResponseDTO>
     */
    @PostMapping("/patients/{patCode}/cart/drugs")
    public ResponseEntity<ResponseDTO> savePurchaseDrug(@PathVariable String patCode,
                                                        @RequestBody PurchaseDrugDTO drug){
        PurchaseDrugDTO purchaseDrug = pharmacyService.savePurchaseDrug(patCode, drug);
        ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, purchaseDrug);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * ## PHARMACIST ## Test senza mettere errori: OK
     * Remove from purchase cart a specific drug
     * @param patCode code of patient
     * @param idDrug code of drug to delete
     * @return ResponseEntity<ResponseDTO>
     */
    @DeleteMapping("/patients/{patCode}/cart/drugs/{idDrug}")
    public ResponseEntity<ResponseDTO> deletePurchaseDrug(@PathVariable String patCode, @PathVariable int idDrug){
        PurchaseDrugDTO purchaseDrug = pharmacyService.deletePurchaseDrug(patCode, idDrug);
        ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchaseDrug);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * ## PHARMACIST ##
     * Modify the quantity of the selected drug that is into the purchase cart of patient
     * @param patCode code of patient
     * @param idDrug code of drug to delete
     * @param quantity new quantity
     * @return ResponseEntity<?>
     */
    @PatchMapping("/patients/{patCode}/cart/drugs/{idDrug}")
    public ResponseEntity<ResponseDTO> modifyPurchaseDrugQuantity(@PathVariable String patCode, @PathVariable int idDrug,
                                                        @RequestBody int quantity){
        try {
            PurchaseDrugDTO purchaseDrug = pharmacyService.modifyPurchaseDrugQuantity(patCode, idDrug, quantity);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchaseDrug);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotFoundException e) {
            return exceptionHandler.handleNotFoundException(e);
        }

    }

    /**
     * ## PHARMACIST ##
     * Conferma il pagamento, cancella tutti i farmaci prescritti, controlla quale di questi sono anche
     * parte di qualche prescrizione e la segna come acquistata.
     * Deve anche inserire le informazioni nel document
     * @param patCode code of patient
     * @return ResponseEntity<ResponseDTO>
     */
    @PatchMapping("/patients/{patCode}/cart/checkout")
    public ResponseEntity<ResponseDTO> confirmPurchase(@PathVariable String patCode){
        List<PurchaseDrugDTO> purchaseCart = pharmacyService.confirmPurchaseCart(patCode);
        ResponseDTO response = new ResponseDTO(HttpStatus.OK, purchaseCart);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
