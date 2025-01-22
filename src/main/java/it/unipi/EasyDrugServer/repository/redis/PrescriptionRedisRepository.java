package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.exception.UnauthorizedException;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
@Repository
public class PrescriptionRedisRepository {
    private final String pres = "pres";
    private final String presDrug = "pres-drug";
    private final JedisCluster jedisCluster;
    private final RedisHelper redisHelper;
    private final int day = 3600*24;
    private final int month = day*30;

    /*
        pres:id_pres:id_pat:timestamp
        pres:id_pres:id_pat:toPurchase

        pres-drug:id_pres-drug:id_pres:id
        pres-drug:id_pres-drug:id_pres:info { name, price }
        pres-drug:id_pres-drug:id_pres:quantity
        pres-drug:id_pres-drug:id_pres:purchased
     */

    public PrescriptionRedisRepository(JedisCluster jedisCluster, RedisHelper redisHelper) {
        this.jedisCluster = jedisCluster;
        this.redisHelper = redisHelper;
    }

    public List<PrescriptionDTO> getAllActivePrescriptions(String patientCode) {
        if(!isValidPatient(patientCode))
            throw new BadRequestException("Patient "+patientCode+" does not exists!");

        List<PrescriptionDTO> prescriptions = new ArrayList<>();
        for (int i=1; i <= redisHelper.nEntities(jedisCluster, this.pres); i++) {
            String keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            // Controllare che le prescrizioni siano attive, quindi con timestamp != false
            if(jedisCluster.exists(keyPres + "timestamp") &&
                    !Objects.equals(jedisCluster.get(keyPres + "timestamp"), "null")){
                // allora si tratta di una prescrizione attiva
                String timestampString = jedisCluster.get(keyPres + "timestamp");
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                LocalDateTime timestamp = LocalDateTime.parse(timestampString, formatter);

                // cerco all'interno della prescrizione alla ricerca di tutti i farmaci di essa
                PrescriptionDTO prescription = new PrescriptionDTO();
                prescription.setTimestamp(timestamp);
                for(int j=1; j<=redisHelper.nEntities(jedisCluster, this.presDrug); j++) {
                    String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                    if(jedisCluster.exists(keyPresDrug + "id")){
                        // allora il farmaco è relativo a quella prescrizione dell'utente
                        prescription.addPrescribedDrug(createPrescribedDrugDTO(keyPresDrug));
                    }
                }
                prescriptions.add(prescription);
            }
        }
        return prescriptions;
    }

    public PrescriptionDTO getInactivePrescription(String doctorCode, String patientCode) {
        if(!this.isValidDoctor(doctorCode, patientCode))
            throw new UnauthorizedException("The patient "+patientCode+" has another family doctor.");

        PrescriptionDTO prescription = new PrescriptionDTO();
        prescription.setTimestamp(null);
        for (int i=1; i <= redisHelper.nEntities(jedisCluster, this.pres); i++) {
            String keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            // Controllare che le prescrizioni siano attive, quindi con timestamp != false
            if(jedisCluster.exists(keyPres + "timestamp") &&
                    Objects.equals(jedisCluster.get(keyPres + "timestamp"), "null")){
                // allora si tratta di una prescrizione inattiva
                // cerco all'interno della prescrizione alla ricerca di tutti i farmaci di essa
                for(int j=1; j<=redisHelper.nEntities(jedisCluster, this.presDrug); j++) {
                    String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                    if(jedisCluster.exists(keyPresDrug + "id")){
                        // allora il farmaco è relativo a quella prescrizione dell'utente
                        prescription.addPrescribedDrug(createPrescribedDrugDTO(keyPresDrug));
                    }
                }
                break;
            }
        }
        return prescription;
    }

