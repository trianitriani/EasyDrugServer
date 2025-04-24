package it.unipi.EasyDrugServer.utility;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import java.io.IOException;
import java.util.*;
import com.google.gson.*;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@Component
@RequiredArgsConstructor
public class MongoBoot {
    private final MongoTemplate mongoTemplate;

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
        insertCollection("drugs");
        insertCollection("doctors");
        insertCollection("patients");
        insertCollection("pharmacies");
        insertCollection("purchases");
        insertCollection("researchers");
    }
}