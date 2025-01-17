package it.unipi.EasyDrugServer.config;

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

    @Bean
    public JedisCluster jedisCluster() {
        Set<HostAndPort> jedisClusterNodes = new HashSet<>();
        jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7001));
        jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7002));
        jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7003));
        return new JedisCluster(jedisClusterNodes);
    }

}
