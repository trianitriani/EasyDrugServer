package it.unipi.EasyDrugServer.repository.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unipi.EasyDrugServer.dto.PrescribedDrugDTO;
import it.unipi.EasyDrugServer.dto.PrescriptionDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.UnauthorizedException;
import it.unipi.EasyDrugServer.utility.RedisHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisCluster;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;

@Setter
@Getter
@Repository
public class PrescriptionRedisRepository {
    private final String entity = "pres";
    private final JedisCluster jedisCluster;
    private final RedisHelper redisHelper;

    public PrescriptionRedisRepository(JedisCluster jedisCluster, RedisHelper redisHelper) {
        this.jedisCluster = jedisCluster;
        this.redisHelper = redisHelper;
    }

    public PrescribedDrugDTO saveDrugIntoPrescription(String doctorCode, String patientCode, PrescribedDrugDTO drug) throws UnauthorizedException {
        try(jedisCluster){
            // controllo se il medico può prescrivere al paziente selezionato [funzione]
            if(!this.isValidDoctor(doctorCode, patientCode))
                throw new UnauthorizedException("The patient "+patientCode+" has another family doctor.");

            // inserisco il farmaco nel key value
            JsonObject info = new JsonObject();
            info.addProperty("id", drug.getId());
            info.addProperty("name", drug.getName());
            info.addProperty("price", drug.getPrice());

            int quantity = drug.getQuantity();
            boolean purchased = drug.isPurchased();

            String id_pres = redisHelper.getReusableId(jedisCluster, "pres");
            String key = this.entity + ":" + id_pres + ":" + patientCode + ":";
            jedisCluster.set(key + "info", String.valueOf(info));
            jedisCluster.set(key + "quantity", String.valueOf(quantity));
            jedisCluster.set(key + "purchased", String.valueOf(purchased));

            PrescribedDrugDTO prescriptedDrug = new PrescribedDrugDTO();
            prescriptedDrug.setId(drug.getId());
            prescriptedDrug.setName(drug.getName());
            prescriptedDrug.setPrice(drug.getPrice());
            prescriptedDrug.setQuantity(quantity);
            prescriptedDrug.setPurchased(purchased);
            return prescriptedDrug;
        }
    }

    public PrescriptionDTO confirmPrescription(String doctorCode, String patientCode) throws UnauthorizedException, ForbiddenException {
        try(jedisCluster){
            // controllo se il medico può prescrivere al paziente selezionato [funzione]
            if(!this.isValidDoctor(doctorCode, patientCode))
                throw new UnauthorizedException("The patient "+patientCode+" has another family doctor.");

            PrescriptionDTO prescription = new PrescriptionDTO();
            LocalDateTime timestamp = LocalDateTime.now();
            prescription.setTimestamp(timestamp);
            for(int i=1; i<=redisHelper.nEntities(jedisCluster, this.entity); i++){
                // cerco tutti i farmaci con il campo "timestamp" non esistente (quelli non ancora confermati)
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if(jedisCluster.exists(key + "info") && !jedisCluster.exists(key + "timestamp")){
                    // inserisco il farmaco prescritto nella lista di prescrizione
                    PrescribedDrugDTO prescriptedDrug = new PrescribedDrugDTO();
                    JsonObject jsonObject = JsonParser.parseString(jedisCluster.get(key + "info")).getAsJsonObject();
                    prescriptedDrug.setId(jsonObject.get("id").getAsInt());
                    prescriptedDrug.setName(jsonObject.get("name").getAsString());
                    prescriptedDrug.setPrice(jsonObject.get("price").getAsDouble());

                    String quantity = jedisCluster.get(key + "quantity");
                    prescriptedDrug.setQuantity(Integer.parseInt(quantity));
                    String purchased = jedisCluster.get(key + "purchased");
                    prescriptedDrug.setPurchased(Boolean.parseBoolean(purchased));

                    // setto per ognuno di questi il timestamp attuale
                    jedisCluster.set(key + "timestamp", String.valueOf(timestamp));
                    prescription.addPrescribedDrug(prescriptedDrug);
                }
            }
            if(prescription.isEmpty())
                throw new ForbiddenException("The patient "+patientCode+" has no prescriptions.");
            return prescription;
        }
    }

