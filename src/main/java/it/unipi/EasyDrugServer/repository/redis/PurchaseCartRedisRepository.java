package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import it.unipi.EasyDrugServer.model.PurchaseCart;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;

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

    public Patient findByCode(String codePatient) {
        return null;
    }

    public void saveDrugIntoPurchaseCart(String codPatient, Drug drug) {
        try(jedisCluster){
            String info = "{ id: " + drug.getId() + ", name: '" + drug.getName() + "', price: " + drug.getPrice() +
                    ", prescriptionTimestamp: '" + drug.getPrescriptionTimestamp() + "' }";
            double quantity = drug.getQuantity();
            // Now we have to search a valid id_purch for a new element
            String id_purch = redisHelper.getReusableId(jedisCluster, "purch");
            String key = this.entity + ":" + id_purch + ":" + codPatient + ":";
            jedisCluster.set(key + "info", info);
            jedisCluster.set(key + "quantity", String.valueOf((quantity)));
            System.out.println(jedisCluster.get(key + "info"));
        }
    }

    public PurchaseCart getPurchaseCart(String patientCode){
        try(jedisCluster){
            PurchaseCart purchaseCart = new PurchaseCart();
            List<Drug> cartList = new ArrayList<>();
            for(int i=8; i<redisHelper.nEntities(jedisCluster, this.entity); i++){
                    String info;
                    String quantity;
                    String key = this.entity + ":" + i + ":" + patientCode + ":";
                    if(jedisCluster.exists(key + "info")){
                        info = jedisCluster.get(key + "info");
                        quantity = jedisCluster.get(key + "quantity");
                    } else continue;
                    // Se continuo significa che l'oggetto esiste realmente e lo inserisco nella lista
                    Drug drug = new Drug();
                    // inserimento info
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    int id = jsonObject.get("id").getAsInt();
                    String name = jsonObject.get("name").getAsString();
                    double price = jsonObject.get("price").getAsDouble();

                    // Ottieni il timestamp come long
                    long timestampMillis = jsonObject.get("timestamp").getAsLong();
                    Timestamp timestamp = new Timestamp(timestampMillis);

                    drug.setId(id);
                    drug.setName(name);
                    drug.setPrice(price);
                    drug.setPrescriptionTimestamp(timestamp);
                    drug.setQuantity(Integer.parseInt(quantity));
                    cartList.add(drug);
            }
            purchaseCart.setDrugs(cartList);
            return purchaseCart;
        }
    }


    public int confirmPurchase(String patientCode){
        try(jedisCluster){
            int deleted = 0;

            // cerco tutti i farmaci che sono nel carrello e si riferiscono al paziente selezionato
            for(int i=0; i<redisHelper.nEntities(jedisCluster, this.entity); i++){
                String key = this.entity + i + ":" + patientCode + ":";
                if(jedisCluster.exists(key + "info")){
                    String info = jedisCluster.get(key + "info");
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    int id = jsonObject.get("id").getAsInt();
                    long timestampMillis = jsonObject.get("timestamp").getAsLong();
                    Timestamp timestamp = new Timestamp(timestampMillis);

                    // elimino dal key value il farmaco nel carrello
                    deleted++;
                    jedisCluster.del(key + "info");
                    jedisCluster.del(key + "quantity");

                    // metto purchased = true al corrispondente farmaco prescritto (stesso codice e timestamp)


                }
            }

            // se tutti i farmaci della prescrizione hanno purchased = true, allora elimino tutta la prescrizione


            return deleted;
        }
    }

}
