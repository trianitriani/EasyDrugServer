package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.dto.SimpleDrugDTO;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.repository.mongo.DrugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class DrugService {
    private final DrugRepository drugRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    public Drug getDrugById(int id) {
        Optional<Drug> optDrug = drugRepository.findById(id);
        if(optDrug.isPresent()) return optDrug.get();
        throw new NotFoundException("Drug "+id+" does not exists");
    }

    public void modifyDrug(Drug drug) {
        if(drugRepository.existsById(drug.getId())) {
            drugRepository.save(drug);
        } else throw new NotFoundException("Drug "+ drug.getId() +" does not exists");
    }

    public Drug deleteDrug(int id) {
        Optional<Drug> optDrug = drugRepository.findById(id);
        if(optDrug.isPresent()) {
            System.out.println(optDrug);
            Drug drug = optDrug.get();
            drugRepository.deleteById(id);
            return drug;
        } else throw new NotFoundException("Drug "+ id+" does not exists");
    }

    public void insertDrug(Drug drug) {
        drugRepository.save(drug);
    }
    
    public List<SimpleDrugDTO> getDrugsThatContain(String name) {
        List<Drug> drugs = drugRepository.findByDrugNameContainingIgnoreCase(name);
        return getSimpleDrugsByDrugs(drugs);
    }

    public List<SimpleDrugDTO> getDrugsPurchasableThatContain(String name) {
        List<Drug> drugs = drugRepository.findByDrugNameContainingIgnoreCaseAndOnPrescriptionFalse(name);
        return getSimpleDrugsByDrugs(drugs);
    }

    public List<SimpleDrugDTO> getDrugsOnPrescriptionThatContain(String name) {
        List<Drug> drugs = drugRepository.findByDrugNameContainingIgnoreCaseAndOnPrescriptionTrue(name);
        return getSimpleDrugsByDrugs(drugs);
    }

    public List<SimpleDrugDTO> getDrugsByIndication(String name) {
        List<Drug> drugs = drugRepository.findByIndicationsIndicationName(name);
        return getSimpleDrugsByDrugs(drugs);
    }

    private List<SimpleDrugDTO> getSimpleDrugsByDrugs(List<Drug> drugs) {
        List<SimpleDrugDTO> simpleDrugs = new ArrayList<>();
        for(Drug drug: drugs){
            SimpleDrugDTO simpleDrugDTO = new SimpleDrugDTO();
            simpleDrugDTO.setId(drug.getId());
            simpleDrugDTO.setName(drug.getDrugName());
            simpleDrugs.add(simpleDrugDTO);
        }
        return simpleDrugs;
    }



}
