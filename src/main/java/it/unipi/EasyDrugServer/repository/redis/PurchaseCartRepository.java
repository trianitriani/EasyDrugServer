package it.unipi.EasyDrugServer.repository.redis;

import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.config.RedisConfig;
import it.unipi.EasyDrugServer.model.PurchaseCart;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PurchaseCartRepository extends RedisConfig {
    private final String entity = "purch";

    public PurchaseCartRepository() {
        super();
    }

    public Patient findByCode(String codePatient) {
        return null;
    }
    
    public void saveDrugIntoPurchaseCart(String codPatient, Drug drug) {
        try(JedisPool pool = new JedisPool(host, port); Jedis jedis = pool.getResource()){
            String info = "{ id_drug: " + drug.getId() + "name: " + drug.getName() + "price: " + drug.getPrice() +
                    "prescriptionTimestamp:" + drug.getPrescriptionTimestamp() + "}";
            double quantity = drug.getQuantity();
            // Now we have to search a valid id_purch for a new element
            String id_purch = getReusableId(jedis, "purch");
            String key = this.entity + id_purch + ":" + codPatient + ":";
            jedis.set(key + "info", info);
            jedis.set(key + "quantity", String.valueOf((quantity)));
        }
    }

    public PurchaseCart getPurchaseCart(String patientCode){
        try(JedisPool pool = new JedisPool(host, port); Jedis jedis = pool.getResource()){
            PurchaseCart purchaseCart = new PurchaseCart();
            List<Drug> cartList = new ArrayList<>();
            for(int i=0; i<nEntities(jedis, this.entity); i++){
                String key = this.entity + i + ":" + patientCode + ":";
                String info = jedis.get(key + "info");
                String quantity;
                if(info != null){
                    quantity = jedis.get(key + "quantity");
                } else continue;
                // Se continuo significa che l'oggetto esiste realmente e lo inserisco nella lista
                Drug drug = new Drug();
                drug.setQuantity(Integer.parseInt(quantity));
                cartList.add(drug);
            }
            purchaseCart.setDrugs(cartList);
            return purchaseCart;
        }
    }
}
