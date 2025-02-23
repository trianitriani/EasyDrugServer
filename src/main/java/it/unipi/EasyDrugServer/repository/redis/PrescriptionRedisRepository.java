package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
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
                String keyPres = this.pres + ":" + id_pres + ":" + id_pat + ":";
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

    public PrescriptionDTO getPrescriptionCart(String id_pat) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
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
                        String idDrug = values.get(i * 2);
                        String infoJson = values.get(i * 2 + 1);
                        PrescribedDrugDTO drug = createPrescribedDrugDTO(idDrug, infoJson, "false", id_pres_drug);
                        prescription.getPrescribedDrugs().add(drug);
                    }
                    return prescription;
                }
            }
            return prescription;
        }
    }

    public PrescriptionDTO saveDrugIntoPrescriptionCart(String id_pat, int id_pres, PrescribedDrugDTO drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescriptionDTO prescriptionDTO = new PrescriptionDTO();
            boolean found = true;
            // se la prescrizione inattiva non esiste (id_pres = 0), devo crearla
            if (id_pres == 0) {
                found = false;
                id_pres = Integer.parseInt(redisHelper.getReusableId(jedis, this.pres));
            }

            // controllare che il farmaco non sia già all'interno del carrello
            String listDrugKey = this.presDrug + ":" + id_pres + ":set";
            List<String> presDrugIds = new ArrayList<>(jedis.smembers(listDrugKey));
            List<String> keys = new ArrayList<>();
            for(String id_pres_drug : presDrugIds)
                keys.add(this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":id");

            if(!keys.isEmpty()){
                // leggere SOLO gli N id dei farmaci all'interno del carrello della prescrizione
                List<String> values = jedis.mget(keys.toArray(new String[0]));
                if(values.contains(drug.getIdDrug()))
                    throw new ForbiddenException("Drug " + drug.getIdDrug() + " is already into the prescription cart");
            }

            // adesso possiamo inserire il farmaco presente all'interno del db
            String id_pres_drug = redisHelper.getReusableId(jedis, this.presDrug);
            String presKey = this.pres + ":" + id_pres + ":" + id_pat + ":";
            String presDrugKey = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
            String listKeyPres = this.pres + ":" + id_pat + ":set";
            String listKeyPresDrug = this.presDrug + ":" + id_pres + ":set";
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
            if(!found)
                // inserimento dell'id della nuova prescrizione all'interno del set delle prescrizioni
                jedis.sadd(listKeyPres, String.valueOf(id_pres));

            // inserimento dell'id del nuovo farmaco prescritto all'interno del set dei farmaci prescritti
            jedis.sadd(listKeyPresDrug, id_pres_drug);

            // dopo un giorno, se la prescrizione non viene attivata, il farmaco viene eliminato
            jedis.expire(presDrugKey + "id", this.day);
            jedis.expire(presDrugKey + "info", this.day);
            jedis.expire(listKeyPres, this.month);
            jedis.expire(listKeyPresDrug, this.day);

            drug.setIdPresDrug(Integer.parseInt(id_pres_drug));
            drug.setPurchased(false);
            prescriptionDTO.setIdPres(id_pres);
            prescriptionDTO.addPrescribedDrug(drug);
            return prescriptionDTO;
        }
    }

    public PrescribedDrugDTO deleteDrugIntoPrescriptionCart(String id_pat, int id_pres, int id_pres_drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            String keyPres = this.pres + ":" + id_pres + ":" + id_pat + ":";
            String keyPresDrug = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
            String timestamp = jedis.get(keyPres + "timestamp");
            if(timestamp == null)
                throw new NotFoundException("Not found any prescription with id " + id_pres + " related to patient " + id_pat);
            if(!timestamp.isEmpty())
                throw new NotFoundException("Not found any prescription cart related to patient " + id_pat);

            // la prescrizione è inattiva
            String idDrug = jedis.get(keyPresDrug + "id");
            if(idDrug != null){
                String infoJson = jedis.get(keyPres + "info");
                String listKey = this.presDrug + ":" + id_pres + ":set";
                // prima rimuovo l'id dal set dei farmaci nel carrello
                jedis.srem(listKey, String.valueOf(id_pres_drug));
                // rimuovo le informazioni sul farmaco dal carrello
                jedis.del(keyPresDrug + "id");
                jedis.del(keyPresDrug + "info");
                redisHelper.returnIdToPool(jedis, this.presDrug, String.valueOf(id_pres_drug));
                return createPrescribedDrugDTO(idDrug, infoJson, "false", id_pres_drug);
            }
            // se qui allora il farmaco da cancellare non è esistente
            throw new NotFoundException("Not found the pres drug with id " + id_pres_drug);
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
            String idDrug = jedis.get(keyPresDrug + "id");
            if(idDrug != null){
                // modifica il campo quantità
                String infoJson = jedis.get(keyPresDrug + "info");
                JsonObject jsonObject = JsonParser.parseString(infoJson).getAsJsonObject();
                jsonObject.addProperty("quantity", quantity);
                infoJson = jsonObject.toString();
                jedis.set(keyPresDrug + "info", infoJson);
                return createPrescribedDrugDTO(idDrug, infoJson, "false", id_pres_drug);
            }
            throw new NotFoundException("Not found any drug with pres_id "+ id_pres_drug +" into an inactive prescription");
        }
    }

    public PrescriptionDTO activatePrescriptionCart(String id_pat, int id_pres) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            PrescriptionDTO prescription = new PrescriptionDTO();
            prescription.setIdPres(id_pres);
            prescription.setTimestamp(LocalDateTime.now());
            // controllare che la prescrizione esista e sia un carrello (inattiva)
            String keyPres = this.pres + ":" + id_pres + ":" + id_pat + ":";
            String timestamp = jedis.get(keyPres + "timestamp");
            if(timestamp == null)
                throw new NotFoundException("Not found any prescription with id " + id_pres + " related to patient " + id_pat);
            if(!timestamp.isEmpty())
                throw new NotFoundException("Not found any inactive prescription related to patient " + id_pat);

            // ottenimento di tutti e i SOLI farmaci del carrello delle prescrizioni
            String listPresDrug = this.presDrug + ":" + id_pres + ":set";
            String listPres = this.pres + ":"  + id_pat + ":set";
            List<String> presDrugIds = new ArrayList<>(jedis.smembers(listPresDrug));
            int nDrugs = presDrugIds.size();
            if(nDrugs == 0)
                throw new ForbiddenException("The patient "+id_pat+" has no drugs in the cart.");

            // otteniamo la lista di tutte le chiavi da leggere da redis
            List<String> keys = new ArrayList<>();
            for (String id_pres_drug : presDrugIds) {
                String keyPresDrug = this.presDrug + ":" + id_pres_drug + ":" + id_pres + ":";
                keys.add(keyPresDrug + "id");
                keys.add(keyPresDrug + "info");
                // se è la prima volta ad inserirle
                jedis.set(keyPresDrug + "purchased", "false");
                // modify time to expire for all drugs into the prescription to activate
                jedis.expire(keyPresDrug + "id", this.month);
                jedis.expire(keyPresDrug + "info", this.month);
                jedis.expire(keyPresDrug + "purchased", this.month);
            }

            // effettuiamo una sola chiamata a MGET per tutti e i SOLI farmaci della prescrizione
            List<String> values = jedis.mget(keys.toArray(new String[0]));
            for (int i = 0; i < presDrugIds.size(); i++) {
                int id_pres_drug = Integer.parseInt(presDrugIds.get(i));
                String idDrug = values.get(i * 2);
                String infoJson = values.get(i * 2 + 1);
                PrescribedDrugDTO drug = createPrescribedDrugDTO(idDrug, infoJson, "false", id_pres_drug);
                prescription.getPrescribedDrugs().add(drug);
            }

            // setto il numero di farmaci all'interno
            jedis.set(keyPres + "toPurchase", String.valueOf(nDrugs));
            // setto il timestamp a quello di ora e conto i farmaci relativi a quella prescrizione
            jedis.set(keyPres + "timestamp", String.valueOf(prescription.getTimestamp()));

            // un mese dopo la sua creazione la prescrizione viene eliminata
            jedis.expire(keyPres + "timestamp", this.month);
            jedis.expire(keyPres + "toPurchase", this.month);
            jedis.expire(listPres, this.month);
            jedis.expire(listPresDrug, this.month);
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
}
