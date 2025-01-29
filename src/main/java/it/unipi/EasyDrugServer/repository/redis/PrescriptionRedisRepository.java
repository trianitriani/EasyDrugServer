package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.exception.UnauthorizedException;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Transaction;
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
    private final Jedis jedis;
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

    public PrescriptionRedisRepository(Jedis jedis, RedisHelper redisHelper) {
        this.jedis = jedis;
        this.redisHelper = redisHelper;
    }

    public List<PrescriptionDTO> getAllActivePrescriptions(String patientCode) {
        List<PrescriptionDTO> prescriptions = new ArrayList<>();
        for (int i = 1; i <= redisHelper.nEntities(jedis, this.pres); i++) {
            String keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            // Controllare che le prescrizioni siano attive, quindi con timestamp != false
            if(jedis.exists(keyPres + "timestamp") &&
                    !Objects.equals(jedis.get(keyPres + "timestamp"), "")){
                // allora si tratta di una prescrizione attiva
                String timestampString = jedis.get(keyPres + "timestamp");
                System.out.println(timestampString);
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                LocalDateTime timestamp = LocalDateTime.parse(timestampString, formatter);

                // cerco all'interno della prescrizione alla ricerca di tutti i farmaci di essa
                PrescriptionDTO prescription = new PrescriptionDTO();
                prescription.setTimestamp(timestamp);
                for(int j = 1; j<=redisHelper.nEntities(jedis, this.presDrug); j++) {
                    String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                    if(jedis.exists(keyPresDrug + "id")){
                        // allora il farmaco è relativo a quella prescrizione dell'utente
                        prescription.addPrescribedDrug(createPrescribedDrugDTO(keyPresDrug));
                    }
                }
                prescriptions.add(prescription);
            }
        }
        return prescriptions;
    }

    public PrescriptionDTO getInactivePrescription(String patientCode) {
        PrescriptionDTO prescription = new PrescriptionDTO();
        prescription.setTimestamp(null);
        for (int i = 1; i <= redisHelper.nEntities(jedis, this.pres); i++) {
            String keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            // Controllare che le prescrizioni siano attive, quindi con timestamp != false
            if(jedis.exists(keyPres + "timestamp") &&
                    Objects.equals(jedis.get(keyPres + "timestamp"), "")){
                // allora si tratta di una prescrizione inattiva
                // cerco all'interno della prescrizione alla ricerca di tutti i farmaci di essa
                for(int j = 1; j<=redisHelper.nEntities(jedis, this.presDrug); j++) {
                    String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                    if(jedis.exists(keyPresDrug + "id")){
                        // allora il farmaco è relativo a quella prescrizione dell'utente
                        prescription.addPrescribedDrug(createPrescribedDrugDTO(keyPresDrug));
                    }
                }
                break;
            }
        }
        return prescription;
    }

    public PrescribedDrugDTO insertInactivePrescribedDrug(String patientCode, PrescribedDrugDTO drug) {
        boolean found = false;
        // controllare se esiste una prescrizione inattiva del paziente
        String presKey = "";
        int i;
        for(i=1; i<=redisHelper.nEntities(jedis, this.pres); i++){
            presKey = this.pres + ":" + i + ":" + patientCode + ":";
            if(jedis.exists(presKey + "timestamp")){
                String timestamp = jedis.get(presKey + "timestamp");
                if(!Objects.equals(timestamp, "")) continue;
                // trovato l'id della prescrizione inattiva devo inserire all'interno
                // della prescrizione un nuovo farmaco
                found = true;
                break;
            }
        }
        
        // se la prescrizione inattiva non esiste, devo crearla
        if(!found){
            int id_pres = Integer.parseInt(redisHelper.getReusableId(jedis, this.pres));
            presKey = this.pres + ":" + id_pres + ":" + patientCode + ":";
            i = id_pres;
        }
        // adesso possiamo inserire il farmaco presente all'interno del db
        String id_presDrug = redisHelper.getReusableId(jedis, this.presDrug);
        String presDrugKey = this.presDrug + ":" + id_presDrug + ":" + i + ":";
        JsonObject info = new JsonObject();

        // Modifica il k-value in maniera atomica
        Transaction transaction = jedis.multi();

        // Modifica dati della prescrizione
        if(!found) transaction.set(presKey + "timestamp", "");
        transaction.expire(presKey + "timestamp", this.day);

        // Modifica dati del farmaco
        transaction.set(presDrugKey + "id", String.valueOf(drug.getId()));
        info.addProperty("name", drug.getName());
        info.addProperty("price", drug.getPrice());
        transaction.set(presDrugKey + "info", String.valueOf(info));
        transaction.set(presDrugKey + "quantity", String.valueOf(drug.getQuantity()));
        transaction.set(presDrugKey + "purchased", String.valueOf(drug.isPurchased()));

        // dopo un giorno, se la prescrizione non viene attivata, il farmaco viene eliminato
        transaction.expire(presDrugKey + "id", this.day);
        transaction.expire(presDrugKey + "info", this.day);
        transaction.expire(presDrugKey + "quantity", this.day);
        transaction.expire(presDrugKey + "purchased", this.day);
        List<Object> result = transaction.exec();
        if(result == null)
            throw new JedisException("Error in the transaction");

        return drug;
    }

    public PrescribedDrugDTO deleteInactivePrescribedDrug(String patientCode, int idDrug) {
        PrescribedDrugDTO prescribedDrug;
        for(int i = 1; i<=redisHelper.nEntities(jedis, this.pres); i++) {
            // cerco l'unica prescrizione inattiva con timestamp false
            String keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            if (jedis.exists(keyPres + "timestamp") &&
                    Objects.equals(jedis.get(keyPres + "timestamp"), "")) {

                // l'id della prescrizione inattiva è i, adesso devo cercare il farmaco da eliminare
                for(int j = 1; j<=redisHelper.nEntities(jedis, this.presDrug); j++) {
                    String keyPresDrug = this.presDrug + ":" + j + ":" + i + ":";
                    if(jedis.exists(keyPresDrug + "id")){
                        if(idDrug == Integer.parseInt(jedis.get(keyPresDrug + "id"))){
                            // è il farmaco da rimuovere
                            // preparo l'oggetto per il ritorno
                            prescribedDrug = createPrescribedDrugDTO(idDrug, keyPresDrug);
                            // rimuovo il farmaco dalla prescrizione attiva
                            Transaction transaction = jedis.multi();
                            transaction.del(keyPresDrug + "id");
                            transaction.del(keyPresDrug + "info");
                            transaction.del(keyPresDrug + "quantity");
                            transaction.del(keyPresDrug + "purchased");
                            redisHelper.returnIdToPool(transaction, this.presDrug, String.valueOf(i));
                            List<Object> result = transaction.exec();
                            if(result == null)
                                throw new JedisException("Error in the transaction");
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

    public PrescribedDrugDTO modifyInactivePrescribedDrugQuantity(String patientCode, int idDrug, int quantity) {
        // cercare l'id della prescrizione inattiva
        int index_pres = 0;
        for(int i = 1; i<=redisHelper.nEntities(jedis, this.pres); i++) {
            // cerco l'unica prescrizione inattiva con timestamp false
            String keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            if(jedis.exists(keyPres + "timestamp") && Objects.equals(jedis.get(keyPres + "timestamp"), "")){
                index_pres = i;
                break;
            }
        }
        if(index_pres == 0)
            throw new NotFoundException("Not found any inactive prescription for patient "+patientCode);

        // adesso dato l'id della prescrizione trovo il farmaco a cui modificare la quantità
        for(int i = 1; i<=redisHelper.nEntities(jedis, this.presDrug); i++) {
            // cerco tutti i farmaci con il campo "timestamp" non esistente (quelli non ancora confermati)
            String keyPresDrug = this.presDrug + ":" + i + ":" + index_pres + ":";
            if(jedis.exists(keyPresDrug + "id")){
                if(idDrug == Integer.parseInt(jedis.get(keyPresDrug + "id"))){
                    // ho trovato il farmaco da modificare
                    jedis.set(keyPresDrug + "quantity", String.valueOf(quantity));
                    return createPrescribedDrugDTO(idDrug, quantity, keyPresDrug);
                }
            }
        }
        throw new NotFoundException("Not found any drug with id "+idDrug+" into an inactive prescription");
    }

    public PrescriptionDTO activatePrescription(String patientCode) {
        PrescriptionDTO prescription = new PrescriptionDTO();
        prescription.setTimestamp(LocalDateTime.now());
        String keyPres = "";
        List<String> keyDrugsList = new ArrayList<>();
        int  nDrugs = 0;
        for(int i = 1; i<=redisHelper.nEntities(jedis, this.pres); i++){
            // cerco la prescrizione con dato id
            keyPres = this.pres + ":" + i + ":" + patientCode + ":";
            if(jedis.exists(keyPres + "timestamp") && Objects.equals(jedis.get(keyPres + "timestamp"), "")){
                // cicliamo i vari farmaci della prescrizione
                for (int j = 1; j<=redisHelper.nEntities(jedis, this.presDrug); j++){
                    String keyDrugs = this.presDrug + ":" + j + ":" + i + ":";
                    if(jedis.exists(keyDrugs + "id")) {
                        nDrugs++;
                        // inserisco il farmaco prescritto nella lista di prescrizione per ritornarlo
                        prescription.addPrescribedDrug(createPrescribedDrugDTO(keyDrugs));
                        keyDrugsList.add(keyDrugs);
                    }
                }
                break;
            }
        }
        if(prescription.checkIfEmpty())
            throw new ForbiddenException("The patient "+patientCode+" has no prescriptions.");

        Transaction transaction = jedis.multi();
        // Modify db in atomic way
        for (String keyDrugs : keyDrugsList){
            // modify time to expire for all drugs into the prescription to activate
            transaction.expire(keyDrugs + "id", this.month);
            transaction.expire(keyDrugs + "info", this.month);
            transaction.expire(keyDrugs + "quantity", this.month);
            transaction.expire(keyDrugs + "purchased", this.month);
        }

        // setto il timestamp a quello di ora e conto i farmaci relativi a quella prescrizione
        transaction.set(keyPres + "timestamp", String.valueOf(prescription.getTimestamp()));
        // setto il numero di farmaci all'interno
        transaction.set(keyPres + "toPurchase", String.valueOf(nDrugs));

        // un mese dopo la sua creazione la prescrizione viene eliminata
        transaction.expire(keyPres + "timestamp", this.month);
        transaction.expire(keyPres + "toPurchase", this.month);
        List<Object> result = transaction.exec();
        if(result == null)
            throw new JedisException("Error in the transaction");

        return prescription;
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(String key){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        prescribedDrug.setId(Integer.parseInt(jedis.get(key + "id")));
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedis.get(key + "purchased")));
        return prescribedDrug;
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(int id, String key){
        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        prescribedDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        prescribedDrug.setPurchased(Boolean.parseBoolean(jedis.get(key + "purchased")));
        return prescribedDrug;
    }

    private PrescribedDrugDTO createPrescribedDrugDTO(int id, int quantity, String key){
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
