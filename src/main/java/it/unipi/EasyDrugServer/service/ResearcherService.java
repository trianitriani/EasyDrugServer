package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.DrugRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import it.unipi.EasyDrugServer.repository.mongo.ResearcherRepository;
import it.unipi.EasyDrugServer.utility.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResearcherService {
    private final UserService userService;
    private final ResearcherRepository researcherRepository;
    private final PatientRepository patientRepository;
    private final PurchaseRepository purchaseRepository;
    private final DrugRepository drugRepository;
    private MongoTemplate mongoTemplate;

    public Researcher getResearcherById(String id) {
        return (Researcher) userService.getUserIfExists(id, UserType.RESEARCHER);
    }

    public Researcher modifyResearcher(Researcher researcher) {
        if(researcherRepository.existsById(researcher.getId())) {
            Researcher researcher_ = getResearcherById(researcher.getId());
            researcher_.setDistrict(researcher.getDistrict());
            researcher_.setCity(researcher.getCity());
            researcher_.setRegion(researcher.getRegion());
            if(!Objects.equals(researcher.getPassword(), ""))
                researcher_.setPassword(PasswordHasher.hash(researcher.getPassword()));
            researcherRepository.save(researcher_);
            return researcher_;
        } else throw new NotFoundException("Researcher "+researcher.getId()+" does not exist");
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
        return switch (order) {
            case ASC -> purchaseRepository.getDistributionByDrug(idDrug, 1);
            case DESC -> purchaseRepository.getDistributionByDrug(idDrug, -1);
            default -> throw new BadRequestException("The order is invalid");
        };
    }

    public List<TopDrugDTO> getTopPurchases(int top, LocalDateTime from, LocalDateTime to){
        if(top < 1)
            throw new BadRequestException("It is impossible to get a top " + top + " purchases");

        return purchaseRepository.getTopDrugs(from, to, top);
    }

    public List<TopRareIndicationDTO> getIndicationsWithLessDrugs(int top){
        if(top < 1)
            throw new BadRequestException("It is impossible to get a top " + top + " purchases");

        return drugRepository.getIndicationsWithLessDrugs(top);
    }
}
