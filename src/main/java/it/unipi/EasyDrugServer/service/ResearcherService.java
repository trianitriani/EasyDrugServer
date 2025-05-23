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
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResearcherService {
    private final UserService userService;
    private final ResearcherRepository researcherRepository;
    private final PatientRepository patientRepository;
    private final PurchaseRepository purchaseRepository;
    private final DrugRepository drugRepository;

    public Researcher getResearcherById(String id) {
        return (Researcher) userService.getUserIfExists(id, UserType.RESEARCHER);
    }

    public Researcher modifyResearcher(Researcher researcher) {
        if(researcherRepository.existsById(researcher.getId())) {
            Researcher researcher_ = getResearcherById(researcher.getId());
            researcher_.setDistrict(researcher.getDistrict());
            researcher_.setCity(researcher.getCity());
            researcher_.setRegion(researcher.getRegion());
            if(researcher.getPassword() != null){
                String hash = PasswordHasher.hash(researcher.getPassword());
                researcher_.setPassword(hash);
            }
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

    public List<DrugDistributionDTO> getDistributionByDrug(String idDrug, Order order, LocalDateTime from, LocalDateTime to) {
        ObjectId objDrugId = new ObjectId(idDrug);
        return switch (order) {
            case ASC -> purchaseRepository.getDistributionByDrug(objDrugId, 1, from, to);
            case DESC -> purchaseRepository.getDistributionByDrug(objDrugId, -1, from, to);
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
