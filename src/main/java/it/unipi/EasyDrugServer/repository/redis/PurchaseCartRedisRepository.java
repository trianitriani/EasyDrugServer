package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.*;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Setter
@Getter
@Repository
public class PurchaseCartRedisRepository {
    private final String entity = "purch-drug";
    private final JedisCluster jedisCluster;
    private final RedisHelper redisHelper;

    /*
        purch-drug:purch-drug:id_pat:id
        purch-drug:purch-drug:id_pat:info { name, price, prescriptionTimestamp }
        purch-drug:purch-drug:id_pat:quantity
     */

    public PurchaseCartRedisRepository(JedisCluster jedisCluster, RedisHelper redisHelper) {
        this.jedisCluster = jedisCluster;
        this.redisHelper = redisHelper;
    }

    /**
     * ## PHARMACIST ##
     * Insert into redis db information related to a specific drug that is insert into a cart
     * of a specific patient by a pharmacist
     * @param patientCode code of patient
     * @param drug drug insert into a cart
     */
    public PurchaseDrugDTO savePurchaseDrug(String patientCode, PurchaseDrugDTO drug) {
        try {
            JsonObject info = new JsonObject();
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());
            info.addProperty("prescriptionTimestamp", String.valueOf(drug.getPrescriptionTimestamp()));

            int quantity = drug.getQuantity();
            // Now we have to search a valid id_purch for a new element
            String id_purch = redisHelper.getReusableId(jedisCluster, this.entity);
            String key = this.entity + ":" + id_purch + ":" + patientCode + ":";
            jedisCluster.set(key + "id", String.valueOf(drug.getId()));
            jedisCluster.set(key + "info", String.valueOf(info));
            jedisCluster.set(key + "quantity", String.valueOf(quantity));
            return drug;
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Connection error with redis: " + e.getMessage(), e);
        } catch (JedisDataException e) {
            throw new RuntimeException("Data error with redis: " + e.getMessage(), e);
        } catch (JedisException e) {
            throw new RuntimeException("Generic error with redis: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Generic server error: " + e.getMessage(), e);
        }
    }

    /**
     * ## PHARMACIST ##
     * @param patientCode
     * @return
     */
    public List<PurchaseDrugDTO> getPurchaseCart(String patientCode){
        try {
            List<PurchaseDrugDTO> cartList = new ArrayList<>();
            for(int i=0; i<=redisHelper.nEntities(jedisCluster, this.entity); i++){
                String info;
                String quantity;
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if(!jedisCluster.exists(key + "id")) continue;
                // Se sono qui significa che l'oggetto esiste realmente e lo inserisco nella lista
                PurchaseDrugDTO drug = createPurchaseDrugDTO(key);
                cartList.add(drug);
            }
            return cartList;
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Connection error with redis: " + e.getMessage(), e);
        } catch (JedisDataException e) {
            throw new RuntimeException("Data error with redis: " + e.getMessage(), e);
        } catch (JedisException e) {
            throw new RuntimeException("Generic error with redis: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Generic server error: " + e.getMessage(), e);
        }
    }

    /**
     * ## PHARMACIST ##
     * @param patientCode
     * @return
     */
    public List<PurchaseDrugDTO> confirmPurchaseCart(String patientCode){
        try {
            HashMap<String, List<Integer>> prescribedDrugs = new HashMap<>();
            List<PurchaseDrugDTO> purchaseDrugs = new ArrayList<>();
            // liste di chiavi che comporteranno una modifica al db
            List<String> purchToDelete = new ArrayList<>();
            List<String> presDrugToDelete = new ArrayList<>();
            List<String> presDrugPurchased = new ArrayList<>();
            List<String> presToDelete = new ArrayList<>();
            HashMap<String, Integer> presToModify = new HashMap<>();
            // cerco tutti i farmaci che sono nel carrello e si riferiscono al paziente selezionato
            for(int i=1; i<=redisHelper.nEntities(jedisCluster, this.entity); i++){
                String keyPurch = this.entity + ":" + i + ":" + patientCode + ":";
                if(jedisCluster.exists(keyPurch + "id")){
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
                    purchToDelete.add(keyPurch);
                    jedisCluster.del(keyPurch + "id");
                    jedisCluster.del(keyPurch + "info");
                    jedisCluster.del(keyPurch + "quantity");
                    redisHelper.returnIdToPool(jedisCluster, this.entity, String.valueOf(i));
                }
            }
            // adesso per ogni prescrizione che contiene farmaci acquistati vado a modificare il db
            // segnando quel farmaco come acquistato e controllando se una prescrizione è conclusa.
            for(int j=1; j<=redisHelper.nEntities(jedisCluster, "pres"); j++){
                String presKey = "pres:" + j + ":" + patientCode + ":";
                if(jedisCluster.exists(presKey + "timestamp")){
                    String stringTimestamp = jedisCluster.get(presKey + "timestamp");
                    if(!Objects.equals(stringTimestamp, "false")) continue;
                    // allora la prescrizione è attiva
                    if(!prescribedDrugs.containsKey(stringTimestamp)) continue;
                    // allora sono stati acquistati farmaci di quella prescrizione
                    List<Integer> drugs = prescribedDrugs.get(stringTimestamp);
                    int nPurchased = drugs.size();
                    boolean ended = false;
                    int toPurchase = Integer.parseInt(jedisCluster.get(presKey+" toPurchase"));
                    if(nPurchased == toPurchase){
                        // allora significa che tutti i farmaci della prescrizione sono stati acquistati
                        ended = true;
                    }
                    // ciclo tutti i farmaci
                    for(int k=1; k<=redisHelper.nEntities(jedisCluster, "pres-drug"); k++){
                        String presDrugKey = "pres-drug:" + k + ":" + j + ":";
                        if(jedisCluster.exists(presDrugKey + "id")){
                            int id = Integer.parseInt(jedisCluster.get((presDrugKey + "id")));
                            if(prescribedDrugs.get(stringTimestamp).contains(id)){
                                if (ended){
                                    // Allora vado a eliminare quel farmaco
                                    presDrugToDelete.add(presDrugKey);
                                    jedisCluster.del(presDrugKey + "id");
                                    jedisCluster.del(presDrugKey + "info");
                                    jedisCluster.del(presDrugKey + "quantity");
                                    jedisCluster.del(presDrugKey + "purchased");
                                    redisHelper.returnIdToPool(jedisCluster, "pres-drug", Integer.toString(k));
                                } else {
                                    presDrugPurchased.add(presDrugKey);
                                    jedisCluster.set(presDrugKey + "purchased", "true");
                                }
                            }
                        }
                    }
                    if(ended) {
                        presToDelete.add(presKey);
                        jedisCluster.del(presKey + "timestamp");
                        jedisCluster.del(presKey + "toPurchase");
                        redisHelper.returnIdToPool(jedisCluster, "pres", Integer.toString(j));
                    } else {
                        toPurchase -= nPurchased;
                        presToModify.put(presKey, toPurchase);
                        jedisCluster.set(presKey + "toPurchase", String.valueOf(toPurchase));
                    }
                }
            }
            if(purchaseDrugs.isEmpty())
                throw new ForbiddenException("You can not complete da payment if a cart is empty");

            // effettuo le modifiche usando multi

            for (String purchKey : purchToDelete) {
                // Esegui un'operazione su ciascun elemento
                System.out.println("Eliminazione di: " + purchKey);
            }
            for (String presDrugKey : presDrugToDelete) {
                // Esegui un'operazione su ciascun elemento
                System.out.println("Eliminazione di: " + presDrugKey);
            }
            for (String presDrugKey : presDrugPurchased) {
                // Esegui un'operazione su ciascun elemento
                System.out.println("Contrassegno come acquistato: " + presDrugKey);
                // Ad esempio: jedisCluster.set(purchasedDrugKey, "true");
            }
            for (String presKey : presToDelete) {
                // Esegui un'operazione su ciascun elemento
                System.out.println("Contrassegno come acquistato: " + presKey);
                // Ad esempio: jedisCluster.set(purchasedDrugKey, "true");
            }
            for (Map.Entry<String, Integer> entry : presToModify.entrySet()) {
                String presKey = entry.getKey();
                Integer newValue = entry.getValue();

                // Esegui un'operazione su ciascuna coppia chiave-valore
                System.out.println("Modifica di: " + presKey + " con valore: " + newValue);
                // Ad esempio: jedisCluster.set(presKey, newValue.toString());
            }


            return purchaseDrugs;
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Connection error with redis: " + e.getMessage(), e);
        } catch (JedisDataException e) {
            throw new RuntimeException("Data error with redis: " + e.getMessage(), e);
        } catch (JedisException e) {
            throw new RuntimeException("Generic error with redis: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Generic server error: " + e.getMessage(), e);
        }
    }
    
    public PurchaseDrugDTO modifyPurchaseDrugQuantity(String patientCode, int idDrug, int quantity) {
        try {
            // ricavare il farmaco con id = idDrug
            for(int i=0; i<=redisHelper.nEntities(jedisCluster, this.entity); i++){
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if(jedisCluster.exists(key + "id")){

                    if(idDrug != Integer.parseInt(jedisCluster.get(key + "id"))) continue;
                    // modifica il campo quantità
                    jedisCluster.set(key + "quantity", String.valueOf(quantity));
                    return createPurchaseDrugDTO(idDrug, quantity, key);
                }
            }
            throw new NotFoundException("None drug "+idDrug+" into purchase cart of patient "+patientCode);

        } catch (JedisConnectionException e) {
            throw new RuntimeException("Connection error with redis: " + e.getMessage(), e);
        } catch (JedisDataException e) {
            throw new RuntimeException("Data error with redis: " + e.getMessage(), e);
        } catch (JedisException e) {
            throw new RuntimeException("Generic error with redis: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Generic server error: " + e.getMessage(), e);
        }
    }

    /**
     * ## PHARMACIST ##
     * @param patientCode
     * @param idDrug
     * @return
     */
    public PurchaseDrugDTO deletePurchaseDrug(String patientCode, int idDrug) {
        try {
            for(int i=0; i<=redisHelper.nEntities(jedisCluster, this.entity); i++) {
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if (jedisCluster.exists(key + "info")) {
                    if (idDrug != Integer.parseInt(jedisCluster.get(key + "id"))) continue;

                    // elimino il farmaco
                    jedisCluster.del(key + "id");
                    jedisCluster.del(key + "info");
                    jedisCluster.del(key + "quantity");
                    redisHelper.returnIdToPool(jedisCluster, this.entity, String.valueOf(i));
                    return createPurchaseDrugDTO(idDrug, key);
                }
            }
            throw new NotFoundException("None drug "+idDrug+" into purchase cart of patient "+patientCode);

        } catch (JedisConnectionException e) {
            throw new RuntimeException("Connection error with redis: " + e.getMessage(), e);
        } catch (JedisDataException e) {
            throw new RuntimeException("Data error with redis: " + e.getMessage(), e);
        } catch (JedisException e) {
            throw new RuntimeException("Generic error with redis: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Generic server error: " + e.getMessage(), e);
        }
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(String key) {
        PurchaseDrugDTO prescribedDrug = new PurchaseDrugDTO();
        prescribedDrug.setId(Integer.parseInt(jedisCluster.get(key + "id")));
        JsonObject jsonObject = JsonParser.parseString(jedisCluster.get(key + "info")).getAsJsonObject();
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
        prescribedDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        prescribedDrug.setQuantity(Integer.parseInt(jedisCluster.get(key + "quantity")));
        return prescribedDrug;
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(int id, String key){
        PurchaseDrugDTO prescribedDrug = new PurchaseDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedisCluster.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
        prescribedDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        prescribedDrug.setQuantity(Integer.parseInt(jedisCluster.get(key + "quantity")));
        return prescribedDrug;
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(int id, int quantity, String key){
        PurchaseDrugDTO prescribedDrug = new PurchaseDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedisCluster.get(key + "info")).getAsJsonObject();
        prescribedDrug.setId(id);
        prescribedDrug.setName(jsonObject.get("name").getAsString());
        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
        prescribedDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        prescribedDrug.setQuantity(quantity);
        return prescribedDrug;
    }
}
