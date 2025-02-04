package it.unipi.EasyDrugServer.utility;

import com.mongodb.client.*;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class MongoBoot {
    private static Process startProcess(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        // processBuilder.inheritIO(); // Permette di vedere l'output nel terminale Java
        return processBuilder.start();
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

        /*
        // 1. Controlla se MongoDB è già in esecuzione
        if (!isMongoRunning()) {
            System.out.println("Starting MongoDB...");
            ProcessBuilder mongod27018 = new ProcessBuilder("mongod --replSet rs0 --port 27018 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r1  --oplogSize 200");
            mongod27018.start();
            ProcessBuilder mongod27019 = new ProcessBuilder("mongod --replSet rs0 --port 27019 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r2  --oplogSize 200");
            mongod27019.start();
            ProcessBuilder mongod27020 = new ProcessBuilder("mongod --replSet rs0 --port 27020 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r3  --oplogSize 200");
            mongod27020.start();
            ProcessBuilder processBuilder = new ProcessBuilder("mongosh --port 27018");
            processBuilder.start();
            Thread.sleep(5000); // Aspetta che MongoDB si avvii
        }
        */

        List<Process> processes = new ArrayList<>();

        // Avvia i nodi del replica set
        processes.add(startProcess("mongod --replSet rs0 --port 27018 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r1 --oplogSize 200"));
        processes.add(startProcess("mongod --replSet rs0 --port 27019 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r2 --oplogSize 200"));
        processes.add(startProcess("mongod --replSet rs0 --port 27020 --bind_ip localhost --dbpath c:\\MongoDB\\data\\r3 --oplogSize 200"));

        // Aspetta un attimo che i processi si avviino
        Thread.sleep(5000);

        // Avvia mongosh per connettersi al primo nodo
        processes.add(startProcess("mongosh --port 27018"));

        System.out.println("Tutti i processi sono stati avviati!");

        Thread.sleep(5000);

        // 2. Connessione a MongoDB
        String uri = "mongodb://localhost:27018,localhost:27019,localhost:27020/EasyDrugDB?replicaSet=rs0";
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
