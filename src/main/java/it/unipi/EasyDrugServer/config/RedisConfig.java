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
    private JedisSentinelPool jedisSentinelPool;
    private final String sentinel1 = "10.1.1.20";
    private final String sentinel2 = "10.1.1.21";
    private final String sentinel3 = "10.1.1.22";

    @Bean
    public JedisSentinelPool jedisSentinelPool() {
        Set<String> sentinels = new HashSet<>();
        sentinels.add(sentinel1+":26379");
        sentinels.add(sentinel2+":26379");
        sentinels.add(sentinel3+":26379");
        jedisSentinelPool = new JedisSentinelPool("mymaster", sentinels);
        return jedisSentinelPool;
    }

    @PreDestroy
    public void shutdownJedis() {
        System.out.println("Chiusura delle connessioni Redis...");
        try {
            if (jedisSentinelPool != null) {
                jedisSentinelPool.close();
                System.out.println("JedisSentinelPool chiuso correttamente.");
            }
        } catch (Exception e) {
            System.err.println("Errore durante la chiusura di Redis: " + e.getMessage());
        }
    }
}
