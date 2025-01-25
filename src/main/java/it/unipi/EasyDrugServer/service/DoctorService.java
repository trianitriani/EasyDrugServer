package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Doctor;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.repository.mongo.DoctorRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import lombok.RequiredArgsConstructor;
// import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import it.unipi.EasyDrugServer.exception.BadRequestException;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorService {
    private final PrescriptionRedisRepository prescriptionRedisRepository;
    private final DoctorRepository doctorRepository;

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

    public Doctor getDoctorById(String id) {
        Optional<Doctor> optDoctor = doctorRepository.findById(id);
        if(optDoctor.isPresent())
            return optDoctor.get();
        throw new NotFoundException("Doctor "+id+" does not exists");
    }

    public void modifyDoctor(Doctor doctor) {
        if(doctorRepository.existsById(doctor.getIdentifyCode())) {
            doctorRepository.save(doctor);
        } else throw new NotFoundException("Researcher "+doctor.getIdentifyCode()+" does not exists");
    }

    public void deleteDoctor(Doctor doctor) {
        if(doctorRepository.existsById(doctor.getIdentifyCode())) {
            doctorRepository.delete(doctor);
        } else throw new NotFoundException("Researcher "+doctor.getIdentifyCode()+" does not exists");
    }

}
