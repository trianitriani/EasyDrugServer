package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.dto.DrugDistributionDTO;
import it.unipi.EasyDrugServer.dto.Order;
import it.unipi.EasyDrugServer.dto.PatientDoctorRatioDTO;
import it.unipi.EasyDrugServer.dto.UserType;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.ResearcherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResearcherService extends UserService {
    private final ResearcherRepository researcherRepository;
    private final PatientRepository patientRepository;

    public Researcher getResearcherById(String id) {
        return (Researcher) getUserIfExists(id, UserType.RESEARCHER);
    }

    public void modifyResearcher(Researcher researcher) {
        if(researcherRepository.existsById(researcher.getIdentifyCode())) {
            researcherRepository.save(researcher);
        } else throw new NotFoundException("Researcher "+researcher.getIdentifyCode()+" does not exists");
    }

    public Researcher deleteResearcher(String id) {
        Researcher researcher = getResearcherById(id);
        researcherRepository.deleteById(id);
        return researcher;
    }

    public List<PatientDoctorRatioDTO> getPatientsToDoctorsRatio(Order order) {
        return switch (order) {
            case ASC -> patientRepository.getPatientsToDoctorsRatio(1);
            case DESC -> patientRepository.getPatientsToDoctorsRatio(-1);
            default -> throw new BadRequestException("The order is invalid");
        };
    }

    public List<DrugDistributionDTO> getDistributionByDrug(String idDrug, Order order) {

    }
}
