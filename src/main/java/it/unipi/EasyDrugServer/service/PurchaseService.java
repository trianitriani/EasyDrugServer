package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Purchase;
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

    public Purchase getPurchaseById(String id) {
        Optional<Purchase> optPurchase = purchaseRepository.findById(id);
        if(optPurchase.isPresent())
            return optPurchase.get();
        throw new NotFoundException("Purchase "+id+" does not exists");
    }

    public void modifyPurchase(Purchase purchase) {
        if(purchaseRepository.existsById(purchase.getId())) {
            purchaseRepository.save(purchase);
        } else throw new NotFoundException("Purchase "+ purchase.getId() +" does not exists");
    }

    public Purchase deletePurchase(String id) {
        Purchase purchase = getPurchaseById(id);
        purchaseRepository.deleteById(id);
        return purchase;
    }
}
