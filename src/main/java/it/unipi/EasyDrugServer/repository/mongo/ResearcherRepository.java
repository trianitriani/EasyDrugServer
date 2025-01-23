package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.model.Researcher;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResearcherRepository extends MongoRepository<Researcher, String> {

}
