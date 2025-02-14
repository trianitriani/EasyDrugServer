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
    private String master = "localhost";
    private String replica1 = "localhost";
    private String replica2 = "localhost";

    @Bean
    public JedisSentinelPool jedisSentinelPool() {
        Set<String> sentinels = new HashSet<>();
        sentinels.add(master+":26379");
        sentinels.add(replica1+":26380");
        sentinels.add(replica2+":26381");
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