    public PrescribedDrugDTO modifyPrescribedDrugQuantity(String doctorCode, String patientCode, int idDrug, int quantity) throws UnauthorizedException, ForbiddenException {
        try(jedisCluster){
            if(!this.isValidDoctor(doctorCode, patientCode))
                throw new UnauthorizedException("The patient "+patientCode+" has another family doctor.");

            for(int i=1; i<=redisHelper.nEntities(jedisCluster, this.entity); i++) {
                // cerco tutti i farmaci con il campo "timestamp" non esistente (quelli non ancora confermati)
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if(!jedisCluster.exists(key + "timestamp") && jedisCluster.exists(key + "info")){
                    String info = jedisCluster.get(key + "info");
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    if(idDrug == jsonObject.get("id").getAsInt()){
                        jedisCluster.set(key + "quantity", String.valueOf(quantity));

                        PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
                        prescribedDrug.setId(jsonObject.get("id").getAsInt());
                        prescribedDrug.setName(jsonObject.get("name").getAsString());
                        prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
                        prescribedDrug.setQuantity(Integer.parseInt(String.valueOf(quantity)));
                        String purchased = jedisCluster.get(key + "purchased");
                        prescribedDrug.setPurchased(Boolean.parseBoolean(purchased));
                        return prescribedDrug;
                    }
                }
            }
            throw new BadRequestException("None drug with id "+idDrug+" into an inactive prescription");
        }
    }

    public PrescribedDrugDTO deletePrescribedDrug(String doctorCode, String patientCode, int idDrug){
        try(jedisCluster) {
            if (!this.isValidDoctor(doctorCode, patientCode))
                throw new UnauthorizedException("The patient " + patientCode + " has another family doctor.");

            for(int i=1; i<=redisHelper.nEntities(jedisCluster, this.entity); i++) {
                // cerco tutti i farmaci con il campo "timestamp" non esistente (quelli non ancora confermati)
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if (!jedisCluster.exists(key + "timestamp") && jedisCluster.exists(key + "info")) {
                    String info = jedisCluster.get(key + "info");
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    if(idDrug == jsonObject.get("id").getAsInt()){
                        jedisCluster.del(key + "info");
                        jedisCluster.del(key + "quantity");
                        jedisCluster.del(key + "purchased");
                        redisHelper.returnIdToPool(jedisCluster, String.valueOf(i));
                    }
                }
            }
            return null;
        }
    }



    private boolean isValidDoctor(String doctorCode, String patientCode){
        String patKey = "pat: " + patientCode + ":" + doctorCode;
        if(!Objects.equals(doctorCode, jedisCluster.get(patKey)))
            return false;
        return true;
    }

    public HashMap<LocalDateTime, PrescriptionDTO> getAllPrescriptions(String patientCode) {
        try(jedisCluster) {
            HashMap<LocalDateTime, PrescriptionDTO> prescriptions = new HashMap<>();
            for (int i = 1; i <= redisHelper.nEntities(jedisCluster, this.entity); i++) {
                String key = this.entity + ":" + i + ":" + patientCode + ":";
                if(jedisCluster.exists(key + "timestamp")){
                    // allora si tratta di un farmaco di una prescrizione già attiva
                    String timestampString = jedisCluster.get("timestamp");
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    LocalDateTime timestamp = LocalDateTime.parse(timestampString, formatter);

                    // Creazione dell'oggetto PrescribedDrug da inserire nella lista della prescrizione
                    PrescribedDrugDTO prescribedDrug = new PrescribedDrugDTO();
                    String info = jedisCluster.get(key + "info");
                    JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
                    prescribedDrug.setId(jsonObject.get("id").getAsInt());
                    prescribedDrug.setName(jsonObject.get("name").getAsString());
                    prescribedDrug.setPrice(jsonObject.get("price").getAsDouble());
                    String quantity = jedisCluster.get(key + "quantity");
                    prescribedDrug.setQuantity(Integer.parseInt(String.valueOf(quantity)));
                    String purchased = jedisCluster.get(key + "purchased");
                    prescribedDrug.setPurchased(Boolean.parseBoolean(purchased));

                    if(prescriptions.containsKey(timestamp)){
                        PrescriptionDTO prescription = prescriptions.get(timestamp);
                        prescription.addPrescribedDrug(prescribedDrug);
                    } else {
                        PrescriptionDTO prescription = new PrescriptionDTO();
                        prescription.setTimestamp(timestamp);
                        prescription.addPrescribedDrug(prescribedDrug);
                        prescriptions.put(timestamp, prescription);
                    }
                }

            }
        }
    }
}
