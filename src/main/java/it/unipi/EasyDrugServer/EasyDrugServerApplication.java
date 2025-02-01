package it.unipi.EasyDrugServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "it.unipi.EasyDrugServer.repository.mongo")
public class EasyDrugServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EasyDrugServerApplication.class, args);
	}

}