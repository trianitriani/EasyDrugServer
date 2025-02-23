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
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ResearcherRepository researcherRepository;
    private final PharmacyRepository pharmacyRepository;

    public SessionUserDTO signup(SignupUserDTO user) {
        SessionUserDTO sessionUserDTO = new SessionUserDTO();
        sessionUserDTO.setType(user.getType());
        switch (user.getType()){
            case PATIENT:
                if(!correctInformation(UserType.PATIENT, user))
                    throw new BadRequestException("Some information are incorrect");

                // controllare che non esista già un paziente con lo stesso codice
                if(patientRepository.findById("P"+user.getTaxCode()).isPresent())
                    throw new ForbiddenException("Patient already exists");

                if(doctorRepository.findById(user.getDoctorCode()).isEmpty())
                    throw new ForbiddenException("Your doctor not exists");

                // inserimento paziente sul document
                Patient patient = new Patient();
                patient.setId("P" + user.getTaxCode());
                patient.setPassword(PasswordHasher.hash(user.getPassword()));
                patient.setName(user.getName());
                patient.setSurname(user.getSurname());
                patient.setDateOfBirth(user.getDateOfBirth());
                patient.setCity(user.getCity());
                patient.setDistrict(user.getDistrict());
                patient.setRegion(user.getRegion());
                patient.setTaxCode(user.getTaxCode());
                patient.setDoctorCode(user.getDoctorCode());
                patient.setGender(user.getGender());
                patientRepository.save(patient);
                sessionUserDTO.setName(patient.getName());
                sessionUserDTO.setId(patient.getId());
                break;
            case DOCTOR:
                if(!correctInformation(UserType.DOCTOR, user))
                    throw new BadRequestException("Some information are incorrect");

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
                doctor.setCity(user.getCity());
                doctor.setDistrict(user.getDistrict());
                doctor.setRegion(user.getRegion());
                doctor.setTaxCode(user.getTaxCode());
                doctor.setDoctorRegisterCode(user.getDoctorRegisterCode());
                doctor.setGender(user.getGender());
                doctorRepository.save(doctor);
                sessionUserDTO.setName(doctor.getName());
                sessionUserDTO.setId(doctor.getId());
                break;
            case RESEARCHER:
                if(!correctInformation(UserType.RESEARCHER, user))
                    throw new BadRequestException("Some information are incorrect");

                if(researcherRepository.findById("R"+user.getTaxCode()).isPresent())
                    throw new ForbiddenException("Researcher already exists");
                // inserimento ricercatore sul document
                Researcher researcher = new Researcher();
                researcher.setId("R" + user.getTaxCode());
                researcher.setPassword(PasswordHasher.hash(user.getPassword()));
                researcher.setName(user.getName());
                researcher.setSurname(user.getSurname());
                researcher.setDateOfBirth(user.getDateOfBirth());
                researcher.setCity(user.getCity());
                researcher.setDistrict(user.getDistrict());
                researcher.setRegion(user.getRegion());
                researcher.setTaxCode(user.getTaxCode());
                researcher.setResearcherRegisterCode(user.getResearcherRegisterCode());
                researcher.setGender(user.getGender());
                researcherRepository.save(researcher);
                sessionUserDTO.setName(researcher.getName());
                sessionUserDTO.setId(researcher.getId());
                break;
            case PHARMACY:
                if(!correctInformation(UserType.PHARMACY, user))
                    throw new BadRequestException("Some information are incorrect");

                if(pharmacyRepository.findById("Ph"+user.getVatNumber()).isPresent())
                    throw new ForbiddenException("Pharmacy already exists");
                // Inserimento nel document
                Pharmacy pharmacy = new Pharmacy();
                pharmacy.setPassword(PasswordHasher.hash(user.getPassword()));
                pharmacy.setAddress(user.getAddress());
                pharmacy.setName(user.getName());
                pharmacy.setCity(user.getCity());
                pharmacy.setDistrict(user.getDistrict());
                pharmacy.setRegion(user.getRegion());
                pharmacy.setOwnerTaxCode(user.getOwnerTaxCode());
                pharmacy.setVATnumber(user.getVatNumber());
                pharmacy.setId("Ph"+user.getVatNumber());
                pharmacyRepository.save(pharmacy);
                sessionUserDTO.setName(pharmacy.getName());
                sessionUserDTO.setId(pharmacy.getId());
                break;
            default:
                throw new BadRequestException("One or more fields have unknown type of data");
        }
        return sessionUserDTO;
    }

    public SessionUserDTO login(LoginUserDTO user) {
        SessionUserDTO sessionUserDTO = new SessionUserDTO();

        if (user.getIdentifyCode() == null || user.getIdentifyCode().isEmpty()) {
            throw new BadRequestException("Identify code cannot be empty");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new BadRequestException("Password code cannot be empty");
        }

        String identifyCode = user.getIdentifyCode();
        String psw = user.getPassword();

        if(identifyCode.startsWith("Ph")){
            // significa che sto effettuando il login di una farmacia
            Optional<Pharmacy> optionalPharmacy = pharmacyRepository.findById(identifyCode);
            if(optionalPharmacy.isEmpty())
                throw new NotFoundException("Pharmacy does not exist");

            Pharmacy pharmacy = optionalPharmacy.get();
            if(!PasswordHasher.verifyPassword(psw, pharmacy.getPassword()))
                throw new UnauthorizedException("Wrong password");

            // se arrivo qui il login è corretto
            sessionUserDTO.setId(identifyCode);
            sessionUserDTO.setName(pharmacy.getName());
            sessionUserDTO.setType(UserType.PHARMACY);

        } else{
            switch (identifyCode.charAt(0)) {
                case 'P':
                    // login patient
                    Optional<Patient> optionalPatient = patientRepository.findById(identifyCode);
                    if (optionalPatient.isEmpty())
                        throw new NotFoundException("Patient does not exist");

                    Patient patient = optionalPatient.get();
                    System.out.println(psw);
                    System.out.println(patient.getPassword());
                    if(!PasswordHasher.verifyPassword(psw, patient.getPassword()))
                        throw new UnauthorizedException("Wrong password");

                    // se arrivo qui il login è corretto
                    sessionUserDTO.setId(identifyCode);
                    sessionUserDTO.setName(patient.getName());
                    sessionUserDTO.setType(UserType.PATIENT);
                    break;
                case 'D':
                    // login doctor
                    Optional<Doctor> optionalDoctor = doctorRepository.findById(identifyCode);
                    if (optionalDoctor.isEmpty())
                        throw new NotFoundException("Doctor does not exist");

                    Doctor doctor = optionalDoctor.get();
                    if(!PasswordHasher.verifyPassword(psw, doctor.getPassword()))
                        throw new UnauthorizedException("Wrong password");

                    // se arrivo qui il login è corretto
                    sessionUserDTO.setId(identifyCode);
                    sessionUserDTO.setName(doctor.getName());
                    sessionUserDTO.setType(UserType.DOCTOR);
                    break;
                case 'R':
                    // login researcher
                    Optional<Researcher> optionalResearcher = researcherRepository.findById(identifyCode);
                    if(optionalResearcher.isEmpty())
                        throw new NotFoundException("Researcher does not exist");

                    Researcher researcher = optionalResearcher.get();
                    if(!PasswordHasher.verifyPassword(psw, researcher.getPassword()))
                        throw new UnauthorizedException("Wrong password");

                    sessionUserDTO.setId(identifyCode);
                    sessionUserDTO.setName(researcher.getName());
                    sessionUserDTO.setType(UserType.RESEARCHER);
                    break;
                default:
                    // identifyCode sbagliato
                    throw new BadRequestException("One or more fields have unknown type of data");
            }
        }
        return sessionUserDTO;
    }

    private boolean correctInformation(UserType userType, SignupUserDTO user) {
        if(user.getName() == null) return false;
        if(user.getCity() == null) return false;
        if(user.getDistrict() == null) return false;
        if(user.getRegion() == null) return false;
        if(user.getPassword() == null) return false;
        switch (userType) {
            case PHARMACY:
                if(user.getAddress() == null) return false;
                if(user.getOwnerTaxCode() == null) return false;
                if(user.getVatNumber() == null) return false;
                break;
            case DOCTOR:
                if(user.getSurname() == null) return false;
                if(user.getGender() == null) return false;
                if(user.getDoctorRegisterCode() == null) return false;
                if(user.getTaxCode() == null) return false;
                if(!isValidDateOfBirth(user.getDateOfBirth())) return false;
                break;
            case RESEARCHER:
                if(user.getSurname() == null) return false;
                if(user.getGender() == null) return false;
                if(user.getTaxCode() == null) return false;
                if(user.getResearcherRegisterCode() == null) return false;
                if(!isValidDateOfBirth(user.getDateOfBirth())) return false;
                break;
            case PATIENT:
                if(user.getSurname() == null) return false;
                if(user.getDoctorCode() == null) return false;
                if(user.getTaxCode() == null) return false;
                if(user.getGender() == null) return false;
                if(!isValidDateOfBirth(user.getDateOfBirth())) return false;
                break;
        }
        return true;
    }

    private boolean isValidDateOfBirth(String dateOfBirth) {
        // Usa SimpleDateFormat per verificare se la data è valida
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false); // Imposta il parsing rigoroso (non accetta date come 2023-02-30)

        try {
            sdf.parse(dateOfBirth); // Prova a parsare la data
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

}
