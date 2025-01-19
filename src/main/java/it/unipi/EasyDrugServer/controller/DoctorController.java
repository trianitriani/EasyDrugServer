package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;

    @PostMapping("/{doctorCode}/patients/{patientCode}/drugToPrescribe")
    public PrescribedDrugDTO saveDrugIntoPrescription(@PathVariable String doctorCode, @PathVariable String patientCode,
                                                      @RequestBody PrescribedDrugDTO drug){
        return doctorService.saveDrugIntoPrescription(doctorCode, patientCode, drug);
    }

    @PostMapping("/{doctorCode}/patients/{patientCode}/prescriptions/inactive")
    public PrescriptionDTO confirmPrescription(@PathVariable String doctorCode, @PathVariable String patientCode){
        return doctorService.confirmPrescription(doctorCode, patientCode);
    }

    @PatchMapping("/{doctorCode}/patients/{patientCode}/drugToPrescribe/{idDrug}")
    public PrescribedDrugDTO modifyPrescribedDrugQuantity(@PathVariable String doctorCode, @PathVariable String patientCode,
                                                          @PathVariable int idDrug, @RequestBody int quantity){
        return doctorService.modifyPrescribedDrugQuantity(doctorCode, patientCode, idDrug, quantity);
    }

    @DeleteMapping("/{doctorCode}/patients/{patientCode}/drugToPrescribe/{idDrug}")
    public PrescribedDrugDTO deletePrescribedDrug(@PathVariable String doctorCode, @PathVariable String patientCode,
                                                  @PathVariable int idDrug){
        return doctorService.deletePrescribedDrug(doctorCode, patientCode, idDrug);
    }

}
