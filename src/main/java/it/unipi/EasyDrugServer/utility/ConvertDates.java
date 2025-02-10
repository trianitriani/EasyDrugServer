package it.unipi.EasyDrugServer.utility;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.bson.Document;

public class ConvertDates {
    private static final List<String> DATE_FIELDS = Arrays.asList("timestamp", "prescriptionDate", "purchaseDate");

    // Metodo pubblico e statico per poter essere chiamato dall'esterno
    public static void convertDates(Document document) {
        for (String key : document.keySet()) {
            Object value = document.get(key);

            // Se il valore è una STRINGA e il nome del campo è tra quelli delle date
            if (value instanceof String && DATE_FIELDS.contains(key)) {
                try {
                    String dateString = (String) value;
                    if (!dateString.endsWith("Z")) {
                        dateString += "Z";
                    }
                    Instant instant = Instant.parse(dateString);
                    document.put(key, Date.from(instant));

                } catch (Exception e) {
                    System.err.println("Errore nel parsing della data per il campo " + key + ": " + value);
                }
            }

            // Se il valore è un altro documento, esegue ricorsione
            else if (value instanceof Document) {
                convertDates((Document) value);
            }

            // Se il valore è un array, controlla se contiene documenti e li converte
            else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    if (item instanceof Document) {
                        convertDates((Document) item);
                    }
                }
            }
        }
    }
}
