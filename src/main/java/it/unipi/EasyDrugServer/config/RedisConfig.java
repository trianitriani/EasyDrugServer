package it.unipi.EasyDrugServer.config;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
public class RedisConfig {
    protected final String host = "localhost";
    protected final int port = 6379;
    private final JedisPool jedisPool;

    public RedisConfig() {
        this.jedisPool = new JedisPool(host, port);
    }

    public RedisConfig(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * In this way we can reuse the id of a previous element that was eliminated, if there not exists
     * new increments the counter of element of that entity.
     * @param jedis connection to server
     * @param entity name of the entity
     * @return id
     */
    public String getReusableId(Jedis jedis,String entity){
        String id = jedis.rpop("available_"+entity+"_ids");
        if(id == null){
            Long newId = jedis.incr("global:"+entity+"_counter");
            id = String.valueOf(newId);
        }
        return id;
    }

    public void returnIdToPool(Jedis jedis, String id){
        jedis.lpush("available_"+id+"_ids",id);
    }

    public int nEntities(Jedis jedis, String entity){
        String max = jedis.get("global:"+entity+"_counter");
        return Integer.parseInt(max);
    }

    public void close(){
        if(jedisPool != null){
            jedisPool.close();
        }
    }
}
