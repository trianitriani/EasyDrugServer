package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PrescriptionRedisRepository prescriptionRedisRepository;

    public List<PrescriptionDTO> getAllActivePrescriptions(String patientCode){
        return prescriptionRedisRepository.getAllActivePrescriptions(patientCode);
    }

}
