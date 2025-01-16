package it.unipi.EasyDrugServer.config;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
public class RedisConfig {
    private final JedisPool jedisPool;

    public RedisConfig() {
        this.jedisPool = new JedisPool("localhost", 6379);
    }

    public RedisConfig(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

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
