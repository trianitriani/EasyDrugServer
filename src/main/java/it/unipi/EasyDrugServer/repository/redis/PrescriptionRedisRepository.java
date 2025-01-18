package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import it.unipi.EasyDrugServer.dto.PrescriptedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisCluster;

@Setter
@Getter
@Repository
public class PrescriptionRedisRepository {
    private final String entity = "pres";
    private final JedisCluster jedisCluster;
    private final RedisHelper redisHelper;

    public PrescriptionRedisRepository(JedisCluster jedisCluster, RedisHelper redisHelper) {
        this.jedisCluster = jedisCluster;
        this.redisHelper = redisHelper;
    }

    public void saveDrugIntoPrescriptionList(String doctorCode, String patientCode, PrescriptedDrugDTO drug){
        try(jedisCluster){
            // controllo se il medico può prescrivere al paziente selezionato [funzione]

            // inserisco il farmaco nel key value
            JsonObject info = new JsonObject();
            info.addProperty("id", drug.getId());
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());

            int quantity = drug.getQuantity();
            boolean purchased = drug.getPurchased();

            String id_pres = redisHelper.getReusableId(jedisCluster, "pres");
            String key = this.entity + ":" + id_pres + ":" + patientCode + ":";
            jedisCluster.set(key + "info", String.valueOf(info));
            jedisCluster.set(key + "quantity", String.valueOf(quantity));
            jedisCluster.set(key + "purchased", String.valueOf(purchased));
        }
    }

    public int confirmPrescription(String doctorCode, String patientCode){
        try(jedisCluster){
            int confirmed = 0;
            PrescriptionDTO prescription = new PrescriptionDTO();

            // controllo se il medico può prescrivere al paziente selezionato [funzione]


            for(int i=1; i<=redisHelper.nEntities(jedisCluster, this.entity); i++){
                // cerco tutti i farmaci con il campo "timestamp" non esistente (quelli non ancora confermati)
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if(jedisCluster.exists(key + "info") && !jedisCluster.exists(key + "timestamp")){

                }
            }

            // setto per ognuno di questi il timestamp attuale

            // metto tutti ii farmaci della prescrizione nella classe Prescription e la ritorno

            return confirmed;
        }
    }
}
