package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.model.Pharmacy;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface PharmacyRepository extends MongoRepository<Pharmacy, String> {

    Optional<Object> findByNameAndAddressAndCity(String name, String address, String city);

}