    public PrescribedDrugDTO saveInactivePrescribedDrug(String doctorCode, String patientCode, PrescribedDrugDTO drug) {
        if(!this.isValidDoctor(doctorCode, patientCode))
            throw new UnauthorizedException("The patient "+patientCode+" has another family doctor.");

        boolean found = false;
        int i;
        // controllare se esiste una prescrizione inattiva del paziente
        for(i=1; i<=redisHelper.nEntities(jedisCluster, this.pres); i++){
            String presKey = this.pres + ":" + i + ":" + patientCode + ":";
            if(jedisCluster.exists(presKey + "timestamp")){
                String timestamp = jedisCluster.get(presKey + "timestamp");
                if(!Objects.equals(timestamp, "false")) continue;
                // trovato l'id della prescrizione inattiva devo inserire all'interno
                // della prescrizione un nuovo farmaco

                jedisCluster.expire(presKey + "timestamp", this.day);
                found = true;
                break;
            }
        }

        // se la prescrizione inattiva non esiste, devo crearla
        if(!found){
            int id_pres = Integer.parseInt(redisHelper.getReusableId(jedisCluster, this.pres));
            String presKey = this.pres + ":" + id_pres + ":" + patientCode + ":";
            jedisCluster.set(presKey + "timestamp", "false");

            jedisCluster.expire(presKey + "timestamp", this.day);
            i = id_pres;
        }
        // adesso possiamo inserire il farmaco presente all'interno del db
        String id_presDrug = redisHelper.getReusableId(jedisCluster, this.presDrug);
        String presDrugKey = this.presDrug + ":" + id_presDrug + ":" + i + ":";
        JsonObject info = new JsonObject();
        jedisCluster.set(presDrugKey + "id", String.valueOf(drug.getId()));
        info.addProperty("name", drug.getName());
        info.addProperty("price", drug.getPrice());
        jedisCluster.set(presDrugKey + "info", String.valueOf(info));
        jedisCluster.set(presDrugKey + "quantity", String.valueOf(drug.getQuantity()));
        jedisCluster.set(presDrugKey + "purchased", String.valueOf(drug.isPurchased()));

        // dopo un giorno, se la prescrizione non viene attivata, il farmaco viene eliminato
        jedisCluster.expire(presDrugKey + "id", this.day);
        jedisCluster.expire(presDrugKey + "info", this.day);
        jedisCluster.expire(presDrugKey + "quantity", this.day);
        jedisCluster.expire(presDrugKey + "purchased", this.day);
        return drug;
    }

    public PrescribedDrugDTO deleteInactivePrescribedDrug(String doctorCode, String patientCode, int idDrug) {
        if (!this.isValidDoctor(doctorCode, patientCode))
            throw new UnauthorizedException("The patient " + patientCode + " has another family doctor.");

        PrescribedDrugDTO prescribedDrug;
        for(int i=1; i<=redisHelper.nEntities(jedisCluster, this.pres); i++) {
            // cerco l'unica prescrizione inattiva con timestamp false
            String keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            if (jedisCluster.exists(keyPres + "timestamp") &&
                    !Objects.equals(jedisCluster.get(keyPres + "timestamp"), "false")) {

                // l'id della prescrizione inattiva è i, adesso devo cercare il farmaco da eliminare
                for(int j=1; j<=redisHelper.nEntities(jedisCluster, this.presDrug); j++) {
                    String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                    if(jedisCluster.exists(keyPresDrug + "id")){
                        if(idDrug == Integer.parseInt(jedisCluster.get(keyPresDrug + "id"))){
                            // è il farmaco da rimuovere
                            // preparo l'oggetto per il ritorno
                            prescribedDrug = createPrescribedDrugDTO(idDrug, keyPresDrug);
                            // rimuovo il farmaco dalla prescrizione attiva
                            jedisCluster.del(keyPresDrug + "id");
                            jedisCluster.del(keyPresDrug + "info");
                            jedisCluster.del(keyPresDrug + "quantity");
                            jedisCluster.del(keyPresDrug + "purchased");
                            redisHelper.returnIdToPool(jedisCluster, this.presDrug, String.valueOf(i));
                            return prescribedDrug;
                        }
                    }
                }
                // se qui, allora il farmaco non è stato trovato
                throw new NotFoundException("The selected drug "+idDrug+" is not found.");
            }
        }
        // se qui allora non ci sono prescrizioni inattive
        throw new NotFoundException("Not found any inactive prescription related to patient "+patientCode);
    }

