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

@Service
@RequiredArgsConstructor
public class DoctorService {
    private final PrescriptionRedisRepository prescriptionRedisRepository;

    public PrescribedDrugDTO saveDrugIntoPrescription(String doctorCode, String patientCode, PrescribedDrugDTO drug) throws UnauthorizedException {
        return prescriptionRedisRepository.saveDrugIntoPrescription(doctorCode, patientCode, drug);
    }

    public PrescriptionDTO confirmPrescription(String doctorCode, String patientCode) throws UnauthorizedException, ForbiddenException {
        return prescriptionRedisRepository.confirmPrescription(doctorCode, patientCode);
    }

    public PrescribedDrugDTO modifyPrescribedDrugQuantity(String doctorCode, String patientCode, int idDrug, int quantity) throws BadRequestException {
        if(quantity == 0)
            return prescriptionRedisRepository.deletePrescribedDrug(doctorCode, patientCode, idDrug);
        else if(quantity < 0)
            throw new BadRequestException("You can not insert a negative quantity.");
        return prescriptionRedisRepository.modifyPrescribedDrugQuantity(doctorCode, patientCode, idDrug, quantity);
    }

    public PrescribedDrugDTO deletePrescribedDrug(String doctorCode, String patientCode, int idDrug){
        return prescriptionRedisRepository.deletePrescribedDrug(doctorCode, patientCode, idDrug);
    }
}
