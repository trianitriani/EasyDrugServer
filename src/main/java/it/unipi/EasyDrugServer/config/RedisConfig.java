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

    private JedisCluster jedisCluster;

    @Bean
    public JedisCluster jedisCluster() {
        Set<HostAndPort> jedisClusterNodes = new HashSet<>();
        jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7001));
        jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7002));
        jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7003));
        this.jedisCluster = new JedisCluster(jedisClusterNodes);
        return this.jedisCluster;
    }

    @PreDestroy
    public void shutdownJedisCluster() {
        if (jedisCluster != null) {
            try {
                jedisCluster.close();
                System.out.println("JedisCluster chiuso correttamente.");
            } catch (Exception e) {
                System.err.println("Errore durante la chiusura di JedisCluster: " + e.getMessage());
            }
        }
    }
}
