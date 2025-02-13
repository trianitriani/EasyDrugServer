package it.unipi.EasyDrugServer.config;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class RedisConfig {
    private JedisCluster jedis;
    private String master = "localhost";
    private String replica1 = "localhost";
    private String replica2 = "localhost";
    private Integer port = 6379;

    @Bean
    public JedisSentinelPool jedisSentinelPool() {
        Set<String> sentinels = new HashSet<>();
        sentinels.add(master+":26379");
        sentinels.add(replica1+":26380");
        sentinels.add(replica2+":26381");
        return new JedisSentinelPool("mymaster", sentinels);
    }

    @Bean
    public JedisPool jedisPool(JedisSentinelPool jedisSentinelPool) {
        return new JedisPool(jedisSentinelPool.getCurrentHostMaster().getHost(), jedisSentinelPool.getCurrentHostMaster().getPort());
    }

    @Bean
    public Jedis jedis(JedisPool jedisPool) {
        return jedisPool.getResource();
    }

    @PreDestroy
    public void shutdownJedis() {
        if (jedis != null) {
            try {
                jedis.close();
                System.out.println("Jedis chiuso correttamente.");
            } catch (Exception e) {
                System.err.println("Errore durante la chiusura di Jedis: " + e.getMessage());
            }
        }
    }
}
