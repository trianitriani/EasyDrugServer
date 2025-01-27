package it.unipi.EasyDrugServer.service;


import it.unipi.EasyDrugServer.dto.DrugDistributionDTO;
import it.unipi.EasyDrugServer.dto.Order;
import it.unipi.EasyDrugServer.dto.PatientDoctorRatioDTO;
import it.unipi.EasyDrugServer.dto.UserType;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.dto.TopDrugDTO;
import it.unipi.EasyDrugServer.dto.TopRareDiseaseDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.ResearcherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResearcherService extends UserService {
    private final ResearcherRepository researcherRepository;
    private final PatientRepository patientRepository;
    private MongoTemplate mongoTemplate;

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

    public List<TopRareDiseaseDTO> getDiseasesWithLessDrugs(int top){
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

        AggregationResults<TopRareDiseaseDTO> diseases = mongoTemplate.aggregate(aggregation, "drugs", TopRareDiseaseDTO.class);
        return diseases.getMappedResults();
    }
}
