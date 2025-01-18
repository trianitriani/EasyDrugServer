package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PurchaseDrugDTO;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.*;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Repository
public class PurchaseCartRedisRepository {
    private final String entity = "purch";
    private final JedisCluster jedisCluster;
    private final RedisHelper redisHelper;

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
    public PurchaseDrugDTO saveDrugIntoPurchaseCart(String patientCode, PurchaseDrugDTO drug) {
        try(jedisCluster){
            JsonObject info = new JsonObject();
            info.addProperty("id", drug.getId());
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());
            info.addProperty("prescriptionTimestamp", String.valueOf(drug.getPrescriptionTimestamp()));

            int quantity = drug.getQuantity();
            // Now we have to search a valid id_purch for a new element
            String id_purch = redisHelper.getReusableId(jedisCluster, "purch");
            String key = this.entity + ":" + id_purch + ":" + patientCode + ":";
            jedisCluster.set(key + "info", String.valueOf(info));
            jedisCluster.set(key + "quantity", String.valueOf(quantity));
            System.out.println(jedisCluster.get(key + "info"));

            PurchaseDrugDTO purchaseDrug = new PurchaseDrugDTO();
            purchaseDrug.setId(drug.getId());
            purchaseDrug.setName(drug.getName());
            purchaseDrug.setPrice(drug.getPrice());
            purchaseDrug.setQuantity(quantity);
            purchaseDrug.setPrescriptionTimestamp(drug.getPrescriptionTimestamp());
            return purchaseDrug;
        }
    }

    /**
     * ## PHARMACIST ##
     * @param patientCode
     * @return
     */
    public List<PurchaseDrugDTO> getPurchaseCart(String patientCode){
        try(jedisCluster){
            List<PurchaseDrugDTO> cartList = new ArrayList<>();
            for(int i=0; i<=redisHelper.nEntities(jedisCluster, this.entity); i++){
                    String info;
                    String quantity;
                    String key = this.entity + ":" + i + ":" + patientCode + ":";
                    if(jedisCluster.exists(key + "info")){
                        info = jedisCluster.get(key + "info");
                        quantity = jedisCluster.get(key + "quantity");
                    } else continue;
                    // Se sono qui significa che l'oggetto esiste realmente e lo inserisco nella lista
                    PurchaseDrugDTO drug = new PurchaseDrugDTO();
                    // inserimento info
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    int id = jsonObject.get("id").getAsInt();
                    String name = jsonObject.get("name").getAsString();
                    double price = jsonObject.get("price").getAsDouble();
                    String prescriptionTimestampString = jsonObject.get("prescriptionTimestamp").getAsString();
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    LocalDateTime prescriptionTimestamp = LocalDateTime.parse(prescriptionTimestampString, formatter);

                    drug.setId(id);
                    drug.setName(name);
                    drug.setPrice(price);
                    drug.setPrescriptionTimestamp(prescriptionTimestamp);
                    drug.setQuantity(Integer.parseInt(quantity));
                    cartList.add(drug);
            }
            return cartList;
        }
    }

    /**
     * ## PHARMACIST ##
     * @param patientCode
     * @return
     */
    public int confirmPurchase(String patientCode){
        try(jedisCluster){
            int confirmed = 0;
            // cerco tutti i farmaci che sono nel carrello e si riferiscono al paziente selezionato
            for(int i=0; i<=redisHelper.nEntities(jedisCluster, this.entity); i++){
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if(jedisCluster.exists(key + "info")){
                    // String info = jedisCluster.get(key + "info");
                    // JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();

                    // elimino dal key value il farmaco nel carrello
                    confirmed++;
                    jedisCluster.del(key + "info");
                    jedisCluster.del(key + "quantity");
                    redisHelper.returnIdToPool(jedisCluster, String.valueOf(i));
                    // metto purchased = true al corrispondente farmaco prescritto (stesso codice e timestamp)

                }
            }

            // se tutti i farmaci della prescrizione hanno purchased = true, allora elimino tutta la prescrizione

            return confirmed;
        }
    }

    public PurchaseDrugDTO modifyPurchaseDrugQuantity(String patientCode, int idDrug, int quantity) {
        try(jedisCluster){
            // ricavare il farmaco con id = idDrug
            for(int i=0; i<=redisHelper.nEntities(jedisCluster, this.entity); i++){
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if(jedisCluster.exists(key + "info")){
                    String info = jedisCluster.get(key + "info");
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    if(idDrug != jsonObject.get("id").getAsInt()) continue;
                    // modifica il campo quantità
                    jedisCluster.set(key + "quantity", String.valueOf(quantity));
                    PurchaseDrugDTO purchaseDrugDTO = new PurchaseDrugDTO();
                    purchaseDrugDTO.setId(jsonObject.get("id").getAsInt());
                    purchaseDrugDTO.setName(jsonObject.get("name").getAsString());
                    purchaseDrugDTO.setPrice(jsonObject.get("price").getAsDouble());

                    String prescriptionTimestampString = jsonObject.get("prescriptionTimestamp").getAsString();
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    LocalDateTime prescriptionTimestamp = LocalDateTime.parse(prescriptionTimestampString, formatter);
                    purchaseDrugDTO.setPrescriptionTimestamp(prescriptionTimestamp);
                    purchaseDrugDTO.setQuantity(quantity);
                    return purchaseDrugDTO;
                }
            }
            return null;
        }
    }

    /**
     * ## PHARMACIST ##
     * @param patientCode
     * @param idDrug
     * @return
     */
    public PurchaseDrugDTO deletePurchaseDrug(String patientCode, int idDrug) {
        try(jedisCluster){
            for(int i=0; i<=redisHelper.nEntities(jedisCluster, this.entity); i++) {
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if (jedisCluster.exists(key + "info")) {
                    String info = jedisCluster.get(key + "info");
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    if (idDrug != jsonObject.get("id").getAsInt()) continue;

                    // elimino il farmaco
                    jedisCluster.del(key + "info");
                    jedisCluster.del(key + "quantity");
                    redisHelper.returnIdToPool(jedisCluster, String.valueOf(i));
                }
            }
        }
        return null;
    }
}
