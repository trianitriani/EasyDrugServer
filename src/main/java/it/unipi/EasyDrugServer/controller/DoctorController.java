package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.PrescriptedDrugDTO;
import it.unipi.EasyDrugServer.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;

    @PostMapping("/{doctorCode}/patients/{patientCode}/drugToPrescribe")
    public void saveDrugIntoPrescriptionList(@PathVariable String doctorCode, @PathVariable String patientCode,
                                           @RequestBody PrescriptedDrugDTO drug){
        doctorService.saveDrugIntoPrescriptionList(doctorCode, patientCode, drug);
    }

    @PatchMapping("/{doctorCode}/patients/{patientCode}/prescriptions/inactive")
    public int confirmPrescription(@PathVariable String doctorCode, @PathVariable String patientCode){
        return doctorService.confirmPrescription(doctorCode, patientCode);
    }

}
