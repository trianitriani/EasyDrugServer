package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PurchaseCartRedisRepository purchaseCartRedisRepository;
    private final PrescriptionRedisRepository prescriptionRedisRepository;

    public Patient getPatient(String patientCode){
        return null;
    }

    public PurchaseDrugDTO savePurchaseDrug(String patientCode, PurchaseDrugDTO drug) {
        return purchaseCartRedisRepository.savePurchaseDrug(patientCode, drug);
    }

    public List<PurchaseDrugDTO> getPurchaseCart(String patientCode){
        return purchaseCartRedisRepository.getPurchaseCart(patientCode);
    }

    public List<PurchaseDrugDTO> confirmPurchaseCart(String patientCode){
        return purchaseCartRedisRepository.confirmPurchaseCart(patientCode);
    }

    public PurchaseDrugDTO modifyPurchaseDrugQuantity(String patientCode, int idDrug, int quantity) throws NotFoundException {
        PurchaseDrugDTO purchaseDrugDTO = purchaseCartRedisRepository.modifyPurchaseDrugQuantity(patientCode, idDrug, quantity);
        if(purchaseDrugDTO == null)
            throw new NotFoundException("Impossibile modify the drug: patient "+patientCode+" has not drug with id "+idDrug+" in the cart.");
        return purchaseDrugDTO;
    }

    public PurchaseDrugDTO deletePurchaseDrug(String patientCode, int idDrug) throws NotFoundException {
        PurchaseDrugDTO purchaseDrugDTO =  purchaseCartRedisRepository.deletePurchaseDrug(patientCode, idDrug);
        if(purchaseDrugDTO == null)
            throw new NotFoundException("Impossibile delete the drug: patient "+patientCode+" has not drug with id "+idDrug+" in the cart.");
        return purchaseDrugDTO;
    }

    public List<PrescriptionDTO> getAllPrescriptions(String patientCode){
        return prescriptionRedisRepository.getAllPrescriptions(patientCode);
    }
}
