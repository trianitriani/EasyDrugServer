package it.unipi.EasyDrugServer;

import it.unipi.EasyDrugServer.utility.RedisBoot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableMongoRepositories(basePackages = "it.unipi.EasyDrugServer.repository.mongo")
public class EasyDrugServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EasyDrugServerApplication.class, args);
	}

}