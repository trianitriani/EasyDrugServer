package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.DrugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DrugService {
    private final DrugRepository drugRepository;

    public Drug getDrugById(Integer id) {
        Optional<Drug> optDrug = drugRepository.findById(id);
        if(optDrug.isPresent()) return optDrug.get();
        throw new NotFoundException("Drug "+id+" does not exists");
    }

    public void modifyDrug(Drug drug) {
        if(drugRepository.existsById(drug.getId())) {
            drugRepository.save(drug);
        } else throw new NotFoundException("Researcher "+ drug.getId() +" does not exists");
    }

    public void deleteDrug(Drug drug) {
        if(drugRepository.existsById(drug.getId())) {
            drugRepository.delete(drug);
        } else throw new NotFoundException("Researcher "+drug.getId()+" does not exists");
    }

    public void insertDrug(Drug drug) {
        drugRepository.save(drug);
    }
}
