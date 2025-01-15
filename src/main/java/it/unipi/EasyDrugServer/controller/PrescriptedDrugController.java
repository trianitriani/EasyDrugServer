package it.unipi.EasyDrugServer.controller;

import it.unipi.EasyDrugServer.model.PrescriptedDrug;
import it.unipi.EasyDrugServer.model.Prescription;
import it.unipi.EasyDrugServer.service.PrescriptedDrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptedDrugs")
@RequiredArgsConstructor
public class PrescriptedDrugController {
    private final PrescriptedDrugService prescriptedDrugService;

    /**
     * This API returns a list of active prescriptions of a specific patient, all type of user except
     * drug researcher can use it.
     * @param idPatient Identificatore del paziente
     * @return List<Prescription> Lista delle prescrizioni restituite
     */
    @GetMapping("/patient/{idPatient}")
    public ResponseEntity<List<Prescription>> getPrescriptionsByPatient(@PathVariable int idPatient) {
        return null;
    }

}
