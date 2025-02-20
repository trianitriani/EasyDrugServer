package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.dto.PurchaseCartDrugDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
    private final int day = 3600*24*3;  // MODIFICARE
    private final int month = day*30;

    /*
        pres:id_pres:id_pat:timestamp
        pres:id_pres:id_pat:toPurchase
        pres:id_pat:set

        pres-drug:id_pres-drug:id_pres:id
        pres-drug:id_pres-drug:id_pres:info { name, price, quantity }
        pres-drug:id_pres-drug:id_pres:purchased
        pres-drug:id_pres:set
     */

    @Autowired
    public PrescriptionRedisRepository(JedisSentinelPool jedisSentinelPool, RedisHelper redisHelper) {
        this.jedisSentinelPool = jedisSentinelPool;
        this.redisHelper = redisHelper;
    }

    public List<PrescriptionDTO> getAllActivePrescriptions(String id_pat) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            List<PrescriptionDTO> prescriptions = new ArrayList<>();
            String listKey = this.pres + ":" + id_pat + ":set";
            List<String> presIds = new ArrayList<>(jedis.smembers(listKey));

            if(presIds.isEmpty())
                return prescriptions;

            for(String id_pres: presIds){
                String keyPres = this.pres + ":" + id_pat + ":";
                String listKeyPresDrug = this.presDrug + ":" + id_pres + ":set";
                // controllo che la prescrizione sia o meno attiva
                String timestamp = jedis.get(keyPres + "timestamp");
                if(timestamp.isEmpty())
                    continue;

                // inserimento nel dto delle prescrizioni attive
                PrescriptionDTO prescriptionDTO = new PrescriptionDTO();
                prescriptionDTO.setTimestamp(LocalDateTime.parse(timestamp));
                prescriptionDTO.setIdPres(Integer.parseInt(id_pres));

                // ottenimento delle chiavi da leggere per tutti i farmaci della prescrizione
                List<String> presDrugIds = new ArrayList<>(jedis.smembers(listKeyPresDrug));
                List<String> keys = new ArrayList<>();
                for(String id_pres_drug: presDrugIds){
                    String keyPresDrug = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
                    keys.add(keyPresDrug + "id");
                    keys.add(keyPresDrug + "info");
                    keys.add(keyPresDrug + "purchased");
                }

                // effettuiamo una sola chiamata a MGET per tutti e i SOLI farmaci della prescrizione
                List<String> values = jedis.mget(keys.toArray(new String[0]));
                for (int i = 0; i < presDrugIds.size(); i++) {
                    int id_pres_drug = Integer.parseInt(presDrugIds.get(i));
                    String idDrug = values.get(i * 3);
                    String infoJson = values.get(i * 3 + 1);
                    String purchased = values.get(i * 3 + 2);
                    PrescribedDrugDTO drug = createPrescribedDrugDTO(idDrug, infoJson, purchased, id_pres_drug);
                    prescriptionDTO.getPrescribedDrugs().add(drug);
                }
                prescriptions.add(prescriptionDTO);
            }
            return prescriptions;
        }
    }

    /*
    public List<PrescriptionDTO> getAllActivePrescriptions(String id_pat) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            List<PrescriptionDTO> prescriptions = new ArrayList<>();
            String matchPattern = this.pres + ":*:" + id_pat + ":timestamp";
            String cursor = "0";

            do {
                ScanResult<String> scanResult = jedis.scan(cursor, new ScanParams().match(matchPattern).count(10000));
                cursor = scanResult.getCursor();

                for (String keyPresTimestamp : scanResult.getResult()) {
                    // Rimuove ":timestamp"
                    String keyPres = keyPresTimestamp.substring(0, keyPresTimestamp.lastIndexOf(":"));
                    String timestampString = jedis.get(keyPresTimestamp);
                    if (timestampString == null || timestampString.isEmpty()) continue;

                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    LocalDateTime timestamp = LocalDateTime.parse(timestampString, formatter);

                    PrescriptionDTO prescription = new PrescriptionDTO();
                    prescription.setTimestamp(timestamp);

                    // Trovare i farmaci associati alla prescrizione
                    String matchPresDrugPattern = this.presDrug + ":*:" + keyPres.split(":")[1] + ":";
                    String drugCursor = "0";

                    do {
                        ScanResult<String> scanPresDrug = jedis.scan(drugCursor, new ScanParams().match(matchPresDrugPattern + "id").count(10000));
                        drugCursor = scanPresDrug.getCursor();

                        for (String keyPresDrugId : scanPresDrug.getResult()) {
                            String keyPresDrug = keyPresDrugId.substring(0, keyPresDrugId.lastIndexOf(":")); // Rimuove ":id"
                            int drugIndex = extractIndexFromKey(keyPresDrug);
                            prescription.addPrescribedDrug(createPrescribedDrugDTO(jedis, keyPresDrug + ":", drugIndex));
                        }

                    } while (!drugCursor.equals("0"));

                    prescriptions.add(prescription);
                }
            } while (!cursor.equals("0"));

            return prescriptions;
        }
    }

    private int extractIndexFromKey(String key) {
        String[] parts = key.split(":");
        return Integer.parseInt(parts[1]); // Supponendo che l'indice sia nella seconda posizione
    }*/

    public PrescriptionDTO getPrescriptionCart(String id_pat) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Gson gson = new Gson();
            PrescriptionDTO prescription = new PrescriptionDTO();
            prescription.setTimestamp(null);
            String listKey = this.pres + ":" + id_pat + ":set";
            List<String> presIds = new ArrayList<>(jedis.smembers(listKey));

            if(presIds.isEmpty())
                return prescription;

            // ottenimento delle SOLE N prescrizioni dell'utente
            for (String id_pres : presIds) {
                String keyPres = this.pres + ":" + id_pat + ":";
                String listKeyPresDrug = this.presDrug + ":" + id_pres + ":set";
                // controllo che la prescrizione sia o meno attiva
                String timestamp = jedis.get(keyPres + "timestamp");
                if (timestamp.isEmpty()) {
                    // in questo caso la prescrizione è inattiva
                    // otteniamo i SOLI id dei farmaci del carrello della prescrizione
                    List<String> presDrugIds =  new ArrayList<>(jedis.smembers(listKeyPresDrug));
                    List<String> keys = new ArrayList<>();
                    for (String id_pres_drug : presDrugIds) {
                        String keyPresDrug = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
                        keys.add(keyPresDrug + "id");
                        keys.add(keyPresDrug + "info");
                    }

                    // effettuiamo una sola chiamata a MGET per tutti e i SOLI farmaci della prescrizione
                    List<String> values = jedis.mget(keys.toArray(new String[0]));
                    for (int i = 0; i < presDrugIds.size(); i++) {
                        int id_pres_drug = Integer.parseInt(presDrugIds.get(i));
                        String idDrug = values.get(i * 3);
                        String infoJson = values.get(i * 3 + 1);
                        String purchased = values.get(i * 3 + 2);
                        PrescribedDrugDTO drug = createPrescribedDrugDTO(idDrug, infoJson, purchased, id_pres_drug);
                        prescription.getPrescribedDrugs().add(drug);
                    }
                    return prescription;
                }
            }
        }
        throw new NotFoundException("The prescription cart of patient : " + id_pat + " does not exist");
    }

    /*public PrescriptionDTO getPrescriptionCart(String id_pat) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescriptionDTO prescription = new PrescriptionDTO();
            prescription.setTimestamp(null);
            for (int i = 1; i <= redisHelper.nEntities(jedis, this.pres); i++) {
                String keyPres = this.pres + ":" + i + ":" + id_pat + ":";
                // Controllare che le prescrizioni siano attive, quindi con timestamp != false
                if (jedis.exists(keyPres + "timestamp") && jedis.get(keyPres + "timestamp").isEmpty()) {
                    // allora si tratta di una prescrizione inattiva
                    // cerco all'interno della prescrizione alla ricerca di tutti i farmaci di essa
                    for (int j = 1; j <= redisHelper.nEntities(jedis, this.presDrug); j++) {
                        String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                        if (jedis.exists(keyPresDrug + "id")) {
                            // allora il farmaco è relativo a quella prescrizione dell'utente
                            prescription.addPrescribedDrug(createPrescribedDrugDTO(jedis, keyPresDrug, j));
                        }
                    }
                    break;
                }
            }
            return prescription;
        }
    }*/

    public PrescriptionDTO saveDrugIntoPrescriptionCart(String id_pat, int id_pres, PrescribedDrugDTO drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescriptionDTO prescriptionDTO = new PrescriptionDTO();
            boolean found = true;
            // se la prescrizione inattiva non esiste (id_pres = 0), devo crearla
            if (id_pres == 0) {
                found = false;
                id_pres = Integer.parseInt(redisHelper.getReusableId(jedis, this.pres));
            }
            // adesso possiamo inserire il farmaco presente all'interno del db
            String id_pres_drug = redisHelper.getReusableId(jedis, this.presDrug);
            String presKey = this.pres + ":" + id_pres + ":" + id_pat + ":";
            String presDrugKey = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
            String listKeyPres = this.pres + ":" + id_pat + ":set";
            String listKeyPresDrug = this.presDrug + ":" + id_pat + ":set";
            JsonObject info = new JsonObject();

            // Modifica dati della prescrizione
            if (!found) jedis.set(presKey + "timestamp", "");
            jedis.expire(presKey + "timestamp", this.day);

            // Modifica dati del farmaco
            jedis.set(presDrugKey + "id", String.valueOf(drug.getIdDrug()));
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());
            info.addProperty("quantity", drug.getQuantity());
            jedis.set(presDrugKey + "info", String.valueOf(info));
            jedis.set(presDrugKey + "purchased", String.valueOf(false));
            if(!found)
                // inserimento dell'id della nuova prescrizione all'interno del set delle prescrizioni
                jedis.sadd(listKeyPres, String.valueOf(id_pres));

            // inserimento dell'id del nuovo farmaco prescritto all'interno del set dei farmaci prescritti
            jedis.sadd(listKeyPresDrug, id_pres_drug);

            // dopo un giorno, se la prescrizione non viene attivata, il farmaco viene eliminato
            jedis.expire(presDrugKey + "id", this.day);
            jedis.expire(presDrugKey + "info", this.day);
            jedis.expire(presDrugKey + "purchased", this.day);
            jedis.expire(listKeyPres, this.month);
            jedis.expire(listKeyPresDrug, this.day);

            drug.setIdPresDrug(Integer.parseInt(id_pres_drug));
            prescriptionDTO.setIdPres(id_pres);
            prescriptionDTO.addPrescribedDrug(drug);
            return prescriptionDTO;
        }
    }

    public PrescribedDrugDTO deleteDrugIntoPrescriptionCart(String id_pat, int id_pres, int id_pres_drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescribedDrugDTO prescribedDrug;
            String keyPres = this.pres + ":" + id_pres + ":" + id_pat + ":";
            String keyPresDrug = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
            String timestamp = jedis.get(keyPres + "timestamp");
            if(timestamp == null)
                throw new NotFoundException("Not found any prescription with id " + id_pres + " related to patient " + id_pat);
            if(!timestamp.isEmpty())
                throw new NotFoundException("Not found any inactive prescription related to patient " + id_pat);

            // la prescrizione è inattiva
            if(jedis.exists(keyPresDrug + "id")){
                prescribedDrug = createPrescribedDrugDTO(jedis, keyPresDrug, id_pres_drug);
                // rimuovo il farmaco dalla prescrizione attiva
                jedis.del(keyPresDrug + "id");
                jedis.del(keyPresDrug + "info");
                jedis.del(keyPresDrug + "quantity");
                jedis.del(keyPresDrug + "purchased");
                redisHelper.returnIdToPool(jedis, this.presDrug, String.valueOf(id_pres_drug));
                return prescribedDrug;
            }
            // se qui allora il farmaco da cancellare non è esistente
            throw new NotFoundException("Not found the pres drug with id " + id_pres_drug);

            /*
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
             */
        }
    }

    public PrescribedDrugDTO modifyDrugQuantityIntoPrescriptionCart(String id_pat, int id_pres, int id_pres_drug, int quantity) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            String keyPres = this.pres + ":" + id_pres + ":" + id_pat + ":";
            String keyPresDrug = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
            String timestamp = jedis.get(keyPres + "timestamp");
            if(timestamp == null)
                throw new NotFoundException("Not found any prescription with id " + id_pres + " related to patient " + id_pat);
            if(!timestamp.isEmpty())
                throw new NotFoundException("Not found any inactive prescription related to patient " + id_pat);
            // la prescrizione è inattiva
            if(jedis.exists(keyPresDrug + "id")){
                jedis.set(keyPresDrug + "quantity", String.valueOf(quantity));
                return createPrescribedDrugDTO(jedis, quantity, keyPresDrug, id_pres_drug);
            }
            throw new NotFoundException("Not found any drug with pres_id "+ id_pres_drug +" into an inactive prescription");

            /*
            if(jedis.exists(keyPres + "timestamp") && jedis.get(keyPres + "timestamp").isEmpty()
                    && jedis.exists(keyPresDrug + "id")){
                jedis.set(keyPresDrug + "quantity", String.valueOf(quantity));
                return createPrescribedDrugDTO(jedis, quantity, keyPresDrug, id_pres_drug);
            }
            */
            /*
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

             */

        }
    }

    public PrescriptionDTO activatePrescriptionCart(String id_pat, int id_pres, List<Integer> id_pres_drugs) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescriptionDTO prescription = new PrescriptionDTO();
            prescription.setIdPres(id_pres);
            prescription.setTimestamp(LocalDateTime.now());
            String keyPres = this.pres + ":" + id_pres + ":" + id_pat + ":";
            int nDrugs = id_pres_drugs.size();
            if(nDrugs == 0)
                throw new ForbiddenException("The patient "+id_pat+" has no prescribed drugs in the cart.");

            /*
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
                            prescription.addPrescribedDrug(createPrescribedDrugDTO(jedis, keyDrugs, j));
                            keyDrugsList.add(keyDrugs);
                        }
                    }
                    break;
                }
            }*/

            // controllare che i farmaci all'interno del carrello abbiano tutti gli attributi esistenti
            // atomicità infatti non è garantita per gli inserimenti di farmaci
            for (int id_pres_drug : id_pres_drugs){
                String keyDrugs = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
                // modify time to expire for all drugs into the prescription to activate
                jedis.expire(keyDrugs + "id", this.month);
                // if there are any missing fields throw an exception because in not possible
                // create a prescription with missing values
                // can be missing because the application don't use atomic operation
                if(jedis.expire(keyDrugs + "info", this.month) == 0)
                    throw new BadRequestException("Some prescription drugs are missing fields");
                if(jedis.expire(keyDrugs + "quantity", this.month) == 0)
                    throw new BadRequestException("Some prescription drugs are missing fields");
                if(jedis.expire(keyDrugs + "purchased", this.month) == 0)
                    throw new BadRequestException("Some prescription drugs are missing fields");

                prescription.addPrescribedDrug(createPrescribedDrugDTO(jedis, keyDrugs, id_pres_drug));
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

    private PrescribedDrugDTO createPrescribedDrugDTO(String id_drug, String info_json, String purchased, int id_pres_drug){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        prescribedDrug.setIdPresDrug(id_pres_drug);
        prescribedDrug.setIdDrug(id_drug);
        JsonObject jsonObject = JsonParser.parseString(info_json).getAsJsonObject();
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(Integer.parseInt(jsonObject.get("quantity").getAsString()));
        prescribedDrug.setPurchased(Boolean.parseBoolean(purchased));
        return prescribedDrug;
    }
    /*
    private PrescribedDrugDTO createPrescribedDrugDTO(Jedis jedis, String key, int id_pres_drug){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        prescribedDrug.setIdPresDrug(id_pres_drug);
        prescribedDrug.setIdDrug(jedis.get(key + "id"));
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedis.get(key + "purchased")));
        return prescribedDrug;
    }
    /*
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

     */
    /*
    private PrescribedDrugDTO createPrescribedDrugDTO(Jedis jedis, int quantity, String key, int id_pres_drug){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        prescribedDrug.setIdPresDrug(id_pres_drug);
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setIdDrug(jedis.get(key + "id"));
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(quantity);
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedis.get(key + "purchased")));
        return prescribedDrug;
    }*/
}
