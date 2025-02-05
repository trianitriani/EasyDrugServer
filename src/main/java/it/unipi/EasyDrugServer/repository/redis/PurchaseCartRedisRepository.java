package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.ConfirmPurchaseCartDTO;
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
        purch-drug:purch-drug_id:id_pat:id
        purch-drug:purch-drug_id:id_pat:info { name, price, prescriptionTimestamp }
        purch-drug:purch-drug_id:id_pat:quantity
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
        // se non deriva da una prescrizione controlliamo
        // che non si trovi di già all'interno del carrello
        if(drug.getPrescriptionTimestamp() == null){
            for(int i=1; i<=redisHelper.nEntities(jedis, this.entity); i++){
                String keyPurch = this.entity + ":" + i + ":" + patientCode + ":";
                if(jedis.exists(keyPurch + "id") && Objects.equals(jedis.get(keyPurch + "id"), String.valueOf(drug.getId())))
                    throw new ForbiddenException("Drug "+drug.getId()+" is already into the purchase cart");
            }
        }

        JsonObject info = new JsonObject();
        info.addProperty("name", drug.getName());
        info.addProperty("price", drug.getPrice());
        if(drug.getPrescriptionTimestamp() == null){
            info.addProperty("prescriptionTimestamp", "");
        } else info.addProperty("prescriptionTimestamp", String.valueOf(drug.getPrescriptionTimestamp()));

        // Now we have to search a valid id_purch for a new element
        String id_purch = redisHelper.getReusableId(jedis, this.entity);
        String key = this.entity + ":" + id_purch + ":" + patientCode + ":";

        // insert in atomic way a drug into a purchase cart
        Transaction transaction = jedis.multi();
        transaction.set(key + "id", String.valueOf(drug.getId()));
        transaction.set(key + "info", String.valueOf(info));
        transaction.set(key + "quantity", String.valueOf(drug.getQuantity()));

        // expire of one hour for delete an object into purchase cart
        transaction.expire(key + "id", this.hour);
        transaction.expire(key + "info", this.hour);
        transaction.expire(key + "quantity", this.hour);
        List<Object> result = transaction.exec();
        if(result == null)
            throw new JedisException("Error in the transaction");

        return drug;
    }

    public PurchaseDrugDTO modifyPurchaseDrugQuantity(String patientCode, int idDrug, int quantity) {
        // searching drug with idDrug
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

    public PurchaseDrugDTO deletePurchaseDrug(String patientCode, int idDrug, String prescriptionTimestamp) {
        // searching into purchase cart the specific drug
        for(int i=0; i<=redisHelper.nEntities(jedis, this.entity); i++) {
            String key = this.entity + ":" + i + ":" + patientCode + ":";
            if (jedis.exists(key + "id")) {
                if (idDrug != Integer.parseInt(jedis.get(key + "id"))) continue;

                // se il farmaco faceva parte di una prescrizione, devo trovare il farmaco con quel timestamp
                // per evitare di eliminare uno stesso farmaco in una prescrizione diversa
                if(!Objects.equals(prescriptionTimestamp, "")){
                    JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
                    String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();

                    if(!Objects.equals(prescriptionTimestamp, timestampString)) continue;
                }

                PurchaseDrugDTO drug = createPurchaseDrugDTO(idDrug, key);

                // delete the specific drug in atomic way
                Transaction transaction = jedis.multi();
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
        throw new NotFoundException("Impossibile to delete the drug: patient "+patientCode+" " +
                "has no drug with id "+idDrug+" in the cart.");
    }

    public ConfirmPurchaseCartDTO confirmPurchaseCart(String patientCode){
        HashMap<String, List<Integer>> prescribedDrugs = new HashMap<>();
        List<PurchaseDrugDTO> purchaseDrugs = new ArrayList<>();
        HashMap<String, Integer> purchToDelete = new HashMap<>();
        HashMap<String, Integer> presDrugToDelete = new HashMap<>();
        List<String> presDrugPurchased = new ArrayList<>();
        HashMap<String, Integer> presToDelete = new HashMap<>();
        HashMap<String, Integer> presToModify = new HashMap<>();
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
                purchToDelete.put(keyPurch, i);
            }
        }
        // adesso per ogni prescrizione che contiene farmaci acquistati vado a modificare il db
        // segnando quel farmaco come acquistato e controllando se una prescrizione è conclusa.
        for(int j=1; j<=redisHelper.nEntities(jedis, "pres"); j++){
            String presKey = "pres:" + j + ":" + patientCode + ":";
            if(jedis.exists(presKey + "timestamp")){
                String stringTimestamp = jedis.get(presKey + "timestamp");
                if(Objects.equals(stringTimestamp, "")) continue;
                // allora la prescrizione è attiva
                if(!prescribedDrugs.containsKey(stringTimestamp)) continue;
                // allora sono stati acquistati farmaci di quella prescrizione
                List<Integer> drugs = prescribedDrugs.get(stringTimestamp);
                int nPurchased = drugs.size();
                boolean ended = false;
                int toPurchase = Integer.parseInt(jedis.get(presKey + "toPurchase"));
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
                                presDrugToDelete.put(presDrugKey, k);
                            } else {
                                presDrugPurchased.add(presDrugKey);
                            }
                        }
                    }
                }
                if(ended)
                    presToDelete.put(presKey, j);
                else {
                    toPurchase -= nPurchased;
                    presToModify.put(presKey, toPurchase);
                }
            }
        }
        if(purchaseDrugs.isEmpty())
            throw new ForbiddenException("You can not complete the payment if a cart is empty");

        // *************************************************** //

        // effettuiamo le modifiche nel db
        Transaction transaction = jedis.multi();

        // delete purchase drug into the cart
        for(Map.Entry<String, Integer> entry : purchToDelete.entrySet()){
            transaction.del(entry.getKey() + "id");
            transaction.del(entry.getKey() + "info");
            transaction.del(entry.getKey() + "quantity");
            redisHelper.returnIdToPool(transaction, this.entity, String.valueOf(entry.getValue()));
        }

        // delete purchase drug into prescriptions that are sold
        for(Map.Entry<String, Integer> entry : presDrugToDelete.entrySet()){
            transaction.del(entry.getKey() + "id");
            transaction.del(entry.getKey() + "info");
            transaction.del(entry.getKey() + "quantity");
            transaction.del(entry.getKey() + "purchased");
            redisHelper.returnIdToPool(transaction, "pres-drug", Integer.toString(entry.getValue()));
        }

        // setting purchased to a drug into a prescription that is sold
        for(String presDrugKey : presDrugPurchased){
            transaction.set(presDrugKey + "purchased", "true");
        }

        // delete prescription if is terminated (all drugs are sold)
        for(Map.Entry<String, Integer> entry : presToDelete.entrySet()){
            transaction.del(entry.getKey() + "timestamp");
            transaction.del(entry.getKey() + "toPurchase");
            redisHelper.returnIdToPool(transaction, "pres", Integer.toString(entry.getValue()));
        }

        for(Map.Entry<String, Integer> entry : presToModify.entrySet()){
            transaction.set(entry.getKey() + "toPurchase", String.valueOf(entry.getValue()));
        }

        /*
        List<Object> result = transaction.exec();
        if(result == null)
            throw new JedisException("Error in the transaction");

         */

        ConfirmPurchaseCartDTO confirmPurchaseCartDTO = new ConfirmPurchaseCartDTO();
        confirmPurchaseCartDTO.setPurchaseDrugs(purchaseDrugs);
        confirmPurchaseCartDTO.setTransaction(transaction);
        return confirmPurchaseCartDTO;
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(String key) {
        PurchaseDrugDTO purchaseDrug = new PurchaseDrugDTO();
        purchaseDrug.setId(Integer.parseInt(jedis.get(key + "id")));
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
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(int id, String key){
        PurchaseDrugDTO purchasedDrug = new PurchaseDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        purchasedDrug.setId(id);
        purchasedDrug.setName(jsonObject.get("name").getAsString());
        purchasedDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        if(Objects.equals(timestampString, "")){
            purchasedDrug.setPrescriptionTimestamp(null);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
            purchasedDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        }
        purchasedDrug.setQuantity(Integer.parseInt(jedis.get(key + "quantity")));
        return purchasedDrug;
    }

    private PurchaseDrugDTO createPurchaseDrugDTO(int id, int quantity, String key){
        PurchaseDrugDTO purchasedDrug = new PurchaseDrugDTO();
        JsonObject jsonObject = JsonParser.parseString(jedis.get(key + "info")).getAsJsonObject();
        purchasedDrug.setId(id);
        purchasedDrug.setName(jsonObject.get("name").getAsString());
        purchasedDrug.setPrice(jsonObject.get("price").getAsDouble());
        String timestampString = jsonObject.get("prescriptionTimestamp").getAsString();
        if(Objects.equals(timestampString, "")){
            purchasedDrug.setPrescriptionTimestamp(null);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime prescriptionTimestamp = LocalDateTime.parse(timestampString, formatter);
            purchasedDrug.setPrescriptionTimestamp(prescriptionTimestamp);
        }
        purchasedDrug.setQuantity(quantity);
        return purchasedDrug;
    }
}
