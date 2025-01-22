package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.UnauthorizedException;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import lombok.RequiredArgsConstructor;
// import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import it.unipi.EasyDrugServer.exception.BadRequestException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DoctorService {
    private final PrescriptionRedisRepository prescriptionRedisRepository;

    public PrescriptionDTO getInactivePrescription(String doctorCode, String patientCode) {
        return prescriptionRedisRepository.getInactivePrescription(doctorCode, patientCode);
    }

    public PrescribedDrugDTO saveInactivePrescribedDrug(String doctorCode, String patientCode, PrescribedDrugDTO drug) {
        if(Objects.equals(drug.getName(), ""))
            throw new BadRequestException("Name of the drug can not be null");
        if(drug.getQuantity() < 1)
            throw new BadRequestException("Quantity can not be lower than one");
        return prescriptionRedisRepository.saveInactivePrescribedDrug(doctorCode, patientCode, drug);
    }

    public PrescribedDrugDTO deleteInactivePrescribedDrug(String doctorCode, String patientCode, int idDrug) {
        return prescriptionRedisRepository.deleteInactivePrescribedDrug(doctorCode, patientCode, idDrug);
    }

    public PrescribedDrugDTO modifyInactivePrescribedDrugQuantity(String doctorCode, String patientCode, int idDrug, int quantity) {
        if(quantity == 0)
            return prescriptionRedisRepository.deleteInactivePrescribedDrug(doctorCode, patientCode, idDrug);
        else if(quantity < 0)
            throw new BadRequestException("Quantity can not lower that zero.");
        return prescriptionRedisRepository.modifyInactivePrescribedDrugQuantity(doctorCode, patientCode, idDrug, quantity);
    }

    public PrescriptionDTO activatePrescription(String doctorCode, String patientCode) {
        return prescriptionRedisRepository.activatePrescription(doctorCode, patientCode);
    }
}
