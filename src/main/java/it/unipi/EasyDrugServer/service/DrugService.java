package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.dto.SimpleDrugDTO;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.repository.mongo.DrugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DrugService {
    private final DrugRepository drugRepository;

    public Drug getDrugById(Integer id) {
        Optional<Drug> optDrug = drugRepository.findByDrugId(id);
        if(optDrug.isPresent()) return optDrug.get();
        throw new NotFoundException("Drug "+id+" does not exists");
    }

    public void modifyDrug(Drug drug) {
        if(drugRepository.existsByDrugId(drug.getDrugId())) {
            drugRepository.save(drug);
        } else throw new NotFoundException("Researcher "+ drug.getDrugId() +" does not exists");
    }

    public Drug deleteDrug(Integer id) {
        Optional<Drug> optDrug = drugRepository.findByDrugId(id);
        if(optDrug.isPresent()) {
            Drug drug = optDrug.get();
            drugRepository.deleteById(id);
            return drug;
        } else throw new NotFoundException("Researcher "+ id+" does not exists");
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
            simpleDrugDTO.setId(drug.getDrugId());
            simpleDrugDTO.setName(drug.getDrugName());
            simpleDrugs.add(simpleDrugDTO);
        }
        return simpleDrugs;
    }



}
