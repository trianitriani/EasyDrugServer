package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRepository;
import it.unipi.EasyDrugServer.model.PurchaseCart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PurchaseCartRepository purchaseCartRepository;

    public Patient getPatient(String codePatient){
        return purchaseCartRepository.findByCode(codePatient);
    }

    public String saveDrugIntoPurchaseCart(String codPatient, Drug drug) {
        purchaseCartRepository.saveDrugIntoPurchaseCart(codPatient, drug);
        return null;
    }

    public PurchaseCart getPurchaseCart(String patientCode){
        return purchaseCartRepository.getPurchaseCart(patientCode);
    }

}
