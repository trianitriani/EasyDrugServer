package it.unipi.EasyDrugServer.utility;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import org.bson.UuidRepresentation;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@Component
@RequiredArgsConstructor
public class MongoBoot {
    private static String primary = "10.1.1.20";
    private static String secondary1 = "10.1.1.21";
    private static String secondary2 = "10.1.1.22";

    private final MongoTemplate mongoTemplate;

    private static Process startProcess(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        // processBuilder.inheritIO(); // Permette di vedere l'output nel terminale Java
        return processBuilder.start();
    }

    private void insertCollection(String collectionName) throws IOException {
        // Creazione della collezione se non esiste
        if (!mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.createCollection(collectionName, CollectionOptions.empty());
            System.out.println("Collezione " + collectionName + " creata!");
        }

        // Controllo se la collezione è vuota
        long documentCount = mongoTemplate.getCollection(collectionName).countDocuments();
        if (documentCount == 0) {
            System.out.println("Importazione dati...");

            // Caricare il JSON da resources/dbFiles
            InputStream inputStream = MongoBoot.class.getClassLoader()
                    .getResourceAsStream("dbFiles/" + collectionName + ".json");
            if (inputStream == null) {
                throw new RuntimeException("File " + collectionName + ".json non trovato in resources/dbFiles!");
            }

            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            List<Document> documents = new ArrayList<>();
            JsonArray jsonArray = JsonParser.parseString(jsonContent).getAsJsonArray();

            for (JsonElement element : jsonArray) {
                Document doc = Document.parse(element.toString());
                ConvertDates.convertDates(doc); // CONVERSIONE DATE
                documents.add(doc);
            }

            final int batchSize = 1500;
            for (int i = 0; i < documents.size(); i += batchSize) {
                List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
                try {
                    mongoTemplate.getCollection(collectionName).insertMany(batch);
                    System.out.println("Inseriti " + batch.size() + " documenti nella collezione " + collectionName);
                } catch (Exception e) {
                    System.err.println("Errore nell'inserimento dei documenti in " + collectionName + ": " + e.getMessage());
                }
            }

            System.out.println("Importazione " + collectionName + " completata!");
        } else {
            System.out.println("Dati di " + collectionName + " già presenti, nessuna importazione necessaria.");
        }
    }

    @PostConstruct
    public void init() throws Exception{
        List<Process> processes = new ArrayList<>();

        /*
        // Avvia i nodi del cluster
        processes.add(startProcess("mongod --replSet rs0 --port 27018 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r1 --oplogSize 200"));
        processes.add(startProcess("mongod --replSet rs0 --port 27019 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r2 --oplogSize 200"));
        processes.add(startProcess("mongod --replSet rs0 --port 27020 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r3 --oplogSize 200"));

        // Aspetta un attimo che i processi si avviino
        Thread.sleep(5000);

        // Avvia mongosh per connettersi al primo nodo
        processes.add(startProcess("mongosh --port 27018"));

        System.out.println("Tutti i processi sono stati avviati!");

        Thread.sleep(3000);


         */
        insertCollection("drugs");
        insertCollection("doctors");
        insertCollection("patients");
        insertCollection("pharmacies");
        insertCollection("purchases");
        insertCollection("researchers");
    }
}