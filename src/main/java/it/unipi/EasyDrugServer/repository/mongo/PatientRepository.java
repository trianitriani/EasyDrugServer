package it.unipi.EasyDrugServer.repository.mongo;

import it.unipi.EasyDrugServer.dto.PatientDoctorRatioDTO;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.EasyDrugServer.model.Patient;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends MongoRepository<Patient, String> {

    List<Patient> findByIdentifyCodeDoctor(String id);

    @Aggregation(pipeline = {
            "{ $group: { _id: '$comune', nPatients: { $sum: 1 }, distDoctors: { $addToSet: '$doctorCode' } } }",
            "{ $project: { municipality: '$_id', nPatients: 1, nDoctors: { $size: '$distDoctors' } } }",
            "{ $project: { municipality: 1, ratio: { $divide: ['$nPatients', '$nDoctors'] } } }",
            "{ $sort: { ratio: ?0 } }"
    })
    List<PatientDoctorRatioDTO> getPatientsToDoctorsRatio(@Param("order") int order);



}
