package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.models.security.SecurityScheme;
import it.unipi.EasyDrugServer.dto.ConfirmPurchaseCartDTO;
import it.unipi.EasyDrugServer.dto.PurchaseCartDrugDTO;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
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
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

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
    private final int hour = 3600*24*3;     // MODIFICARE

    /*

        purch-drug:purch-drug_id:id_pat:id
        purch-drug:purch-drug_id:id_pat:info { name, price, quantity, id_pres, id_pres_drug }
        purch-drug:id_pat:set


        OPERAZIONI:
        1) Inserire nuovo farmaco nel carrello
        2) Controllare che sia già nel carrello
        3) Recuperare tutti i farmaci nel carrello
        4) modificare quantità
        5) eliminare farmaco dal carrello


     */

    @Autowired
    public PurchaseCartRedisRepository(JedisSentinelPool jedisSentinelPool, RedisHelper redisHelper) {
        this.jedisSentinelPool = jedisSentinelPool;
        this.redisHelper = redisHelper;
    }

    public List<PurchaseCartDrugDTO> getPurchaseCart(String id_pat) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            List<PurchaseCartDrugDTO> cartList = new ArrayList<>();
            String listKey = this.entity + ":" + id_pat + ":set";
            List<String> purchIds = new ArrayList<>(jedis.smembers(listKey));

            if(purchIds.isEmpty())
                return cartList;

            // Ottenere tutte le chiavi per fare poi una sola MGET
            List<String> keys = new ArrayList<>();
            for(String id_purch: purchIds){
                String key = this.entity + ":" + id_purch + ":" + id_pat + ":";
                keys.add(key + "id");
                keys.add(key + "info");
            }

            // Effettuiamo una sola chiamata a MGET per tutti i valori
            List<String> values = jedis.mget(keys.toArray(new String[0]));
            for (int i = 0; i < purchIds.size(); i++) {
                int id_purch = Integer.parseInt(purchIds.get(i));
                String idDrug = values.get(i * 2);                  // "id" si trova nella posizione i * 2
                String infoJson = values.get(i * 2 + 1);            // "info" si trova nella posizione i * 2 + 1
                PurchaseCartDrugDTO drug = createPurchaseCartDrugDTO(idDrug, infoJson, id_purch);
                cartList.add(drug);
            }
            return cartList;
        }
    }

    public PurchaseCartDrugDTO insertPurchaseDrug(String id_pat, PurchaseCartDrugDTO drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            String listKey = this.entity + ":" + id_pat + ":set";
            // se esiste già quel farmaco, dobbiamo lanciare un'eccezione
            Set<String> purchIds = jedis.smembers(listKey);
            System.out.println("PURCHIDS: " + purchIds);
            for (String id_purch : purchIds) {
                String purchKey = this.entity + ":" + id_purch + ":" + id_pat + ":";
                if(drug.getIdPres() == null){
                    String drugId = jedis.get(purchKey + "id");
                    if (drug.getIdDrug().equals(drugId))
                        throw new ForbiddenException("Drug " + drug.getIdDrug() + " is already into the purchase cart");
                } else {
                    String info = jedis.get(purchKey + "info");
                    System.out.println("INFO: " + info);
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    if(!jsonObject.has("idPresDrug"))
                        continue;
                    int idPurchDrug = jsonObject.getAsJsonPrimitive("idPresDrug").getAsInt();
                    if(idPurchDrug == drug.getIdPurchDrug())
                        throw new ForbiddenException("Drug " + drug.getIdDrug() + " of the same prescription is already into the purchase cart");
                }
            }

            // se non si trova già all'interno bisogna inserirlo
            JsonObject info = new JsonObject();
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());
            info.addProperty("quantity", drug.getQuantity());
            // in questo modo aggiungiamo una ridondanza utile per la conferma dell'acquisto
            if (drug.getIdPresDrug() != null) {
                info.addProperty("idPres", String.valueOf(drug.getIdPres()));
                info.addProperty("idPresDrug", String.valueOf(drug.getIdPresDrug()));
            }

            // now we have to search a valid id_purch for a new element
            String new_id_purch = redisHelper.getReusableId(jedis, this.entity);
            drug.setIdPurchDrug(Integer.parseInt(new_id_purch));

            String key = this.entity + ":" + new_id_purch + ":" + id_pat + ":";
            // insert a drug into a purchase cart
            jedis.set(key + "id", String.valueOf(drug.getIdDrug()));
            jedis.set(key + "info", String.valueOf(info));
            // inserire nella lista solo dopo che gli altri campi sono stati compilati
            jedis.sadd(listKey, new_id_purch);

            // expire of one hour for delete an object into purchase cart
            jedis.expire(key + "id", this.hour);
            jedis.expire(key + "info", this.hour);
            jedis.expire(listKey, this.hour);
            return drug;
        }
    }

    public PurchaseCartDrugDTO modifyPurchaseDrugQuantity(String id_pat, int id_purch_drug, int quantity) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            // searching drug with idDrug
            String key = this.entity + ":" + id_purch_drug + ":" + id_pat + ":";
            String id_drug = jedis.get(key + "id");
            if (id_drug != null) {
                // modifica il campo quantità
                String json_info = jedis.get(key + "info");
                JsonObject jsonObject = JsonParser.parseString(json_info).getAsJsonObject();
                jsonObject.addProperty("quantity", quantity);
                json_info = jsonObject.toString();
                jedis.set(key + "info", json_info);
                return createPurchaseCartDrugDTO(id_drug, json_info, id_purch_drug);
            }
            throw new NotFoundException("Impossibile to modify the drug: patient " + id_pat + " has no drug with purch_id" + id_purch_drug + " in the cart.");
        }
    }

    public PurchaseCartDrugDTO deletePurchaseDrug(String id_pat, int id_purch_drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()){
            // searching into purchase cart the specific drug
            String key = this.entity + ":" + id_purch_drug + ":" + id_pat + ":";
            String listKey = this.entity + ":" + id_pat + ":set";
            String id_drug = jedis.get(key + "id");
            if (id_drug != null) {
                String json_info = jedis.get(key + "info");
                // va tolta prima dalla lista
                jedis.srem(listKey, String.valueOf(id_purch_drug));
                // eliminiamo le informazioni dalla lista
                jedis.del(key + "id");
                jedis.del(key + "info");
                redisHelper.returnIdToPool(jedis, this.entity, String.valueOf(id_purch_drug));
                return createPurchaseCartDrugDTO(id_drug, json_info, id_purch_drug);
            }
            throw new NotFoundException("Impossibile to delete the drug: patient "+id_pat+" " + "has no purch drug with id "+ id_purch_drug +" in the cart.");
        }
    }

    public ConfirmPurchaseCartDTO confirmPurchaseCart(String id_pat){
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            List<PurchaseCartDrugDTO> purchaseDrugs = new ArrayList<>();
            // <id_pres, set of id_pres_drugs>
            LinkedHashMap<Integer, List<Integer>> prescribedDrugs = new LinkedHashMap<>();
            LinkedHashMap<Integer, List<Integer>> presToDelete = new LinkedHashMap<>();
            LinkedHashMap<Integer, List<Integer>> presToModify = new LinkedHashMap<>();
            LinkedHashMap<Integer, Integer> newToPurchase = new LinkedHashMap<>();

            String listKey = this.entity + ":" + id_pat + ":set";
            List<String> purchIds = new ArrayList<>(jedis.smembers(listKey));
            List<String> keys = new ArrayList<>();
            for(String id_purch_drug: purchIds){
                keys.add(this.entity + ":" + id_purch_drug + ":" + id_pat + ":id");
                keys.add(this.entity + ":" + id_purch_drug + ":" + id_pat + ":info");
            }

            // Effettuiamo una sola chiamata a MGET per tutti i valori
            List<String> values = jedis.mget(keys.toArray(new String[0]));
            for (int i = 0; i < purchIds.size(); i++) {
                int id_purch = Integer.parseInt(purchIds.get(i));
                String idDrug = values.get(i * 2);
                String infoJson = values.get(i * 2 + 1);
                PurchaseCartDrugDTO drug = createPurchaseCartDrugDTO(idDrug, infoJson, id_purch);
                if(drug.getIdPres() != null){
                    String presDate = "pres:" + drug.getIdPres() + ":" + id_pat + ":timestamp";
                    drug.setPrescriptionTimestamp(LocalDateTime.parse(jedis.get(presDate)));
                    // allora è relativo a una prescrizione
                    if(!prescribedDrugs.containsKey(drug.getIdPres()))
                        prescribedDrugs.put(drug.getIdPres(), new ArrayList<>());
                    prescribedDrugs.get(drug.getIdPres()).add(drug.getIdPresDrug());
                }
                purchaseDrugs.add(drug);
            }
            if (purchaseDrugs.isEmpty())
                throw new ForbiddenException("You can not complete the payment if a cart is empty");

            // controllare se le prescrizioni sono da concludere o meno e selezionarle
            // leggendo dall'entrata toPurchase per ogni prescrizione
            for(Map.Entry<Integer, List<Integer>> entry : prescribedDrugs.entrySet()){
                // leggere il numero dei farmaci prescritti ancora da comprare.
                String keyPres = "pres:" + entry.getKey() + ":" + id_pat + ":toPurchase";
                int toPurchase = Integer.parseInt(jedis.get(keyPres));
                if(toPurchase == entry.getValue().size())
                    presToDelete.put(entry.getKey(), entry.getValue());
                else{
                    newToPurchase.put(entry.getKey(), toPurchase - entry.getValue().size());
                    presToModify.put(entry.getKey(), entry.getValue());
                }
            }

            // *************************************************** //

            // effettuiamo le modifiche nel db
            Transaction transaction = jedis.multi();

            // delete purchase drug into the cart
            for (PurchaseCartDrugDTO drug : purchaseDrugs) {
                String key = this.entity + ":" + drug.getIdPurchDrug() + ":" + id_pat + ":";
                transaction.del(key + "id");
                transaction.del(key + "info");
                redisHelper.returnIdToPool(transaction, this.entity, String.valueOf(drug.getIdPurchDrug()));
            }

            // delete drugs into prescriptions that are finished and the prescription
            for (Map.Entry<Integer, List<Integer>> entry : presToDelete.entrySet()) {
                String keyPres = "pres:" + entry.getKey() + ":" + id_pat + ":";
                String keyPresList = "pres:" + id_pat + ":set";
                transaction.del(keyPres + "timestamp");
                transaction.del(keyPres + "toPurchase");
                // rimozione dalla lista della prescrizione
                transaction.srem(keyPresList, String.valueOf(entry.getKey()));
                redisHelper.returnIdToPool(transaction, "pres", Integer.toString(entry.getKey()));

                for(Integer id_pres_drug : entry.getValue()){
                    String keyPresDrug = "pres-drug:" + id_pres_drug + ":" + entry.getKey() + ":";
                    String keyPresDrugList = "pres-drug:" + entry.getKey() + ":set";
                    transaction.del(keyPresDrug + "id");
                    transaction.del(keyPresDrug + "info");
                    transaction.del(keyPresDrug + "purchased");
                    // rimozione della lista della prescrizione
                    transaction.del(keyPresDrugList);
                    redisHelper.returnIdToPool(transaction, "pres-drug", String.valueOf(id_pres_drug));
                }
            }

            // update the number of drug to purchase on unfinished prescriptions and set purchased to
            // selected drugs
            for (Map.Entry<Integer, List<Integer>> entry : presToModify.entrySet()) {
                String keyPres = "pres:" + entry.getKey() + ":" + id_pat + ":";
                transaction.set(keyPres + "toPurchase", String.valueOf(newToPurchase.get(entry.getKey())));
                for(Integer id_pres_drug : entry.getValue()){
                    String keyPresDrug = "pres-drug:" + id_pres_drug + ":" + entry.getKey() + ":";
                    transaction.set(keyPresDrug + "purchased", "true");
                }
            }

            ConfirmPurchaseCartDTO confirmPurchaseCartDTO = new ConfirmPurchaseCartDTO();
            confirmPurchaseCartDTO.setPurchaseDrugs(purchaseDrugs);
            confirmPurchaseCartDTO.setTransaction(transaction);
            return confirmPurchaseCartDTO;
        }
    }

    private PurchaseCartDrugDTO createPurchaseCartDrugDTO(String id_drug, String info_json, int id_purch_drug) {
        PurchaseCartDrugDTO purchaseDrug = new PurchaseCartDrugDTO();
        purchaseDrug.setIdPurchDrug(id_purch_drug);
        purchaseDrug.setIdDrug(id_drug);

        JsonObject jsonObject = JsonParser.parseString(info_json).getAsJsonObject();
        purchaseDrug.setName(jsonObject.get("name").getAsString());
        purchaseDrug.setPrice(jsonObject.get("price").getAsDouble());
        purchaseDrug.setQuantity(jsonObject.get("quantity").getAsInt());

        if(jsonObject.get("idPres") != null)
            purchaseDrug.setIdPres(jsonObject.get("idPres").getAsInt());
        if(jsonObject.get("idPresDrug") != null)
            purchaseDrug.setIdPresDrug(jsonObject.get("idPresDrug").getAsInt());

        return purchaseDrug;
    }

}
