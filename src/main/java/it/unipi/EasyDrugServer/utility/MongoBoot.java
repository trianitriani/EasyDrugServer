package it.unipi.EasyDrugServer.utility;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import org.bson.UuidRepresentation;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MongoBoot {
    private static String primary = "10.1.1.20";
    private static String secondary1 = "10.1.1.21";
    private static String secondary2 = "10.1.1.22";

    private static Process startProcess(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        // processBuilder.inheritIO(); // Permette di vedere l'output nel terminale Java
        return processBuilder.start();
    }

    private static void createIndex(MongoDatabase database, String collection, String index) {
        MongoCollection<Document> drugsCollection = database.getCollection(collection);

        // Creazione dell'indice su "name"
        IndexModel singleIndex = new IndexModel(
                Indexes.ascending(index)
        );
        drugsCollection.createIndexes(Collections.singletonList(singleIndex));
    }

    private static void createCompoundIndex(MongoDatabase database, String collection, String index1, String index2){
        MongoCollection<Document> drugsCollection = database.getCollection(collection);

        // Creazione dell'indice composto su "name" e "category"
        IndexModel compoundIndex = new IndexModel(
                Indexes.compoundIndex(
                        Indexes.ascending(index1),
                        Indexes.ascending(index2)
                )
        );

        drugsCollection.createIndexes(Collections.singletonList(compoundIndex));

    }

    private static void insertCollection(String collectionName, MongoDatabase database) throws IOException {
        // 3. Creazione della collezione (se non esiste)
        boolean collectionExists = false;
        for (String name : database.listCollectionNames()) {
            if (name.equals(collectionName)) {
                collectionExists = true;
                break;
            }
        }
        if (!collectionExists) {
            database.createCollection(collectionName);
            System.out.println("Collezione " + collectionName + " creata!");
        }

        MongoCollection<Document> collection = database.getCollection(collectionName);

        // 4. Importazione dei dati solo se la collezione è vuota
        if (collection.countDocuments() == 0) {
            System.out.println("Importazione dati...");
            //  Caricare il JSON da resources/dbFiles
            InputStream inputStream = MongoBoot.class.getClassLoader()
                    .getResourceAsStream("dbFiles/" + collectionName + ".json");
            if (inputStream == null) {
                throw new RuntimeException("File " + collectionName + ".json non trovato in resources/dbFiles!");
            }

            // Convertire InputStream in Stringa
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            /*
            Gson gson = new Gson();
            List<Document> documents = Arrays.asList(gson.fromJson(jsonContent, Document[].class));
             */

            List<Document> documents = new ArrayList<>();
            JsonArray jsonArray = JsonParser.parseString(jsonContent).getAsJsonArray();

            for (JsonElement element : jsonArray) {
                Document doc = Document.parse(element.toString());
                ConvertDates.convertDates(doc);     // CONVERSIONE DATE 
                documents.add(doc);
            }


            final int batchSize = 1500;
            for (int i = 0; i < documents.size(); i += batchSize) {
                List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
                try {
                    collection.insertMany(batch);
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

    public static void main(String[] args) throws Exception {
        /*
        // 1. Creare una cartella "database" nel progetto se non esiste
        String projectDbPath = System.getProperty("user.dir") + "/database";
        File dbDir = new File(projectDbPath);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
            System.out.println("Cartella database creata in: " + projectDbPath);
        }

         */

        List<Process> processes = new ArrayList<>();

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

        // 2. Connessione a MongoDB
        String uri = "mongodb://localhost:27018,localhost:27019,localhost:27020/EasyDrugDB?replicaSet=rs0";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("EasyDrugDB");

            // createCompoundIndex(database, "drugs", "drugName", "onPrescription");
            // createCompoundIndex(database, "patients", "doctorCode", "surname");
            // createIndex(database, "purchases", "purchaseDate");

            insertCollection("drugs", database);
            insertCollection("doctors", database);
            insertCollection("patients", database);
            insertCollection("pharmacies", database);
            insertCollection("purchases", database);
            insertCollection("researchers", database);
        }
    }

    // Metodo per controllare se MongoDB è in esecuzione
    private static boolean isMongoRunning() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("pgrep", "mongod");
            Process process = processBuilder.start();
            return process.waitFor() == 0; // Se il processo esiste, MongoDB è attivo
        } catch (Exception e) {
            return false;
        }
    }
}