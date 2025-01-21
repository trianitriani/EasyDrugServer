package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DoctorHomeDTO {
    private PrescriptionDTO inactivePrescription;
    private List<PrescriptionDTO> activePrescriptions;

    public DoctorHomeDTO() {

    }
}
