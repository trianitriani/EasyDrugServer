package it.unipi.EasyDrugServer.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {
        ClusterSettings clusterSettings = ClusterSettings.builder()
                .hosts(Arrays.asList(
                        new ServerAddress("localhost", 27018),
                        new ServerAddress("localhost", 27019),
                        new ServerAddress("localhost", 27020)
                ))
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.applySettings(clusterSettings))
                .build();

        return MongoClients.create(settings);
    }
}
