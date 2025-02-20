package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.Gson;
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
        purch-drug:purch-drug_id:id_pat:info {name, price, prescriptionTimestamp, quantity}
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

            if(!jedis.exists(listKey))
                // in realtà significa che non esiste ancora un carrello degli acquisti
                // valutare se ritornare qualcosa di vuoto, mi sa che bisogna fa questo
                throw new NotFoundException("The patient with id: " + id_pat + " does not exist");

            // Ottenere SOLO i purch_id relativi a quel paziente
            String list = jedis.get(listKey);
            Gson gson = new Gson();
            int[] purchIds = gson.fromJson(list, int[].class);

            // Ottenere tutte le chiavi per fare poi una sola MGET
            List<String> keys = new ArrayList<>();
            for(int id_purch: purchIds){
                String key = this.entity + ":" + id_purch + ":" + id_pat + ":";
                keys.add(key + "id");
                keys.add(key + "info");
            }

            // Effettuiamo una sola chiamata a MGET per tutti i valori
            List<String> values = jedis.mget(keys.toArray(new String[0]));
            for (int i = 0; i < purchIds.length; i++) {
                int id_purch = purchIds[i];
                String idDrug = values.get(i * 2);              // "id" si trova nella posizione i * 2
                String infoJson = values.get(i * 2 + 1);        // "info" si trova nella posizione i * 2 + 1
                PurchaseCartDrugDTO drug = createPurchaseCartDrugDTO(idDrug, infoJson, id_purch);
                cartList.add(drug);
            }

            return cartList;
        }
    }

    /*
    public List<PurchaseCartDrugDTO> getPurchaseCart(String id_pat) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            List<PurchaseCartDrugDTO> cartList = new ArrayList<>();
            String matchPattern = this.entity + ":*:" + id_pat + ":id";
            String cursor = "0";
            do {
                ScanResult<String> scanResult = jedis.scan(cursor, new ScanParams().match(matchPattern).count(10000));
                cursor = scanResult.getCursor();
                for (String key : scanResult.getResult()) {
                    String baseKey = key.substring(0, key.lastIndexOf(":")); // Rimuove il ":id"
                    int id_purch = extractIndexFromKey(baseKey);
                    PurchaseCartDrugDTO drug = createPurchaseCartDrugDTO(jedis, baseKey, id_purch);
                    cartList.add(drug);
                }
            } while (!cursor.equals("0"));

            return cartList;
        }
    }


    private int extractIndexFromKey(String key) {
        String[] parts = key.split(":");
        return Integer.parseInt(parts[1]); // Assume che l'indice sia nella seconda posizione
    }*/

    public PurchaseCartDrugDTO insertPurchaseDrug(String id_pat, PurchaseCartDrugDTO drug) {
        try(Jedis jedis = jedisSentinelPool.getResource()) {
            // se esiste già quel farmaco, dobbiamo lanciare un'eccezione
            String listKey = this.entity + ":" + id_pat + ":set";
            String list = jedis.get(listKey);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Integer>>(){}.getType();
            List<Integer> purchIds = gson.fromJson(list, listType);
            for(int old_id_purch: purchIds){
                String purchKey = this.entity + ":" + old_id_purch + ":" + id_pat + ":";
                String drugId = jedis.get(purchKey + "id");
                if(drug.getIdDrug().equals(drugId))
                    throw new ForbiddenException("Drug " + drug.getIdDrug() + " is already into the purchase cart");
            }

            // se non si trova già all'interno bisogna inserirlo
            JsonObject info = new JsonObject();
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());
            info.addProperty("quantity", drug.getQuantity());
            if (drug.getPrescriptionTimestamp() == null) {
                info.addProperty("prescriptionTimestamp", "");
            } else info.addProperty("prescriptionTimestamp", String.valueOf(drug.getPrescriptionTimestamp()));

            // now we have to search a valid id_purch for a new element
            String new_id_purch = redisHelper.getReusableId(jedis, this.entity);
            drug.setIdPurchDrug(Integer.parseInt(new_id_purch));

            String key = this.entity + ":" + new_id_purch + ":" + id_pat + ":";
            // insert a drug into a purchase cart
            jedis.set(key + "id", String.valueOf(drug.getIdDrug()));
            jedis.set(key + "info", String.valueOf(info));
            // inserire nella lista solo dopo che gli altri campi sono stati compilati
            // garantire che
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

    // da capire !
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

    /*
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
    */

    private PurchaseCartDrugDTO createPurchaseCartDrugDTO(String id_drug, String info_json, int id_purch_drug) {
        PurchaseCartDrugDTO purchaseDrug = new PurchaseCartDrugDTO();
        purchaseDrug.setIdPurchDrug(id_purch_drug);
        purchaseDrug.setIdDrug(id_drug);

        JsonObject jsonObject = JsonParser.parseString(info_json).getAsJsonObject();
        purchaseDrug.setName(jsonObject.get("name").getAsString());
        purchaseDrug.setPrice(jsonObject.get("price").getAsDouble());

        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        purchaseDrug.setPrescriptionTimestamp(
                (timestampString.isEmpty()) ? null : LocalDateTime.parse(timestampString, DateTimeFormatter.ISO_DATE_TIME)
        );

        // forse bisogna fare lo stesso controllo della data (?)
        String quantityStr = String.valueOf(jsonObject.get("quantity"));
        purchaseDrug.setQuantity(quantityStr != null ? Integer.parseInt(quantityStr) : 0);

        return purchaseDrug;
    }

    /*private PurchaseCartDrugDTO createPurchaseCartDrugDTO(Jedis jedis, String key, int id_purch_drug) {
        PurchaseCartDrugDTO purchaseDrug = new PurchaseCartDrugDTO();
        purchaseDrug.setIdPurchDrug(id_purch_drug);

        // Recupero più valori in un'unica chiamata con MGET
        List<String> values = jedis.mget(key + "id", key + "info");
        String idDrug = values.get(0);
        String infoJson = values.get(1);
        if (infoJson == null)  return null;

        JsonObject jsonObject = JsonParser.parseString(infoJson).getAsJsonObject();
        purchaseDrug.setIdDrug(idDrug);
        purchaseDrug.setName(jsonObject.get("name").getAsString());
        purchaseDrug.setPrice(jsonObject.get("price").getAsDouble());
        String quantityStr = String.valueOf(jsonObject.get("quantity"));

        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        purchaseDrug.setPrescriptionTimestamp(
                (timestampString.isEmpty()) ? null : LocalDateTime.parse(timestampString, DateTimeFormatter.ISO_DATE_TIME)
        );

        // forse va gestito come il timestamp sopra (?)
        purchaseDrug.setQuantity(quantityStr != null ? Integer.parseInt(quantityStr) : 0);
        return purchaseDrug;
    }*/

    /*private PurchaseCartDrugDTO createPurchaseCartDrugDTO(Jedis jedis, int quantity, String key, int id_purch_drug){
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
    }*/

    private PurchaseCartDrugDTO createPurchaseCartDrugDTO(Jedis jedis, int quantity, String key, int id_purch_drug) {
        PurchaseCartDrugDTO purchaseDrug = new PurchaseCartDrugDTO();
        purchaseDrug.setIdPurchDrug(id_purch_drug);

        // Recupero più valori in un'unica chiamata con MGET
        List<String> values = jedis.mget(key + "id", key + "info");
        String idDrug = values.get(0);
        String infoJson = values.get(1);
        if (infoJson == null)  return null;

        JsonObject jsonObject = JsonParser.parseString(infoJson).getAsJsonObject();
        purchaseDrug.setIdDrug(idDrug);
        purchaseDrug.setName(jsonObject.get("name").getAsString());
        purchaseDrug.setPrice(jsonObject.get("price").getAsDouble());
        purchaseDrug.setQuantity(quantity);

        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        purchaseDrug.setPrescriptionTimestamp(
                (timestampString.isEmpty()) ? null : LocalDateTime.parse(timestampString, DateTimeFormatter.ISO_DATE_TIME)
        );

        return purchaseDrug;
    }
}
