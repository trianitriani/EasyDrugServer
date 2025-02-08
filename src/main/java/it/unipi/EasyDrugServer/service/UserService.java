package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.UserType;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Doctor;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Pharmacy;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.DoctorRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PharmacyRepository;
import it.unipi.EasyDrugServer.repository.mongo.ResearcherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ResearcherRepository researcherRepository;
    private final PharmacyRepository pharmacyRepository;

    protected Object getUserIfExists(String id, UserType type) {
        switch (type) {
            case PATIENT:
                System.out.println(id + " " + id.getClass());
                System.out.println("D1: "+patientRepository.findById(id));
                Optional<Patient> optPatient = patientRepository.findById(id);
                System.out.println(optPatient + " WAAAAAA");
                if(optPatient.isPresent()) return optPatient.get();
                throw new NotFoundException("Patient "+id+" does not exist");
            case DOCTOR:
                Optional<Doctor> optDoctor = doctorRepository.findById(id);
                if(optDoctor.isPresent()) return optDoctor.get();
                throw new NotFoundException("Doctor "+id+" does not exist");
            case RESEARCHER:
                Optional<Researcher> optResearcher = researcherRepository.findById(id);
                if(optResearcher.isPresent()) return optResearcher.get();
                throw new NotFoundException("Researcher "+id+" does not exist");
            case PHARMACY:
                Optional<Pharmacy> optPharmacy = pharmacyRepository.findById(id);
                if(optPharmacy.isPresent()) return optPharmacy.get();
                throw new NotFoundException("Pharmacy "+id+" does not exist");
            default:
                return null;
        }
    }
}
