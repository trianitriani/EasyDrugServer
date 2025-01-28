package it.unipi.EasyDrugServer.config;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class RedisConfig {
    private Jedis jedis;
    private String host = "localhost";
    private Integer port = 6379;

    @Bean
    public Jedis jedis() {
        this.jedis = new Jedis(host, port);
        return this.jedis;
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
