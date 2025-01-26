package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.model.Purchase;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseRepository extends MongoRepository<Purchase, Integer> {
    // Query per trovare i Purchase di un paziente tra due date
    List<Purchase> findByPatientCodeAndPurchaseDateBetween(String patientCode, LocalDateTime startDate, LocalDateTime endDate);
}