    public PrescribedDrugDTO modifyInactivePrescribedDrugQuantity(String doctorCode, String patientCode, int idDrug, int quantity) {
        if(!this.isValidDoctor(doctorCode, patientCode))
            throw new UnauthorizedException("The patient "+patientCode+" has another family doctor.");

        // cercare l'id della prescrizione inattiva
        int index_pres = 0;
        for(int i=1; i<redisHelper.nEntities(jedisCluster, this.pres); i++){
            String keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            if(jedisCluster.exists(keyPres + "timestamp")){
                String timestamp = jedisCluster.get(keyPres + "timestamp");
                if(!timestamp.equals("false")) continue;
                index_pres = i;
                break;
            }
        }
        if(index_pres == 0)
            throw new NotFoundException("Not found any inactive prescription for patient "+patientCode);

        // adesso dato l'id della prescrizione trovo il farmaco a cui modificare la quantità
        for(int i=1; i<=redisHelper.nEntities(jedisCluster, this.presDrug); i++) {
            // cerco tutti i farmaci con il campo "timestamp" non esistente (quelli non ancora confermati)
            String keyPresDrug = this.presDrug + ":" + i + ":" + index_pres + ":";
            if(jedisCluster.exists(keyPresDrug + "id")){
                if(idDrug == Integer.parseInt(jedisCluster.get(keyPresDrug + "id"))){
                    // ho trovato il farmaco da modificare
                    jedisCluster.set(keyPresDrug + "quantity", String.valueOf(quantity));
                    return createPrescribedDrugDTO(idDrug, quantity, keyPresDrug);
                }
            }
        }
        throw new NotFoundException("Not found any drug with id "+idDrug+" into an inactive prescription");
    }

    public PrescriptionDTO activatePrescription(String doctorCode, String patientCode) {
        // controllo se il medico può prescrivere al paziente selezionato
        if(!this.isValidDoctor(doctorCode, patientCode))
            throw new UnauthorizedException("The patient "+patientCode+" has another family doctor.");

        PrescriptionDTO prescription = new PrescriptionDTO();
        LocalDateTime newTimestamp = LocalDateTime.now();
        String stringNewTimestamp = String.valueOf(newTimestamp);
        prescription.setTimestamp(newTimestamp);
        for(int i=1; i<=redisHelper.nEntities(jedisCluster, this.pres); i++){
            // cerco la prescrizione con dato id
            String keyPresc = this.pres + ":" + i + ":" + patientCode + ":";
            if(jedisCluster.exists(keyPresc + "timestamp")){
                String timestamp = jedisCluster.get(keyPresc + "timestamp");
                if(!timestamp.equals("false")) continue;

                // setto il timestamp a quello di ora e conto i farmaci relativi a quella prescrizione
                jedisCluster.set(keyPresc + "timestamp", stringNewTimestamp);

                int  nDrugs = 0;
                // cicliamo i vari farmaci della prescrizione
                for (int j=1; j<=redisHelper.nEntities(jedisCluster, this.presDrug); j++){
                    String keyDrugs = this.presDrug + ":" + j + ":" + i + ":";
                    if(jedisCluster.exists(keyDrugs + "id")) {
                        nDrugs++;
                        // inserisco il farmaco prescritto nella lista di prescrizione per ritornarlo
                        prescription.addPrescribedDrug(createPrescribedDrugDTO(keyDrugs));

                        // dopo un mese elimino il farmaco prescritto
                        jedisCluster.expire(keyDrugs + "id", this.month);
                        jedisCluster.expire(keyDrugs + "info", this.month);
                        jedisCluster.expire(keyDrugs + "quantity", this.month);
                        jedisCluster.expire(keyDrugs + "purchased", this.month);
                    }
                }
                // setto il numero di farmaci all'interno
                jedisCluster.set(keyPresc + "toPurchase", String.valueOf(nDrugs));

                // un mese dopo la sua creazione la prescrizione viene eliminata
                jedisCluster.expire(keyPresc + "timestamp", this.month);
                jedisCluster.expire(keyPresc + "toPurchase", this.month);

            }
        }
        if(prescription.isEmpty())
            throw new ForbiddenException("The patient "+patientCode+" has no prescriptions.");

        return prescription;
    }

    private boolean isValidPatient(String patientCode) {
        String patKey = "pat: " + patientCode + ":doc";
        return jedisCluster.exists(patKey);
    }

    private boolean isValidDoctor(String doctorCode, String patientCode){
        String patKey = "pat: " + patientCode + ":doc";
        return Objects.equals(doctorCode, jedisCluster.get(patKey));
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(String key){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        prescribedDrug.setId(Integer.parseInt(jedisCluster.get(key + "id")));
        JsonObject jsonObject = JsonParser.parseString(jedisCluster.get(key + "info")).getAsJsonObject();
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(Integer.parseInt(jedisCluster.get(key + "quantity")));
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedisCluster.get(key + "purchased")));
        return prescribedDrug;
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(int id, String key){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedisCluster.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(Integer.parseInt(jedisCluster.get(key + "quantity")));
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedisCluster.get(key + "purchased")));
        return prescribedDrug;
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(int id, int quantity, String key){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedisCluster.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(quantity);
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedisCluster.get(key + "purchased")));
        return prescribedDrug;
    }
}
