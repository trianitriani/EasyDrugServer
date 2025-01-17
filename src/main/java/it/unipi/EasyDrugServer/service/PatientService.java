package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public String saveDrugIntoPurchaseCart(String codPatient, PurchaseDrugDTO drug) {
        purchaseCartRedisRepository.saveDrugIntoPurchaseCart(codPatient, drug);
        return null;
    }

    public List<PurchaseDrugDTO> getPurchaseCart(String patientCode){
        return purchaseCartRedisRepository.getPurchaseCart(patientCode);
    }

    public int confirmPurchase(String patientCode){
        return purchaseCartRedisRepository.confirmPurchase(patientCode);
    }

}
