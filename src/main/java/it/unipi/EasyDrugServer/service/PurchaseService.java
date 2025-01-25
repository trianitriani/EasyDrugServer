package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Doctor;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Purchase;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;

    public void insertPurchase(Purchase purchase){
        purchaseRepository.save(purchase);
    }

    public Purchase getPurchaseById(Integer id) {
        Optional<Purchase> optPurchase = purchaseRepository.findById(id);
        if(optPurchase.isPresent())
            return optPurchase.get();
        throw new NotFoundException("Researcher "+id+" does not exists");
    }

    public void modifyPurchase(Purchase purchase) {
        if(purchaseRepository.existsById(purchase.getId())) {
            purchaseRepository.save(purchase);
        } else throw new NotFoundException("Researcher "+ purchase.getId() +" does not exists");
    }

    public void deletePurchase(Purchase purchase) {
        if(purchaseRepository.existsById(purchase.getId())) {
            purchaseRepository.delete(purchase);
        } else throw new NotFoundException("Researcher "+purchase.getId()+" does not exists");
    }
}
