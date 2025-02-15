package it.unipi.EasyDrugServer.utility;

import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.repository.mongo.DrugRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.service.DoctorService;
import it.unipi.EasyDrugServer.service.PatientService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.List;

import static it.unipi.EasyDrugServer.exception.GlobalExceptionHandler.logger;

@Component
@RequiredArgsConstructor
public class RedisBoot {
    private final PatientRepository patientRepository;
    private final DrugRepository drugRepository;
    private final DoctorService doctorService;
    private final JedisSentinelPool jedisSentinelPool;

    @PostConstruct
    public void init() {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            jedis.flushAll();
            List<Patient> patients = patientRepository.findAll();
            List<Drug> drugs = drugRepository.findByOnPrescriptionTrue();
            for (int i = 0; i < 150; i++) {
                Patient patient = patients.get(i);
                for(int j = 0; j < 2; j++){
                    Drug drug = drugs.get(random(drugs.size()-1));
                    PrescribedDrugDTO prescribedDrugDTO = new PrescribedDrugDTO();
                    prescribedDrugDTO.setId(drug.getId());
                    prescribedDrugDTO.setName(drug.getDrugName());
                    prescribedDrugDTO.setPrice(drug.getPrice());
                    prescribedDrugDTO.setQuantity(random(2));
                    prescribedDrugDTO.setPurchased(false);
                    doctorService.saveDrugIntoPrescriptionCart(patient.getId(), prescribedDrugDTO);
                }
                doctorService.activatePrescriptionCart(patient.getId());
                System.out.println("Prescrizioni attive per: "+patient.getId());
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
