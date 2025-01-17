package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import it.unipi.EasyDrugServer.model.PurchaseCart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PurchaseCartRedisRepository purchaseCartRedisRepository;

    /*
    public PatientService(PurchaseCartRedisRepository purchaseCartRedisRepository) {
        this.purchaseCartRedisRepository = purchaseCartRedisRepository;
    }

     */



    public Patient getPatient(String codePatient){
        return purchaseCartRedisRepository.findByCode(codePatient);
    }

    public String saveDrugIntoPurchaseCart(String codPatient, Drug drug) {
        purchaseCartRedisRepository.saveDrugIntoPurchaseCart(codPatient, drug);
        return null;
    }

    public PurchaseCart getPurchaseCart(String patientCode){
        return purchaseCartRedisRepository.getPurchaseCart(patientCode);
    }

    public int confirmPurchase(String patientCode){
        return purchaseCartRedisRepository.confirmPurchase(patientCode);
    }

}
