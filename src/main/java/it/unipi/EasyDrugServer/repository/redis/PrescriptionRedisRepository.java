package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionSystemException;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
@Repository
@Retryable(
        retryFor = { JedisConnectionException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
)
public class PrescriptionRedisRepository {
    private final String pres = "pres";
    private final String presDrug = "pres-drug";
    private final JedisSentinelPool jedisSentinelPool;
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

    @Autowired
    public PrescriptionRedisRepository(JedisSentinelPool jedisSentinelPool, RedisHelper redisHelper) {
        this.jedisSentinelPool = jedisSentinelPool;
        this.redisHelper = redisHelper;
    }

    public List<PrescriptionDTO> getAllActivePrescriptions(String id_pat) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            List<PrescriptionDTO> prescriptions = new ArrayList<>();
            for (int i = 1; i <= redisHelper.nEntities(jedis, this.pres); i++) {
                String keyPres = this.pres + ":" + i + ":" + id_pat + ":";
                // Controllare che le prescrizioni siano attive, quindi con timestamp != false
                if (jedis.exists(keyPres + "timestamp") &&
                        !Objects.equals(jedis.get(keyPres + "timestamp"), "")) {
                    // allora si tratta di una prescrizione attiva
                    String timestampString = jedis.get(keyPres + "timestamp");
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    LocalDateTime timestamp = LocalDateTime.parse(timestampString, formatter);

                    // cerco all'interno della prescrizione alla ricerca di tutti i farmaci di essa
                    PrescriptionDTO prescription = new PrescriptionDTO();
                    prescription.setTimestamp(timestamp);
                    for (int j = 1; j <= redisHelper.nEntities(jedis, this.presDrug); j++) {
                        String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                        if (jedis.exists(keyPresDrug + "id")) {
                            // allora il farmaco è relativo a quella prescrizione dell'utente
                            prescription.addPrescribedDrug(createPrescribedDrugDTO(jedis, keyPresDrug));
                        }
                    }
                    prescriptions.add(prescription);
                }
            }
            return prescriptions;
        }
    }

    public PrescriptionDTO getPrescriptionCart(String id_pat) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescriptionDTO prescription = new PrescriptionDTO();
            prescription.setTimestamp(null);
            for (int i = 1; i <= redisHelper.nEntities(jedis, this.pres); i++) {
                String keyPres = this.pres + ":" + i + ":" + id_pat + ":";
                // Controllare che le prescrizioni siano attive, quindi con timestamp != false
                if (jedis.exists(keyPres + "timestamp") &&
                        Objects.equals(jedis.get(keyPres + "timestamp"), "")) {
                    // allora si tratta di una prescrizione inattiva
                    // cerco all'interno della prescrizione alla ricerca di tutti i farmaci di essa
                    for (int j = 1; j <= redisHelper.nEntities(jedis, this.presDrug); j++) {
                        String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                        if (jedis.exists(keyPresDrug + "id")) {
                            // allora il farmaco è relativo a quella prescrizione dell'utente
                            prescription.addPrescribedDrug(createPrescribedDrugDTO(jedis, keyPresDrug));
                        }
                    }
                    break;
                }
            }
            return prescription;
        }
    }

    public PrescribedDrugDTO saveDrugIntoPrescriptionCart(String id_pat, PrescribedDrugDTO drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            boolean found = false;
            // controllare se esiste una prescrizione inattiva del paziente
            String presKey = "";
            int i;
            for (i = 1; i <= redisHelper.nEntities(jedis, this.pres); i++) {
                presKey = this.pres + ":" + i + ":" + id_pat + ":";
                if (jedis.exists(presKey + "timestamp") && Objects.equals(jedis.get(presKey + "timestamp"), "")) {
                    // trovato l'id della prescrizione inattiva devo inserire all'interno
                    // della prescrizione un nuovo farmaco
                    found = true;

                    // in one prescription there can not be twice the same drug
                    for (int j = 1; j <= redisHelper.nEntities(jedis, this.presDrug); j++) {
                        String presDrugKey = this.presDrug + ":" + j + ":" + i + ":";
                        if (jedis.exists(presDrugKey + "id") &&
                                Objects.equals(jedis.get(presDrugKey + "id"), String.valueOf(drug.getId())))
                            throw new ForbiddenException("Drug " + drug.getId() + " is already into the prescription cart");
                    }
                    break;
                }
            }

            // se la prescrizione inattiva non esiste, devo crearla
            if (!found) {
                int id_pres = Integer.parseInt(redisHelper.getReusableId(jedis, this.pres));
                presKey = this.pres + ":" + id_pres + ":" + id_pat + ":";
                i = id_pres;
            }
            // adesso possiamo inserire il farmaco presente all'interno del db
            String id_presDrug = redisHelper.getReusableId(jedis, this.presDrug);
            String presDrugKey = this.presDrug + ":" + id_presDrug + ":" + i + ":";
            JsonObject info = new JsonObject();

            // Modifica dati della prescrizione
            if (!found) jedis.set(presKey + "timestamp", "");
            jedis.expire(presKey + "timestamp", this.day);

            // Modifica dati del farmaco
            jedis.set(presDrugKey + "id", String.valueOf(drug.getId()));
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());
            jedis.set(presDrugKey + "info", String.valueOf(info));
            jedis.set(presDrugKey + "quantity", String.valueOf(drug.getQuantity()));
            jedis.set(presDrugKey + "purchased", String.valueOf(drug.isPurchased()));

            // dopo un giorno, se la prescrizione non viene attivata, il farmaco viene eliminato
            jedis.expire(presDrugKey + "id", this.day);
            jedis.expire(presDrugKey + "info", this.day);
            jedis.expire(presDrugKey + "quantity", this.day);
            jedis.expire(presDrugKey + "purchased", this.day);
            return drug;
        }
    }

    public PrescribedDrugDTO deleteDrugIntoPrescriptionCart(String id_pat, String id_drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescribedDrugDTO prescribedDrug;
            for (int i = 1; i <= redisHelper.nEntities(jedis, this.pres); i++) {
                // cerco l'unica prescrizione inattiva con timestamp false
                String keyPres = this.pres + ":" + i + ":" + id_pat + ":";
                if (jedis.exists(keyPres + "timestamp") && jedis.get(keyPres + "timestamp").isEmpty()) {
                    // l'id della prescrizione inattiva è i, adesso devo cercare il farmaco da eliminare
                    for (int j = 1; j <= redisHelper.nEntities(jedis, this.presDrug); j++) {
                        String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                        if (jedis.exists(keyPresDrug + "id")) {
                            if (Objects.equals(id_drug, jedis.get(keyPresDrug + "id"))) {
                                // è il farmaco da rimuovere
                                // preparo l'oggetto per il ritorno
                                prescribedDrug = createPrescribedDrugDTO(jedis, id_drug, keyPresDrug);
                                // rimuovo il farmaco dalla prescrizione attiva
                                jedis.del(keyPresDrug + "id");
                                jedis.del(keyPresDrug + "info");
                                jedis.del(keyPresDrug + "quantity");
                                jedis.del(keyPresDrug + "purchased");
                                redisHelper.returnIdToPool(jedis, this.presDrug, String.valueOf(i));
                                return prescribedDrug;
                            }
                        }
                    }
                    // se qui, allora il farmaco non è stato trovato
                    throw new NotFoundException("The selected drug " + id_drug + " is not found.");
                }
            }
            // se qui allora non ci sono prescrizioni inattive
            throw new NotFoundException("Not found any inactive prescription related to patient " + id_pat);
        }
    }

    public PrescribedDrugDTO modifyDrugQuantityIntoPrescriptionCart(String id_pat, String id_drug, int quantity) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            // cercare l'id della prescrizione inattiva
            int index_pres = 0;
            for(int i = 1; i<=redisHelper.nEntities(jedis, this.pres); i++) {
                // cerco l'unica prescrizione inattiva con timestamp false
                String keyPres = this.pres + ":" + i + ":" + id_pat + ":";
                if(jedis.exists(keyPres + "timestamp") && jedis.get(keyPres + "timestamp").isEmpty()){
                    index_pres = i;
                    break;
                }
            }
            if(index_pres == 0)
                throw new NotFoundException("Not found any inactive prescription for patient "+id_pat);

            // adesso dato l'id della prescrizione trovo il farmaco a cui modificare la quantità
            for(int i = 1; i<=redisHelper.nEntities(jedis, this.presDrug); i++) {
                // cerco tutti i farmaci con il campo "timestamp" non esistente (quelli non ancora confermati)
                String keyPresDrug = this.presDrug + ":" + i + ":" + index_pres + ":";
                if(jedis.exists(keyPresDrug + "id")){
                    if(Objects.equals(id_drug, jedis.get(keyPresDrug + "id"))){
                        // ho trovato il farmaco da modificare
                        jedis.set(keyPresDrug + "quantity", String.valueOf(quantity));
                        return createPrescribedDrugDTO(jedis, id_drug, quantity, keyPresDrug);
                    }
                }
            }
            throw new NotFoundException("Not found any drug with id "+id_drug+" into an inactive prescription");
        }
    }

    public PrescriptionDTO activatePrescriptionCart(String id_pat) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescriptionDTO prescription = new PrescriptionDTO();
            prescription.setTimestamp(LocalDateTime.now());
            String keyPres = "";
            List<String> keyDrugsList = new ArrayList<>();
            int  nDrugs = 0;
            for(int i = 1; i<=redisHelper.nEntities(jedis, this.pres); i++){
                // cerco la prescrizione con dato id
                keyPres = this.pres + ":" + i + ":" + id_pat + ":";
                if(jedis.exists(keyPres + "timestamp") && jedis.get(keyPres + "timestamp").isEmpty()){
                    // cicliamo i vari farmaci della prescrizione
                    for (int j = 1; j<=redisHelper.nEntities(jedis, this.presDrug); j++){
                        String keyDrugs = this.presDrug + ":" + j + ":" + i + ":";
                        if(jedis.exists(keyDrugs + "id")) {
                            nDrugs++;
                            // inserisco il farmaco prescritto nella lista di prescrizione per ritornarlo
                            prescription.addPrescribedDrug(createPrescribedDrugDTO(jedis, keyDrugs));
                            keyDrugsList.add(keyDrugs);
                        }
                    }
                    break;
                }
            }
            if(prescription.checkIfEmpty())
                throw new ForbiddenException("The patient "+id_pat+" has no prescriptions.");

            // Modify db in atomic way
            for (String keyDrugs : keyDrugsList){
                // modify time to expire for all drugs into the prescription to activate
                jedis.expire(keyDrugs + "id", this.month);
                jedis.expire(keyDrugs + "info", this.month);
                jedis.expire(keyDrugs + "quantity", this.month);
                jedis.expire(keyDrugs + "purchased", this.month);
            }

            // un mese dopo la sua creazione la prescrizione viene eliminata
            jedis.expire(keyPres + "timestamp", this.month);
            jedis.expire(keyPres + "toPurchase", this.month);

            // setto il numero di farmaci all'interno
            jedis.set(keyPres + "toPurchase", String.valueOf(nDrugs));

            // setto il timestamp a quello di ora e conto i farmaci relativi a quella prescrizione
            jedis.set(keyPres + "timestamp", String.valueOf(prescription.getTimestamp()));
            return prescription;
        }
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(Jedis jedis, String key){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        prescribedDrug.setId(jedis.get(key + "id"));
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedis.get(key + "purchased")));
        return prescribedDrug;
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(Jedis jedis, String id, String key){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedis.get(key + "purchased")));
        return prescribedDrug;
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(Jedis jedis, String id, int quantity, String key){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(quantity);
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedis.get(key + "purchased")));
        return prescribedDrug;
    }
}
