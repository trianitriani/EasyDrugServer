package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.ConfirmPurchaseCartDTO;
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
import redis.clients.jedis.exceptions.JedisException;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Setter
@Getter
@Repository
@Retryable(
        retryFor = { JedisConnectionException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
)
public class PurchaseCartRedisRepository {
    private final String entity = "purch-drug";
    private final JedisSentinelPool jedisSentinelPool;
    private final RedisHelper redisHelper;
    private final int hour = 3600;

    /*
        purch-drug:purch-drug_id:id_pat:id
        purch-drug:purch-drug_id:id_pat:info { name, price, prescriptionTimestamp }
        purch-drug:purch-drug_id:id_pat:quantity
     */

    @Autowired
    public PurchaseCartRedisRepository(JedisSentinelPool jedisSentinelPool, RedisHelper redisHelper) {
        this.jedisSentinelPool = jedisSentinelPool;
        this.redisHelper = redisHelper;
    }

    public List<PurchaseCartDrugDTO> getPurchaseCart(String id_pat){
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            List<PurchaseCartDrugDTO> cartList = new ArrayList<>();
            for(int i=0; i<=redisHelper.nEntities(jedis, this.entity); i++){
                String key = this.entity + ":" + i + ":" + id_pat + ":";
                if(!jedis.exists(key + "id")) continue;
                // Se sono qui significa che l'oggetto esiste realmente e lo inserisco nella lista
                PurchaseCartDrugDTO drug = createPurchaseCartDrugDTO(jedis, key, i);
                cartList.add(drug);
            }
            return cartList;
        }
    }

    public PurchaseCartDrugDTO insertPurchaseDrug(String id_pat, PurchaseCartDrugDTO drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            JsonObject info = new JsonObject();
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());
            if (drug.getPrescriptionTimestamp() == null) {
                info.addProperty("prescriptionTimestamp", "");
            } else info.addProperty("prescriptionTimestamp", String.valueOf(drug.getPrescriptionTimestamp()));

            // now we have to search a valid id_purch for a new element
            String id_purch = redisHelper.getReusableId(jedis, this.entity);
            drug.setIdPurchDrug(Integer.parseInt(id_purch));
            String key = this.entity + ":" + id_purch + ":" + id_pat + ":";

            // insert a drug into a purchase cart
            jedis.set(key + "id", String.valueOf(drug.getIdDrug()));
            jedis.set(key + "info", String.valueOf(info));
            jedis.set(key + "quantity", String.valueOf(drug.getQuantity()));

            // expire of one hour for delete an object into purchase cart
            jedis.expire(key + "id", this.hour);
            jedis.expire(key + "info", this.hour);
            jedis.expire(key + "quantity", this.hour);
            return drug;
        }
    }

    public PurchaseCartDrugDTO modifyPurchaseDrugQuantity(String id_pat, int id_purch_drug, int quantity) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            // searching drug with idDrug
            String key = this.entity + ":" + id_purch_drug + ":" + id_pat + ":";
            if (jedis.exists(key + "id")) {
                // modifica il campo quantità
                jedis.set(key + "quantity", String.valueOf(quantity));
                return createPurchaseCartDrugDTO(jedis, quantity, key, id_purch_drug);
            }
            throw new NotFoundException("Impossibile to modify the drug: patient " + id_pat + " has no drug with purch_id" + id_purch_drug + " in the cart.");
        }
    }

    public PurchaseCartDrugDTO deletePurchaseDrug(String id_pat, int id_purch_drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()){
            // searching into purchase cart the specific drug
            String key = this.entity + ":" + id_purch_drug + ":" + id_pat + ":";
            if (jedis.exists(key + "id")) {
                PurchaseCartDrugDTO drug = createPurchaseCartDrugDTO(jedis, key, id_purch_drug);
                jedis.del(key + "id");
                jedis.del(key + "info");
                jedis.del(key + "quantity");
                redisHelper.returnIdToPool(jedis, this.entity, String.valueOf(id_purch_drug));
                return drug;
            }
            throw new NotFoundException("Impossibile to delete the drug: patient "+id_pat+" " + "has no purch drug with id "+ id_purch_drug +" in the cart.");
        }
    }

    public ConfirmPurchaseCartDTO confirmPurchaseCart(String id_pat, List<Integer> id_purch_drugs){
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            LinkedHashMap<String, List<String>> prescribedDrugs = new LinkedHashMap<>();
            List<PurchaseCartDrugDTO> purchaseDrugs = new ArrayList<>();
            LinkedHashMap<String, Integer> purchToDelete = new LinkedHashMap<>();
            LinkedHashMap<String, Integer> presDrugToDelete = new LinkedHashMap<>();
            List<String> presDrugPurchased = new ArrayList<>();
            LinkedHashMap<String, Integer> presToDelete = new LinkedHashMap<>();
            LinkedHashMap<String, Integer> presToModify = new LinkedHashMap<>();

            for(int id_purch_drug: id_purch_drugs){
                String keyPurch = this.entity + ":" + id_purch_drug + ":" + id_pat + ":";
                PurchaseCartDrugDTO drug = createPurchaseCartDrugDTO(jedis, keyPurch, id_purch_drug);
                purchaseDrugs.add(drug);
                String id = drug.getIdDrug();
                // aggiornare hash per diverse prescrizioni di farmaci prescritti acquistati.
                if (drug.getPrescriptionTimestamp() != null) {
                    // allora il farmaco è relativo a una prescrizione
                    String timestampString = String.valueOf(drug.getPrescriptionTimestamp());
                    if (!prescribedDrugs.containsKey(timestampString))
                        prescribedDrugs.put(timestampString, new ArrayList<>());
                    prescribedDrugs.get(timestampString).add(id);
                }
                // if there are any missing fields throw an exception because in not possible
                // create a prescription with missing values
                // can be missing because the application don't use atomic operation
                if(!jedis.exists(keyPurch + "info"))
                    throw new BadRequestException("Some purchases drugs are missing fields");
                if(!jedis.exists(keyPurch + "quantity"))
                    throw new BadRequestException("Some purchases drugs are missing fields");
                // elimino dal key value il farmaco nel carrello
                purchToDelete.put(keyPurch, id_purch_drug);
            }

            /*
            // cerco tutti i farmaci che sono nel carrello e si riferiscono al paziente selezionato
            for (int i = 1; i <= redisHelper.nEntities(jedis, this.entity); i++) {
                String keyPurch = this.entity + ":" + i + ":" + id_pat + ":";
                if (jedis.exists(keyPurch + "id")) {
                    PurchaseCartDrugDTO drug = createPurchaseCartDrugDTO(jedis, keyPurch, i);
                    purchaseDrugs.add(drug);
                    String id = drug.getIdDrug();
                    // aggiornare hash per diverse prescrizioni di farmaci prescritti acquistati.
                    if (drug.getPrescriptionTimestamp() != null) {
                        // allora il farmaco è relativo a una prescrizione
                        String timestampString = String.valueOf(drug.getPrescriptionTimestamp());
                        if (!prescribedDrugs.containsKey(timestampString))
                            prescribedDrugs.put(timestampString, new ArrayList<>());
                        prescribedDrugs.get(timestampString).add(id);
                    }
                    // if there are any missing fields throw an exception because in not possible
                    // create a prescription with missing values
                    // can be missing because the application don't use atomic operation
                    if(!jedis.exists(keyPurch + "info"))
                        throw new BadRequestException("Some purchases drugs are missing fields");
                    if(!jedis.exists(keyPurch + "quantity"))
                        throw new BadRequestException("Some purchases drugs are missing fields");
                    // elimino dal key value il farmaco nel carrello
                    purchToDelete.put(keyPurch, i);
                }
            }
             */

            // adesso per ogni prescrizione che contiene farmaci acquistati vado a modificare il db
            // segnando quel farmaco come acquistato e controllando se una prescrizione è conclusa.
            for (int j = 1; j <= redisHelper.nEntities(jedis, "pres"); j++) {
                String presKey = "pres:" + j + ":" + id_pat + ":";
                if (jedis.exists(presKey + "timestamp")) {
                    String stringTimestamp = jedis.get(presKey + "timestamp");
                    if (stringTimestamp.isEmpty()) continue;
                    // allora la prescrizione è attiva
                    if (!prescribedDrugs.containsKey(stringTimestamp)) continue;
                    // allora sono stati acquistati farmaci di quella prescrizione
                    List<String> drugs = prescribedDrugs.get(stringTimestamp);
                    int nPurchased = drugs.size();
                    boolean ended = false;
                    int toPurchase = Integer.parseInt(jedis.get(presKey + "toPurchase"));
                    if (nPurchased == toPurchase) {
                        // allora significa che tutti i farmaci della prescrizione sono stati acquistati
                        ended = true;
                    }
                    // ciclo tutti i farmaci
                    for (int k = 1; k <= redisHelper.nEntities(jedis, "pres-drug"); k++) {
                        String presDrugKey = "pres-drug:" + k + ":" + j + ":";
                        if (jedis.exists(presDrugKey + "id")) {
                            String id = jedis.get((presDrugKey + "id"));

                            if (drugs.contains(id)) {
                                if (ended) {
                                    // Allora vado a eliminare quel farmaco
                                    presDrugToDelete.put(presDrugKey, k);
                                } else {
                                    presDrugPurchased.add(presDrugKey);
                                }
                            }
                        }
                    }
                    if (ended)
                        presToDelete.put(presKey, j);
                    else {
                        toPurchase -= nPurchased;
                        presToModify.put(presKey, toPurchase);
                    }
                }
            }
            if (purchaseDrugs.isEmpty())
                throw new ForbiddenException("You can not complete the payment if a cart is empty");

            // *************************************************** //

            // effettuiamo le modifiche nel db
            Transaction transaction = jedis.multi();

            // delete purchase drug into the cart
            for (Map.Entry<String, Integer> entry : purchToDelete.entrySet()) {
                transaction.del(entry.getKey() + "id");
                transaction.del(entry.getKey() + "info");
                transaction.del(entry.getKey() + "quantity");
                redisHelper.returnIdToPool(transaction, this.entity, String.valueOf(entry.getValue()));
            }

            // delete purchase drug into prescriptions that are sold
            for (Map.Entry<String, Integer> entry : presDrugToDelete.entrySet()) {
                transaction.del(entry.getKey() + "id");
                transaction.del(entry.getKey() + "info");
                transaction.del(entry.getKey() + "quantity");
                transaction.del(entry.getKey() + "purchased");
                redisHelper.returnIdToPool(transaction, "pres-drug", Integer.toString(entry.getValue()));
            }

            // setting purchased to a drug into a prescription that is sold
            for (String presDrugKey : presDrugPurchased) {
                transaction.set(presDrugKey + "purchased", "true");
            }

            // delete prescription if is terminated (all drugs are sold)
            for (Map.Entry<String, Integer> entry : presToDelete.entrySet()) {
                transaction.del(entry.getKey() + "timestamp");
                transaction.del(entry.getKey() + "toPurchase");
                redisHelper.returnIdToPool(transaction, "pres", Integer.toString(entry.getValue()));
            }

            for (Map.Entry<String, Integer> entry : presToModify.entrySet()) {
                transaction.set(entry.getKey() + "toPurchase", String.valueOf(entry.getValue()));
            }

            ConfirmPurchaseCartDTO confirmPurchaseCartDTO = new ConfirmPurchaseCartDTO();
            confirmPurchaseCartDTO.setPurchaseDrugs(purchaseDrugs);
            confirmPurchaseCartDTO.setTransaction(transaction);
            return confirmPurchaseCartDTO;
        }
    }

    /*private PurchaseCartDrugDTO createPurchaseCartDrugDTO(Jedis jedis, String key, int idPurchDrug) {
        PurchaseCartDrugDTO purchaseDrug = new PurchaseCartDrugDTO();
        purchaseDrug.setIdPurchDrug(idPurchDrug);
        purchaseDrug.setIdDrug(jedis.get(key + "id"));
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        purchaseDrug.setName(jsonObject.get("name").getAsString());
        purchaseDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        if(Objects.equals(timestampString, "")){
            purchaseDrug.setPrescriptionTimestamp(null);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
            purchaseDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        }
        purchaseDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        return purchaseDrug;
    }*/

    private PurchaseCartDrugDTO createPurchaseCartDrugDTO(Jedis jedis, String key, int id_purch_drug){
        PurchaseCartDrugDTO purchaseDrug = new PurchaseCartDrugDTO();
        purchaseDrug.setIdPurchDrug(id_purch_drug);
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        purchaseDrug.setIdDrug(jedis.get(key + "id"));
        purchaseDrug.setName(jsonObject.get("name").getAsString());
        purchaseDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        if(Objects.equals(timestampString, "")){
            purchaseDrug.setPrescriptionTimestamp(null);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
            purchaseDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        }
        purchaseDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        return purchaseDrug;
    }

    private PurchaseCartDrugDTO createPurchaseCartDrugDTO(Jedis jedis, int quantity, String key, int id_purch_drug){
        PurchaseCartDrugDTO purchaseDrug = new PurchaseCartDrugDTO();
        purchaseDrug.setIdPurchDrug(id_purch_drug);
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        purchaseDrug.setIdDrug(jedis.get(key + "id"));
        purchaseDrug.setName(jsonObject.get("name").getAsString());
        purchaseDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        if(Objects.equals(timestampString, "")){
            purchaseDrug.setPrescriptionTimestamp(null);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
            purchaseDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        }
        purchaseDrug.setQuantity(quantity);
        return purchaseDrug;
    }
}
