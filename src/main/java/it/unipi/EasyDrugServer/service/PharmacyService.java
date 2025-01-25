package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PharmacyHomeDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Pharmacy;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.PharmacyRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PharmacyService {
    private final PurchaseCartRedisRepository purchaseCartRedisRepository;
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final PharmacyRepository pharmacyRepository;

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

    public Pharmacy getPharmacyById(String id) {
        Optional<Pharmacy> optPharmacy = pharmacyRepository.findById(id);
        if(optPharmacy.isPresent()) return optPharmacy.get();
        throw new NotFoundException("Patient "+id+" does not exists");
    }

    public void modifyPharmacy(Pharmacy pharmacy) {
        if(pharmacyRepository.existsById(pharmacy.getIdentifyCode())) {
            pharmacyRepository.save(pharmacy);
        } else throw new NotFoundException("Researcher "+pharmacy.getIdentifyCode()+" does not exists");
    }

    public void deletePharmacy(Pharmacy pharmacy) {
        if(pharmacyRepository.existsById(pharmacy.getIdentifyCode())) {
            pharmacyRepository.delete(pharmacy);
        } else throw new NotFoundException("Researcher "+pharmacy.getIdentifyCode()+" does not exists");
    }

}
