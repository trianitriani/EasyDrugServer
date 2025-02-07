package it.unipi.EasyDrugServer.service;

import it.unipi.EasyDrugServer.dto.LoginUserDTO;
import it.unipi.EasyDrugServer.dto.SessionUserDTO;
import it.unipi.EasyDrugServer.dto.SignupUserDTO;
import it.unipi.EasyDrugServer.dto.UserType;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.exception.UnauthorizedException;
import it.unipi.EasyDrugServer.model.Doctor;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.model.Pharmacy;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.repository.mongo.DoctorRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PharmacyRepository;
import it.unipi.EasyDrugServer.repository.mongo.ResearcherRepository;
import it.unipi.EasyDrugServer.utility.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static DoctorRepository doctorRepository;
    private static PatientRepository patientRepository;
    private static ResearcherRepository researcherRepository;
    private static PharmacyRepository pharmacyRepository;

    public SessionUserDTO signup(SignupUserDTO user) {
        SessionUserDTO sessionUserDTO = new SessionUserDTO();
        sessionUserDTO.setType(user.getType());
        switch (user.getType()){
            case PATIENT:
                // controllare che non esista già un paziente con lo stesso codice
                if(patientRepository.findById("P"+user.getTaxCode()).isPresent())
                    throw new ForbiddenException("Patient already exists");
                // inserimento paziente sul document
                Patient patient = new Patient();
                patient.setId("P" + user.getTaxCode());
                patient.setPassword(PasswordHasher.hash(user.getPassword()));
                patient.setName(user.getName());
                patient.setSurname(user.getSurname());
                patient.setDateOfBirth(user.getDateOfBirth());
                patient.setCity(user.getMunicipality());
                patient.setDistrict(user.getProvince());
                patient.setRegion(user.getRegion());
                patient.setTaxCode(user.getTaxCode());
                patientRepository.save(patient);
                sessionUserDTO.setName(patient.getName());
                sessionUserDTO.setIdentifyCode(patient.getId());
                break;
            case DOCTOR:
                // controllare che non esista già un paziente con lo stesso codice
                if(doctorRepository.findById("D"+user.getTaxCode()).isPresent())
                    throw new ForbiddenException("Doctor already exists");
                // inserimento paziente sul document
                Doctor doctor = new Doctor();
                doctor.setId("P" + user.getTaxCode());
                doctor.setPassword(PasswordHasher.hash(user.getPassword()));
                doctor.setName(user.getName());
                doctor.setSurname(user.getSurname());
                doctor.setDateOfBirth(user.getDateOfBirth());
                doctor.setCity(user.getMunicipality());
                doctor.setDistrict(user.getProvince());
                doctor.setRegion(user.getRegion());
                doctor.setTaxCode(user.getTaxCode());
                doctor.setDoctorRegisterCode(user.getCertificate());
                doctorRepository.save(doctor);
                sessionUserDTO.setName(doctor.getName());
                sessionUserDTO.setIdentifyCode(doctor.getId());
                break;
            case RESEARCHER:
                if(researcherRepository.findById("R"+user.getTaxCode()).isPresent())
                    throw new ForbiddenException("Researcher already exists");
                // inserimento ricercatore sul document
                Researcher researcher = new Researcher();
                researcher.setId("R" + user.getTaxCode());
                researcher.setPassword(PasswordHasher.hash(user.getPassword()));
                researcher.setName(user.getName());
                researcher.setSurname(user.getSurname());
                researcher.setDateOfBirth(user.getDateOfBirth());
                researcher.setCity(user.getMunicipality());
                researcher.setDistrict(user.getProvince());
                researcher.setRegion(user.getRegion());
                researcher.setTaxCode(user.getTaxCode());
                researcher.setResearcherRegisterCode(user.getCertificate());
                researcherRepository.save(researcher);
                sessionUserDTO.setName(researcher.getName());
                sessionUserDTO.setIdentifyCode(researcher.getId());
                break;
            case PHARMACY:
                if(pharmacyRepository.findByNameAndAddressAndCity(user.getName(), user.getAddress(), user.getMunicipality()).isPresent())
                    throw new ForbiddenException("Pharmacy already exists");
                // Inserimento nel document
                Pharmacy pharmacy = new Pharmacy();
                // l'id deve essere incrementale
                pharmacy.setPassword(PasswordHasher.hash(user.getPassword()));
                pharmacy.setAddress(user.getAddress());
                pharmacy.setName(user.getName());
                pharmacy.setCity(user.getMunicipality());
                pharmacy.setDistrict(user.getProvince());
                pharmacy.setRegion(user.getRegion());
                pharmacy.setOwnerTaxCode(user.getTaxCode());
                pharmacy.setVATnumber(user.getVatNumber());
                pharmacyRepository.save(pharmacy);
                sessionUserDTO.setName(pharmacy.getName());
                sessionUserDTO.setIdentifyCode(pharmacy.getId());
                break;
            default:
                throw new BadRequestException("User type not supported ");
        }
        return sessionUserDTO;
    }

    public SessionUserDTO login(LoginUserDTO user) {
        SessionUserDTO sessionUserDTO = new SessionUserDTO();
        String identifyCode = user.getIdentifyCode();
        String psw = user.getPassword();    // DA HASHARE !!!!!!

        if(Character.isDigit(identifyCode.charAt(0))){
            // significa che sto effettuando il login di una farmacia
            Optional<Pharmacy> optionalPharmacy = pharmacyRepository.findById(identifyCode);
            if(optionalPharmacy.isEmpty())
                throw new NotFoundException("Pharmacy does not exists");

            Pharmacy pharmacy = optionalPharmacy.get();
            if(!PasswordHasher.verifyPassword(psw, pharmacy.getPassword()))
                throw new UnauthorizedException("Wrong password");

            // se arrivo qui il login è corretto
            sessionUserDTO.setIdentifyCode(identifyCode);
            sessionUserDTO.setName(pharmacy.getName());
            sessionUserDTO.setType(UserType.PHARMACY);

        } else{
            switch (identifyCode.charAt(0)) {
                case 'P':
                    // login patient
                    Optional<Patient> optionalPatient = patientRepository.findById(identifyCode);
                    if (optionalPatient.isEmpty())
                        throw new NotFoundException("Patient does not exists");

                    Patient patient = optionalPatient.get();
                    if(!PasswordHasher.verifyPassword(psw, patient.getPassword()))
                        throw new UnauthorizedException("Wrong password");

                    // se arrivo qui il login è corretto
                    sessionUserDTO.setIdentifyCode(identifyCode);
                    sessionUserDTO.setName(patient.getName());
                    sessionUserDTO.setType(UserType.PATIENT);
                    break;
                case 'D':
                    // login doctor
                    Optional<Doctor> optionalDoctor = doctorRepository.findById(identifyCode);
                    if (optionalDoctor.isEmpty())
                        throw new NotFoundException("Doctor does not exists");

                    Doctor doctor = optionalDoctor.get();
                    if(!PasswordHasher.verifyPassword(psw, doctor.getPassword()))
                        throw new UnauthorizedException("Wrong password");

                    // se arrivo qui il login è corretto
                    sessionUserDTO.setIdentifyCode(identifyCode);
                    sessionUserDTO.setName(doctor.getName());
                    sessionUserDTO.setType(UserType.DOCTOR);
                    break;
                case 'R':
                    // login researcher
                    Optional<Researcher> optionalResearcher = researcherRepository.findById(identifyCode);
                    if(optionalResearcher.isEmpty())
                        throw new NotFoundException("Researcher does not exists");

                    Researcher researcher = optionalResearcher.get();
                    if(!PasswordHasher.verifyPassword(psw, researcher.getPassword()))
                        throw new UnauthorizedException("Wrong password");

                    sessionUserDTO.setIdentifyCode(identifyCode);
                    sessionUserDTO.setName(researcher.getName());
                    sessionUserDTO.setType(UserType.RESEARCHER);
                    break;
                default:
                    // identifyCode sbagliato
                    throw new BadRequestException("User type not supported ");
            }
        }
        return sessionUserDTO;
    }
}
