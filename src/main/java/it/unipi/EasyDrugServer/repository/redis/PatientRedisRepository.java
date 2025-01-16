package it.unipi.EasyDrugServer.repository.redis;

import it.unipi.EasyDrugServer.model.Drug;
import it.unipi.EasyDrugServer.model.Patient;
import it.unipi.EasyDrugServer.config.RedisConfig;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PatientRedisRepository extends RedisConfig {

    public PatientRedisRepository(){
        super();
    }

    public Patient findByCode(String codePatient) {
       return null;
    }
    
    public void saveDrugToPurchase(String codPatient, Drug drug) {
        try(JedisPool pool = new JedisPool("localhost", 6379); Jedis jedis = pool.getResource()){
            String info = "{id_drug: " + drug.getId() + "name: " + drug.getName() + "price: " + drug.getPrice() +
                    "prescriptionDate:" + drug.getPrescriptionDate() + "}";
            double quantity = drug.getQuantity();

            String key = "purch:" + drug.getId() + ":" + codPatient + ":";
            jedis.set(key + "info", info);
            jedis.set(key + "quantity", String.valueOf((quantity)));
        }
    }

    public List<Drug> getCart(String patientCode){
        try(JedisPool pool = new JedisPool("localhost", 6379); Jedis jedis = pool.getResource()){
            List<Drug> cartList = new ArrayList<>();
            for(int i=0; i<nEntities(jedis, "purch"); i++){
                String key = "purch:" + i + ":" + patientCode + ":";
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
            return cartList;
        }
    }
}
