package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Setter
@Getter
@Repository
public class PurchaseCartRedisRepository {
    private final String entity = "purch-drug";
    private final Jedis jedis;
    private final RedisHelper redisHelper;
    private final int hour = 3600;

    /*
        purch-drug:purch-drug:id_pat:id
        purch-drug:purch-drug:id_pat:info { name, price, prescriptionTimestamp }
        purch-drug:purch-drug:id_pat:quantity
     */

    public PurchaseCartRedisRepository(Jedis jedis, RedisHelper redisHelper) {
        this.jedis = jedis;
        this.redisHelper = redisHelper;
    }

    public List<PurchaseDrugDTO> getPurchaseCart(String patientCode){
        List<PurchaseDrugDTO> cartList = new ArrayList<>();
        for(int i=0; i<=redisHelper.nEntities(jedis, this.entity); i++){
            String key = this.entity + ":" + i + ":" + patientCode + ":";
            if(!jedis.exists(key + "id")) continue;
            // Se sono qui significa che l'oggetto esiste realmente e lo inserisco nella lista
            PurchaseDrugDTO drug = createPurchaseDrugDTO(key);
            cartList.add(drug);
        }
        return cartList;
    }

    public PurchaseDrugDTO insertPurchaseDrug(String patientCode, PurchaseDrugDTO drug) {
        Transaction transaction = jedis.multi();
        JsonObject info = new JsonObject();
        info.addProperty("name", drug.getName());
        info.addProperty("price", drug.getPrice());
        info.addProperty("prescriptionTimestamp", String.valueOf(drug.getPrescriptionTimestamp()));

        int quantity = drug.getQuantity();
        // Now we have to search a valid id_purch for a new element
        String id_purch = redisHelper.getReusableId(jedis, this.entity);
        String key = this.entity + ":" + id_purch + ":" + patientCode + ":";
        transaction.set(key + "id", String.valueOf(drug.getId()));
        transaction.set(key + "info", String.valueOf(info));
        transaction.set(key + "quantity", String.valueOf(quantity));

        // se passa un giorno, i farmaci vengono eliminati dal carrello
        transaction.expire(key + "id", this.hour);
        transaction.expire(key + "info", this.hour);
        transaction.expire(key + "quantity", this.hour);

        List<Object> result = transaction.exec();
        if(result == null)
            throw new JedisException("Error in the transaction");

        return drug;
    }

    public PurchaseDrugDTO modifyPurchaseDrugQuantity(String patientCode, int idDrug, int quantity) {
        // ricavare il farmaco con id = idDrug
        for(int i=0; i<=redisHelper.nEntities(jedis, this.entity); i++){
            String key = this.entity + ":" + i + ":" + patientCode + ":";
            if(jedis.exists(key + "id")){
                if(idDrug != Integer.parseInt(jedis.get(key + "id"))) continue;
                // modifica il campo quantità
                jedis.set(key + "quantity", String.valueOf(quantity));
                return createPurchaseDrugDTO(idDrug, quantity, key);
            }
        }
        throw new NotFoundException("Impossibile modify the drug: patient "+patientCode+" has not drug with id "+idDrug+" in the cart.");
    }

    public PurchaseDrugDTO deletePurchaseDrug(String patientCode, int idDrug) {
        Transaction transaction = jedis.multi();
        for(int i=0; i<=redisHelper.nEntities(jedis, this.entity); i++) {
            String key = this.entity + ":" + i + ":" + patientCode + ":";
            if (jedis.exists(key + "info")) {
                if (idDrug != Integer.parseInt(jedis.get(key + "id"))) continue;
                // elimino il farmaco
                PurchaseDrugDTO drug = createPurchaseDrugDTO(idDrug, key);
                transaction.del(key + "id");
                transaction.del(key + "info");
                transaction.del(key + "quantity");
                redisHelper.returnIdToPool(transaction, this.entity, String.valueOf(i));

                List<Object> result = transaction.exec();
                if(result == null)
                    throw new JedisException("Error in the transaction");
                return drug;
            }
        }
        throw new NotFoundException("Impossibile delete the drug: patient "+patientCode+" has not drug with id "+idDrug+" in the cart.");
    }

