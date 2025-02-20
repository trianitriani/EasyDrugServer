package it.unipi.EasyDrugServer.utility;

import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.PurchaseCartDrugDTO;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.repository.mongo.DrugRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PharmacyRepository;
import it.unipi.EasyDrugServer.service.DoctorService;
import it.unipi.EasyDrugServer.service.PatientService;
import it.unipi.EasyDrugServer.service.PharmacyService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.ArrayList;
import java.util.List;

import static it.unipi.EasyDrugServer.exception.GlobalExceptionHandler.logger;

@Component
@RequiredArgsConstructor
public class RedisBoot {
    private final PatientRepository patientRepository;
    private final DrugRepository drugRepository;
    private final DoctorService doctorService;
    private final PharmacyService pharmacyService;
    private final JedisSentinelPool jedisSentinelPool;

    @PostConstruct
    public void init() {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            // jedis.flushAll();
            List<Patient> patients = patientRepository.findAll();
            List<Drug> drugs1 = drugRepository.findByOnPrescriptionTrue();
            List<Drug> drugs2 = drugRepository.findByOnPrescriptionFalse();
            /*
            for (int i = 0; i < 1500; i++) {
                Patient patient = patients.get(i);
                int id_pres = 0;
                List<String> alreadyInsertedIdDrugs = new ArrayList<>();
                List<Integer> id_pres_drugs = new ArrayList<>();
                for(int j = 0; j < 2; j++){
                    try {
                        Drug drug = drugs1.get(random(drugs1.size() - 1));
                        PrescribedDrugDTO drug_ = new PrescribedDrugDTO();
                        drug_.setIdDrug(drug.getId());
                        drug_.setName(drug.getDrugName());
                        drug_.setPrice(drug.getPrice());
                        drug_.setQuantity(random(2));
                        drug_.setPurchased(false);
                        PrescriptionDTO pres = doctorService.saveDrugIntoPrescriptionCart(patient.getId(), id_pres, drug_, alreadyInsertedIdDrugs);
                        id_pres = pres.getIdPres();
                        alreadyInsertedIdDrugs.add(drug_.getIdDrug());
                        id_pres_drugs.add(pres.getPrescribedDrugs().remove(0).getIdPresDrug());
                    } catch (Exception e){
                        System.out.println("Farmaco già inserito!");
                    }
                }
                PrescriptionDTO pres = doctorService.activatePrescriptionCart(patient.getId(), id_pres, id_pres_drugs);
                System.out.println("Prescrizioni attive per: "+patient.getId());
                System.out.println(pres);
            }

            for (int i = 0; i < 1500; i++) {
                Patient patient = patients.get(i);
                List<String> alreadyInsertedIdDrugs = new ArrayList<>();
                for(int j = 0; j < 3; j++){
                    try {
                        Drug drug = drugs2.get(random(drugs2.size()-1));
                        PurchaseCartDrugDTO purch = new PurchaseCartDrugDTO();
                        purch.setIdDrug(drug.getId());
                        purch.setName(drug.getDrugName());
                        purch.setPrice(drug.getPrice());
                        purch.setQuantity(random(2));
                        pharmacyService.savePurchaseDrug(patient.getId(), purch, alreadyInsertedIdDrugs);
                        alreadyInsertedIdDrugs.add(drug.getId());
                    } catch (Exception e){
                        System.out.println("Farmaco già inserito!");
                    }
                }
                System.out.println("Carrello per: "+patient.getId());
            }*/

            // Provare a mostrare i farmaci inseriti
            for (int i = 0; i < 1500; i++) {
                long startTime = System.currentTimeMillis();
                Patient patient = patients.get(i);
                pharmacyService.viewPharmacyHome(patient.getId());
                long elapsedTime =  System.currentTimeMillis() - startTime;
                System.out.println("View home per farmacia per: "+patient.getId() + " time: " + elapsedTime);
            }

        } catch (Exception e) {
            logger.error("Errore durante l'inizializzazione di RedisBoot", e);
        }
    }

    private int random(int max){
        int min = 1;
        return (int) (Math.random() * (max - min + 1)) + min;
    }

}
