package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PharmacyHomeDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PharmacyService {
    final PurchaseCartRedisRepository purchaseCartRedisRepository;
    final PrescriptionRedisRepository prescriptionRedisRepository;

    public PharmacyHomeDTO viewPharmacyHome(String patCode) {
        PharmacyHomeDTO pharmacyHomeDTO = new PharmacyHomeDTO();
        // Mostra il carrello degli acquisti del paziente
        pharmacyHomeDTO.setPurchaseCart(purchaseCartRedisRepository.getPurchaseCart(patCode));
        // Mostra tutte le prescrizioni attive del paziente
        pharmacyHomeDTO.setPrescriptions(prescriptionRedisRepository.getAllActivePrescriptions(patCode));
        return pharmacyHomeDTO;
    }

    public PurchaseDrugDTO savePurchaseDrug(String patientCode, PurchaseDrugDTO drug) {
        if(Objects.equals(drug.getName(), ""))
            throw new BadRequestException("Name of the drug can not be null");
        if(drug.getQuantity() < 1)
            throw new BadRequestException("Quantity can not be lower than one");
        return purchaseCartRedisRepository.savePurchaseDrug(patientCode, drug);
    }

    public PurchaseDrugDTO deletePurchaseDrug(String patientCode, int idDrug) {
        return purchaseCartRedisRepository.deletePurchaseDrug(patientCode, idDrug);
    }

    public PurchaseDrugDTO modifyPurchaseDrugQuantity(String patientCode, int idDrug, int quantity) {
        if(quantity == 0)
            return purchaseCartRedisRepository.deletePurchaseDrug(patientCode, idDrug);
        else if(quantity < 0)
            throw new BadRequestException("Quantity can not lower that zero.");
        return purchaseCartRedisRepository.modifyPurchaseDrugQuantity(patientCode, idDrug, quantity);
    }

    public List<PurchaseDrugDTO> confirmPurchaseCart(String patientCode) {
        return purchaseCartRedisRepository.confirmPurchaseCart(patientCode);
    }

}
