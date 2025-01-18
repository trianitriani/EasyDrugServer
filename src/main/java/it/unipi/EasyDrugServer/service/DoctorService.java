package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescriptedDrugDTO;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorService {
    private final PrescriptionRedisRepository prescriptionRedisRepository;

    public void saveDrugIntoPrescriptionList(String doctorCode, String patientCode, PrescriptedDrugDTO drug){
        prescriptionRedisRepository.saveDrugIntoPrescriptionList(doctorCode, patientCode, drug);
    }

    public int confirmPrescription(String doctorCode, String patientCode){
        return prescriptionRedisRepository.confirmPrescription(doctorCode, patientCode);
    }
}
