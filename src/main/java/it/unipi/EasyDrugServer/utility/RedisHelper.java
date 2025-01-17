package it.unipi.EasyDrugServer.utility;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

@Component
public class RedisHelper {
    private final String host = "localhost";
    private final int port = 6379;
    private final JedisPool jedisPool;

    public RedisHelper() {
        this.jedisPool = new JedisPool(host, port);
    }

    public RedisHelper(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * In this way we can reuse the id of a previous element that was eliminated, if there not exists
     * new increments the counter of element of that entity.
     * @param jedisCluster connection to server
     * @param entity name of the entity
     * @return id
     */
     public String getReusableId(JedisCluster jedisCluster, String entity){
        // mi restituisce un id disponibile (cioè di vecchie chiavi eliminate)
        String id = jedisCluster.rpop("available_"+entity+"_ids");
        if(id == null){
            // mi restituisce un nuovo id
            Long newId = jedisCluster.incr("global:"+entity+"_counter");
            id = String.valueOf(newId);
        }
        return id;
    }

    public void returnIdToPool(Jedis jedis, String id){
        jedis.lpush("available_"+id+"_ids",id);
    }

    public int nEntities(JedisCluster jedisCluster, String entity){
        return Integer.parseInt(jedisCluster.get("global:"+entity+"_counter"));
    }

    public void close(){
        if(jedisPool != null){
            jedisPool.close();
        }
    }
}
