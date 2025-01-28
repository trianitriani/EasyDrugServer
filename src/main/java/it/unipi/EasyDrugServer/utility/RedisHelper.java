package it.unipi.EasyDrugServer.utility;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

@Component
public class RedisHelper {

    public RedisHelper() {

    }

    /**
     * In this way we can reuse the id of a previous element that was eliminated, if there not exists
     * new increments the counter of element of that entity.
     * @param jedis connection to server
     * @param entity name of the entity
     * @return id
     */
     public String getReusableId(Jedis jedis, String entity){
        // mi restituisce un id disponibile (cioè di vecchie chiavi eliminate)
        String id = jedis.rpop("available_"+entity+"_ids");
        if(id == null){
            // mi restituisce un nuovo id
            Long newId = jedis.incr("global:"+entity+"_counter");
            id = String.valueOf(newId);
        }
        return id;
    }
    
    public void returnIdToPool(Transaction transaction, String entity, String id){
        transaction.lpush("available_"+entity+"_ids", id);
    }

    public int nEntities(Jedis jedis, String entity){
        String n = jedis.get("global:"+entity+"_counter");
        if(n == null) return 0;
        return Integer.parseInt(n);
    }

}
