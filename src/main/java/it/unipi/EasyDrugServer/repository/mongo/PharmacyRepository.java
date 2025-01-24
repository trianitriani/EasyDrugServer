package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.model.Pharmacy;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PharmacyRepository extends MongoRepository<Pharmacy, String> {

    Optional<Object> findByNameAndAddressAndMunicipality(String name, String address, String municipality);

}
