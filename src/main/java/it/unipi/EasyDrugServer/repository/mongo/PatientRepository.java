package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.dto.PatientDoctorRatioDTO;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Patient;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {

    List<Patient> findByDoctorCode(String id);

    @Aggregation(pipeline = {
            "{ $group: { _id: '$city', nPatients: { $sum: 1 }, distDoctors: { $addToSet: '$doctorCode' } } }",
            "{ $project: { city: '$_id', nPatients: 1, nDoctors: { $size: '$distDoctors' } } }",
            "{ $project: { city: 1, ratio: { $divide: ['$nPatients', '$nDoctors'] } } }",
            "{ $sort: { ratio: ?0 } }"
    })
    List<PatientDoctorRatioDTO> getPatientsToDoctorsRatio(@Param("order") int order);

    List<Patient> findBySurnameContainingIgnoreCaseAndDoctorCode(String id, String patSurname);
}
