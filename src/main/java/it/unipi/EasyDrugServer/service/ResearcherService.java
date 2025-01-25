package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.DoctorRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.ResearcherRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResearcherService {
    private final ResearcherRepository researcherRepository;

    public Researcher getResearcherById(String id) {
        Optional<Researcher> optResearcher = researcherRepository.findById(id);
        if(optResearcher.isPresent())
            return optResearcher.get();
        throw new NotFoundException("Researcher "+id+" does not exists");
    }

    public void modifyResearcher(Researcher researcher) {
        if(researcherRepository.existsById(researcher.getIdentifyCode())) {
            researcherRepository.save(researcher);
        } else throw new NotFoundException("Researcher "+researcher.getIdentifyCode()+" does not exists");
    }

    public void deleteResearcher(Researcher researcher) {
        if(researcherRepository.existsById(researcher.getIdentifyCode())) {
            researcherRepository.delete(researcher);
        } else throw new NotFoundException("Researcher "+researcher.getIdentifyCode()+" does not exists");
    }
}