    public List<PurchaseDrugDTO> confirmPurchaseCart(String patientCode){
        HashMap<String, List<Integer>> prescribedDrugs = new HashMap<>();
        List<PurchaseDrugDTO> purchaseDrugs = new ArrayList<>();
        Transaction transaction = jedis.multi();
        // cerco tutti i farmaci che sono nel carrello e si riferiscono al paziente selezionato
        for(int i=1; i<=redisHelper.nEntities(jedis, this.entity); i++){
            String keyPurch = this.entity + ":" + i + ":" + patientCode + ":";
            if(jedis.exists(keyPurch + "id")){
                PurchaseDrugDTO drug = createPurchaseDrugDTO(keyPurch);
                purchaseDrugs.add(drug);
                int id = drug.getId();
                // aggiornare hash per diverse prescrizioni di farmaci prescritti acquistati.
                if(drug.getPrescriptionTimestamp() != null){
                    // allora il farmaco è relativo a una prescrizione
                    String timestampString = String.valueOf(drug.getPrescriptionTimestamp());
                    if(!prescribedDrugs.containsKey(timestampString))
                        prescribedDrugs.put(timestampString, new ArrayList<>());
                    prescribedDrugs.get(timestampString).add(id);
                }
                // elimino dal key value il farmaco nel carrello
                transaction.del(keyPurch + "id");
                transaction.del(keyPurch + "info");
                transaction.del(keyPurch + "quantity");
                redisHelper.returnIdToPool(transaction, this.entity, String.valueOf(i));
            }
        }
        // adesso per ogni prescrizione che contiene farmaci acquistati vado a modificare il db
        // segnando quel farmaco come acquistato e controllando se una prescrizione è conclusa.
        for(int j=1; j<=redisHelper.nEntities(jedis, "pres"); j++){
            String presKey = "pres:" + j + ":" + patientCode + ":";
            if(jedis.exists(presKey + "timestamp")){
                String stringTimestamp = jedis.get(presKey + "timestamp");
                if(Objects.equals(stringTimestamp, "false")) continue;
                // allora la prescrizione è attiva
                if(!prescribedDrugs.containsKey(stringTimestamp)) continue;
                // allora sono stati acquistati farmaci di quella prescrizione
                List<Integer> drugs = prescribedDrugs.get(stringTimestamp);
                int nPurchased = drugs.size();
                boolean ended = false;
                int toPurchase = Integer.parseInt(jedis.get(presKey+" toPurchase"));
                if(nPurchased == toPurchase){
                    // allora significa che tutti i farmaci della prescrizione sono stati acquistati
                    ended = true;
                }
                // ciclo tutti i farmaci
                for(int k=1; k<=redisHelper.nEntities(jedis, "pres-drug"); k++){
                    String presDrugKey = "pres-drug:" + k + ":" + j + ":";
                    if(jedis.exists(presDrugKey + "id")){
                        int id = Integer.parseInt(jedis.get((presDrugKey + "id")));
                        if(drugs.contains(id)){
                            if (ended){
                                // Allora vado a eliminare quel farmaco
                                transaction.del(presDrugKey + "id");
                                transaction.del(presDrugKey + "info");
                                transaction.del(presDrugKey + "quantity");
                                transaction.del(presDrugKey + "purchased");
                                redisHelper.returnIdToPool(transaction, "pres-drug", Integer.toString(k));
                            } else {
                                transaction.set(presDrugKey + "purchased", "true");
                            }
                        }
                    }
                }
                if(ended) {
                    transaction.del(presKey + "timestamp");
                    transaction.del(presKey + "toPurchase");
                    redisHelper.returnIdToPool(transaction, "pres", Integer.toString(j));
                } else {
                    toPurchase -= nPurchased;
                    transaction.set(presKey + "toPurchase", String.valueOf(toPurchase));
                }
            }
        }
        if(purchaseDrugs.isEmpty())
            throw new ForbiddenException("You can not complete the payment if a cart is empty");

        List<Object> result = transaction.exec();
        if(result == null)
            throw new JedisException("Error in the transaction");

        return purchaseDrugs;
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(String key) {
        PurchaseDrugDTO prescribedDrug = new PurchaseDrugDTO();
        prescribedDrug.setId(Integer.parseInt(jedis.get(key + "id")));
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        if(Objects.equals(timestampString, "null")){
            prescribedDrug.setPrescriptionTimestamp(null);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
            prescribedDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        }
        prescribedDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        return prescribedDrug;
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(int id, String key){
        PurchaseDrugDTO prescribedDrug = new PurchaseDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        if(Objects.equals(timestampString, "null")){
            prescribedDrug.setPrescriptionTimestamp(null);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
            prescribedDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        }
        prescribedDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        return prescribedDrug;
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(int id, int quantity, String key){
        PurchaseDrugDTO prescribedDrug = new PurchaseDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        if(Objects.equals(timestampString, "null")){
            prescribedDrug.setPrescriptionTimestamp(null);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
            prescribedDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        }
        prescribedDrug.setQuantity(quantity);
        return prescribedDrug;
    }
}
