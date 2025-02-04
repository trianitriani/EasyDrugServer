package it.unipi.EasyDrugServer.utility;

import com.mongodb.client.*;
import org.bson.Document;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class MongoBoot {
    public static void main(String[] args) throws Exception {
        // 1. Creare una cartella "database" nel progetto se non esiste
        String projectDbPath = System.getProperty("user.dir") + "/database";
        File dbDir = new File(projectDbPath);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
            System.out.println("Cartella database creata in: " + projectDbPath);
        }

        // 1. Controlla se MongoDB è già in esecuzione
        if (!isMongoRunning()) {
            System.out.println("Starting MongoDB...");
            ProcessBuilder processBuilder = new ProcessBuilder("mongod", "--dbpath",projectDbPath);
            processBuilder.start();
            Thread.sleep(5000); // Aspetta che MongoDB si avvii
        }

        // 2. Connessione a MongoDB
        String uri = "mongodb://localhost:27017";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("EasyDrugDB");

            // 3. Creazione della collezione (se non esiste)
            boolean collectionExists = false;
            for (String name : database.listCollectionNames()) {
                if (name.equals("drugs")) {
                    collectionExists = true;
                    break;
                }
            }
            if (!collectionExists) {
                database.createCollection("drugs");
                System.out.println("Collezione creata!");
            }

            MongoCollection<Document> collection = database.getCollection("drugs");

            // 4. Importazione dei dati solo se la collezione è vuota
            if (collection.countDocuments() == 0) {
                System.out.println("Importazione dati...");
                //  Caricare il JSON da resources/dbFiles
                InputStream inputStream = MongoBoot.class.getClassLoader().getResourceAsStream("dbFiles/drugs.json");
                if (inputStream == null) {
                    throw new RuntimeException("File JSON non trovato in resources/dbFiles!");
                }

                // Convertire InputStream in Stringa
                String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);



                Gson gson = new Gson();
                List<Document> documents = Arrays.asList(gson.fromJson(jsonContent, Document[].class));

                collection.insertMany(documents);
                System.out.println("Importazione completata!");
            } else {
                System.out.println("Dati già presenti, nessuna importazione necessaria.");
            }
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
