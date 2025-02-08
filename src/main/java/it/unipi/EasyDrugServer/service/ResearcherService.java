package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Researcher;
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

    public List<DrugDistributionDTO> getDistributionByDrug(ObjectId idDrug, Order order) {
        return switch (order) {
            case ASC -> purchaseRepository.getDistributionByDrug(idDrug, 1);
            case DESC -> purchaseRepository.getDistributionByDrug(idDrug, -1);
            default -> throw new BadRequestException("The order is invalid");
        };
    }

    public List<TopDrugDTO> getTopPurchases(int top, LocalDate from, LocalDate to){
        if(top <= 1)
            throw new BadRequestException("It is impossible to get a top " + top + " purchases");

        Aggregation aggregation = Aggregation.newAggregation(
                // Filtra gli acquisti con prescriptionDate tra 'from' e 'to'
                Aggregation.match(Criteria.where("prescriptionDate").gte(from).lte(to)),

                // Raggruppa per drugId e conta le occorrenze
                Aggregation.group("drugId", "name").sum("quantity").as("totalQuantity"),

                // Ordina in ordine decrescente in base al totale
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "totalQuantity"),

                // Limita ai primi "top" risultati
                Aggregation.limit(top)
        );

        AggregationResults<TopDrugDTO> topPurchases = mongoTemplate.aggregate(aggregation, "purchases", TopDrugDTO.class);
        return topPurchases.getMappedResults();
    }

    public List<TopRareIndicationDTO> getIndicationsWithLessDrugs(int top){
        if(top <= 1)
            throw new BadRequestException("It is impossible to get a top " + top + " purchases");
        Aggregation aggregation = Aggregation.newAggregation(
                // Esplodi l'array "indications" per lavorare sulle singole malattie
                Aggregation.unwind("indications"),

                // Raggruppa per "indicationId" e conta i farmaci
                Aggregation.group("indications.indicationId", "indications.indicationName")
                        .addToSet("drugName").as("drugNames") // Colleziona i nomi dei farmaci
                        .count().as("drugCount"), // Conta i farmaci

                // Ordina in ordine crescente per numero di farmaci
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "drugCount"),

                // Limita ai primi "top" risultati
                Aggregation.limit(top)
        );

        AggregationResults<TopRareIndicationDTO> diseases = mongoTemplate.aggregate(aggregation, "drugs", TopRareIndicationDTO.class);
        return diseases.getMappedResults();
    }
}
