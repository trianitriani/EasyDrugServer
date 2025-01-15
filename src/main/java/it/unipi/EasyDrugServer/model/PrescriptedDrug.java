package it.unipi.EasyDrugServer.model;
import java.io.Serializable;

public class PrescriptedDrug implements Serializable {
    private Integer id;         // Parte della chiave (ID della prescrizione)
    private String idPaziente; // Parte della chiave (ID del paziente)
    private String info;       // Attributo
    private Integer quantity;      // Attributo
    private Boolean purchased;
    private Boolean active;

    // Costruttore vuoto (necessario per la deserializzazione)
    public PrescriptedDrug() {}



    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdPaziente() {
        return idPaziente;
    }

    public void setIdPaziente(String idPaziente) {
        this.idPaziente = idPaziente;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getPurchased() {
        return purchased;
    }

    public void setPurchased(Boolean purchased) {
        this.purchased = purchased;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
