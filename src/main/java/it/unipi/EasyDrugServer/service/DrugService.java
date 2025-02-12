package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.dto.SimpleDrugDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.repository.mongo.DrugRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    public Drug getDrugById(String id) {
        Optional<Drug> optDrug = drugRepository.findById(id);
        if(optDrug.isPresent()) return optDrug.get();
        throw new NotFoundException("Drug "+id+" does not exist");
    }

    public void modifyDrug(Drug drug) {
        if(drug.getIndications().isEmpty())
            throw new BadRequestException("No added indication");
        if(drugRepository.existsById(drug.getId())) {
            drugRepository.save(drug);
        } else throw new NotFoundException("Drug "+ drug.getId() +" does not exist");
    }

    public Drug deleteDrug(String id) {
        Optional<Drug> optDrug = drugRepository.findById(id);
        if(optDrug.isPresent()) {
            Drug drug = optDrug.get();
            drugRepository.deleteById(id);
            return drug;
        } else throw new NotFoundException("Drug "+ id+" does not exist");
    }

    public void insertDrug(Drug drug) {
        if(drug.getIndications().isEmpty())
            throw new BadRequestException("No added indication");
        drugRepository.save(drug);
    }

    public List<SimpleDrugDTO> getDrugThatStartWith(String name) {
        List<Drug> drugs = drugRepository.findByDrugNameStarting(name.toLowerCase());
        return getSimpleDrugsByDrugs(drugs);
    }

    public List<SimpleDrugDTO> getOPTDrugsThatStartWith(String name) {
        List<Drug> drugs = drugRepository.findByDrugNameStartingAndOnPrescription(name.toLowerCase(), false);
        return getSimpleDrugsByDrugs(drugs);
    }

    public List<SimpleDrugDTO> getOnPrescriptionDrugsThatStartWith(String name) {
        List<Drug> drugs = drugRepository.findByDrugNameStartingAndOnPrescription(name.toLowerCase(), true);
        return getSimpleDrugsByDrugs(drugs);
    }

    private List<SimpleDrugDTO> getSimpleDrugsByDrugs(List<Drug> drugs) {
        List<SimpleDrugDTO> simpleDrugs = new ArrayList<>();
        for(Drug drug: drugs){
            SimpleDrugDTO simpleDrugDTO = new SimpleDrugDTO();
            simpleDrugDTO.setId(drug.getId());
            simpleDrugDTO.setName(drug.getDrugName());
            simpleDrugDTO.setCompany(drug.getCompany());
            simpleDrugs.add(simpleDrugDTO);
        }
        return simpleDrugs;
    }



}
