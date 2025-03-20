package it.unipi.EasyDrugServer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoException;
import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.*;
import it.unipi.EasyDrugServer.repository.mongo.CommitLogRepository;
import it.unipi.EasyDrugServer.repository.mongo.PatientRepository;
import it.unipi.EasyDrugServer.repository.mongo.PharmacyRepository;
import it.unipi.EasyDrugServer.repository.mongo.PurchaseRepository;
import it.unipi.EasyDrugServer.repository.redis.PrescriptionRedisRepository;
import it.unipi.EasyDrugServer.repository.redis.PurchaseCartRedisRepository;
import it.unipi.EasyDrugServer.utility.LocalDateTimeAdapter;
import it.unipi.EasyDrugServer.utility.PasswordHasher;
import it.unipi.EasyDrugServer.utility.RollbackProcessor;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

@Service
@RequiredArgsConstructor
public class PharmacyService {
        private final UserService userService;
        private final PurchaseCartRedisRepository purchaseCartRedisRepository;
        private final PrescriptionRedisRepository prescriptionRedisRepository;
        private final PharmacyRepository pharmacyRepository;
        private final PurchaseRepository purchaseRepository;
        private final PatientRepository patientRepository;
        private final CommitLogRepository commitLogRepository;
        private final RollbackProcessor rollbackProcessor;
        private int attempt = 0;

        @Autowired
        private MongoTemplate mongoTemplate;

        public PharmacyHomeDTO viewPharmacyHome(String id_pat) {
            if(id_pat == null || id_pat.isEmpty())
                throw new BadRequestException("The patient id can not be null");
            PharmacyHomeDTO pharmacyHomeDTO = new PharmacyHomeDTO();
            // Mostra il carrello degli acquisti del paziente
            pharmacyHomeDTO.setPurchaseCart(purchaseCartRedisRepository.getPurchaseCart(id_pat));
            // Mostra tutte le prescrizioni attive del paziente
            pharmacyHomeDTO.setPrescriptions(prescriptionRedisRepository.getAllActivePrescriptions(id_pat));
            return pharmacyHomeDTO;
        }

        public PurchaseCartDrugDTO savePurchaseDrug(String id_pat, PurchaseCartDrugDTO drug) {
            if(id_pat == null || id_pat.isEmpty())
                throw new BadRequestException("The patient id can not be null");
            if(drug.getName() == null || Objects.equals(drug.getName(), ""))
                throw new BadRequestException("Name of the drug can not be null");
            if(drug.getQuantity() < 1)
                throw new BadRequestException("Quantity can not be lower than one");
            if(drug.getPrice() <= 0)
                throw new BadRequestException("Price must be higher than zero");

            return purchaseCartRedisRepository.insertPurchaseDrug(id_pat, drug);
        }

        public PurchaseCartDrugDTO deletePurchaseDrug(String id_pat, int id_purch_drug) {
            if(id_pat == null || id_pat.isEmpty())
                throw new BadRequestException("The patient id can not be null");
            return purchaseCartRedisRepository.deletePurchaseDrug(id_pat, id_purch_drug);
        }

        public PurchaseCartDrugDTO modifyPurchaseDrugQuantity(String id_pat, int id_purch_drug, int quantity) {
            if(id_pat == null || id_pat.isEmpty())
                throw new BadRequestException("The patient id can not be null");
            if(quantity == 0)
                return purchaseCartRedisRepository.deletePurchaseDrug(id_pat, id_purch_drug);
            else if(quantity < 0)
                throw new BadRequestException("Quantity can not be lower that zero.");
            return purchaseCartRedisRepository.modifyPurchaseDrugQuantity(id_pat, id_purch_drug, quantity);
        }

        // funzione usata per inserire un acquisto di farmaci all'interno di mongo db
        private NewPurchaseDTO insertPurchases(String patientCode, String pharmacyRegion, List<PurchaseCartDrugDTO> purchasedDrugs){
            List<ObjectId> prescribedDrugsId =  new ArrayList<>();
            List<ObjectId> purchaseDrugsId =  new ArrayList<>();
            LocalDateTime currentTimestamp = LocalDateTime.now();
            LatestPurchase latestPurchase = new LatestPurchase();
            List<String> purchaseIds = new ArrayList<>();

            for(PurchaseCartDrugDTO purchaseDrugDTO : purchasedDrugs){
                // creo, per ogni farmaco acquistato, il documento da inserire nella collezione purchases
                Purchase purchase = new Purchase();

                ObjectId objectIdDrug = new ObjectId(purchaseDrugDTO.getIdDrug());
                purchase.setDrugId(String.valueOf(objectIdDrug));

                purchase.setName(purchaseDrugDTO.getName());
                purchase.setQuantity(purchaseDrugDTO.getQuantity());
                purchase.setPrice(purchaseDrugDTO.getPrice());
                purchase.setPrescriptionDate(purchaseDrugDTO.getPrescriptionTimestamp());
                purchase.setPurchaseDate(currentTimestamp);
                purchase.setRegion(pharmacyRegion);

                // inserisco nella collezione purchase il farmaco acquistato
                String idPurchase = purchaseRepository.save(purchase).getId();
                purchaseIds.add(idPurchase);

                ObjectId purchObjectId = new ObjectId(idPurchase);
                purchaseDrugsId.add(purchObjectId);
                if(purchase.getPrescriptionDate() != null) prescribedDrugsId.add(purchObjectId);

                // creo, per ogni farmaco acquistato, il documento da inserire nella collezione patients
                LatestDrug latestDrug = new LatestDrug();

                ObjectId objectIdPurchase = new ObjectId(purchaseDrugDTO.getIdDrug());
                latestDrug.setDrugId(String.valueOf(objectIdPurchase));

                latestDrug.setDrugName(purchaseDrugDTO.getName());
                latestDrug.setQuantity(purchaseDrugDTO.getQuantity());
                latestDrug.setPrice(purchaseDrugDTO.getPrice());
                latestDrug.setPrescriptionDate(purchaseDrugDTO.getPrescriptionTimestamp());
                latestPurchase.getDrugs().add(latestDrug);
            }

            latestPurchase.setTimestamp(currentTimestamp);

            // seleziono il paziente col codice in esame
            Query query = new Query(Criteria.where("_id").is(patientCode));

            // Rimuove l'ultimo elemento e aggiunge il nuovo in prima posizione se ha già 5 farmaci
            Optional<Patient> optPatient = patientRepository.findById(patientCode);
            Patient patient;
            int nDrugs = 0;

            if(optPatient.isPresent()) {
                patient = optPatient.get();
                nDrugs = patient.getLatestPurchasedDrugs().size();
            }

            if (nDrugs >= 5) {
                // PRIMO UPDATE: Rimuove l'ultimo elemento
                Update popUpdate = new Update().pop("latestPurchasedDrugs", Update.Position.LAST);
                mongoTemplate.updateFirst(query, popUpdate, Patient.class);
            }

            Update pushUpdate = new Update().push("latestPurchasedDrugs").atPosition(0).value(latestPurchase);
            mongoTemplate.updateFirst(query, pushUpdate, Patient.class);

            // aggiorno le liste "purchases" e "prescriptions"
            Update updateLists = new Update()
                    .push("purchases").each(purchaseDrugsId.toArray())
                    .push("prescriptions").each(prescribedDrugsId.toArray());

            mongoTemplate.updateFirst(query, updateLists, Patient.class);
            NewPurchaseDTO newPurchaseDTO = new NewPurchaseDTO();
            newPurchaseDTO.setLatestPurchase(latestPurchase);
            newPurchaseDTO.setPurchaseIds(purchaseIds);
            return newPurchaseDTO;
        }

        private void confirmPurchaseCart(String id_pat, ConfirmPurchaseCartDTO cartDTO, String idLog) throws JedisException {
            final String CONFIRM_PURCHAE_SCRIPT =
            """
                    local id_pat = ARGV[1]
                    local purchaseDrugsJson = ARGV[2]
                    local presToDeleteJson = ARGV[3]
                    local presToModifyJson = ARGV[4]
                    local newToPurchaseJson = ARGV[5]
                    local id_log = ARGV[6]
                    local purchaseDrugs = cjson.decode(purchaseDrugsJson)
                    local presToDelete = cjson.decode(presToDeleteJson)
                    local presToModify = cjson.decode(presToModifyJson)
                    local newToPurchase = cjson.decode(newToPurchaseJson)
            
                    -- 1. Eliminazione farmaci acquistati dal carrello
                    for _, drug in ipairs(purchaseDrugs) do
                        local key = "purch-drug:" .. drug.idPurchDrug .. ":" .. id_pat .. ":"
                        redis.call("DEL", key .. "id")
                        redis.call("DEL", key .. "info")
                        redis.call("LPUSH", "available_purch-drug_ids", drug.idPurchDrug)
                    end
            
                    -- 2. Eliminazione della lista di farmaci nel carrello
                    redis.call("DEL", "purch-drug:" .. id_pat .. ":set")
            
                    -- 3. Eliminazione prescrizioni completate
                    for presId, drugList in pairs(presToDelete) do
                        local keyPres = "pres:" .. presId .. ":" .. id_pat .. ":"
                        local keyPresList = "pres:" .. id_pat .. ":set"
                        redis.call("DEL", keyPres .. "timestamp")
                        redis.call("DEL", keyPres .. "toPurchase")
                        redis.call("SREM", keyPresList, presId)
                        redis.call("LPUSH", "available_pres_ids", presId)
            
                        for _, id_pres_drug in ipairs(drugList) do
                            local keyPresDrug = "pres-drug:" .. id_pres_drug .. ":" .. presId .. ":"
                            local keyPresDrugList = "pres-drug:" .. presId .. ":set"
                            redis.call("DEL", keyPresDrug .. "id")
                            redis.call("DEL", keyPresDrug .. "info")
                            redis.call("DEL", keyPresDrug .. "purchased")
                            redis.call("DEL", keyPresDrugList)
                            redis.call("LPUSH", "available_pres-drug_ids", id_pres_drug)
                        end
                    end
            
                    -- 4. Aggiornamento prescrizioni parziali
                    for presId, drugList in pairs(presToModify) do
                        local keyPres = "pres:" .. presId .. ":" .. id_pat .. ":"
                        redis.call("SET", keyPres .. "toPurchase", newToPurchase[tostring(presId)])
            
                        for _, id_pres_drug in ipairs(drugList) do
                            local keyPresDrug = "pres-drug:" .. id_pres_drug .. ":" .. presId .. ":"
                            redis.call("SET", keyPresDrug .. "purchased", "true")
                        end
                    end
            
                    -- 5. Conferma modifiche su Redis effettuate
                    redis.call("SET", "log:" .. id_log, "ok")
                    redis.call("EXPIRE", "log:" .. id_log, 3600)
                    return "OK"
            """;

            // Eseguiamo lo script LUA poiché così abbiamo la certezza che avvenga in maniera atomica anche
            // se dovesse accadere un problema di connessione col master.
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();
            String purchaseDrugJson = gson.toJson(cartDTO.getPurchaseDrugs());
            String presToDeleteJson = gson.toJson(cartDTO.getPresToDelete());
            String presToModifyJson = gson.toJson(cartDTO.getPresToModify());
            String newToPurchaseJson = gson.toJson(cartDTO.getNewToPurchase());
            String sha1 = cartDTO.getJedis().scriptLoad(CONFIRM_PURCHAE_SCRIPT);
            cartDTO.getJedis().evalsha(sha1, Collections.emptyList(),
                    Arrays.asList(id_pat, purchaseDrugJson, presToDeleteJson, presToModifyJson, newToPurchaseJson, idLog));
        }

        @Transactional
        public NewPurchaseDTO insertPurchasesTransaction(String id_pat, String id_pharm, List<PurchaseCartDrugDTO> purchasedDrugs,
                                               CommitLog log) {
            Optional<Pharmacy> optPharmacy = pharmacyRepository.findById(id_pharm);
            String pharmacyRegion = null;
            if(optPharmacy.isPresent())
                pharmacyRegion = optPharmacy.get().getRegion();
            else throw new BadRequestException("Pharmacy " + id_pharm + " does not exist");

            NewPurchaseDTO newPurchaseDTO = insertPurchases(id_pat, pharmacyRegion, purchasedDrugs);

            // se l'inserimento su mongo db è andato tutto apposto, inseriamo le modifiche fatte su
            // un commit log per ricordarcele se effettivamente dovremo effettuare un rollback
            log.getPurchaseIds().addAll(newPurchaseDTO.getPurchaseIds());
            log.setPatientId(id_pat);
            log.setOperationType("DELETE");
            log.setTimestamp(LocalDateTime.now());
            log.setProcessed(false);
            commitLogRepository.save(log);
            return newPurchaseDTO;
        }

        @Retryable(
                retryFor = { DataAccessException.class, TransactionSystemException.class,
                        JedisException.class, RetryException.class },
                maxAttempts = 3,
                backoff = @Backoff(delay = 2000)
        )
        public LatestPurchase confirmPurchase(String id_pat, String id_pharm) {
            if(id_pat == null || id_pat.isEmpty())
                throw new BadRequestException("The patient id can not be null");

            Jedis jedis = null;
            CommitLog log = new CommitLog();
            NewPurchaseDTO newPurchaseDTO = new NewPurchaseDTO();

            try {
                attempt++;
                System.out.println("Tentativo numero: " + attempt);

                ConfirmPurchaseCartDTO confirmPurchaseCartDTO = purchaseCartRedisRepository.confirmPurchaseCart(id_pat);
                List<PurchaseCartDrugDTO> purchasedDrugs = confirmPurchaseCartDTO.getPurchaseDrugs();

                // eseguiamo la transazione atomica di MongoDB
                newPurchaseDTO = insertPurchasesTransaction(id_pat, id_pharm, purchasedDrugs, log);

                // Eseguiamo le modifiche su Redis in modo atomico utilizzando uno script Lua
                confirmPurchaseCart(id_pat, confirmPurchaseCartDTO, log.getId());
                jedis = confirmPurchaseCartDTO.getJedis();

                //Se tutto funziona correttamente, contrassegnamo come non necessario il rollback su mongo
                try{
                    log.setProcessed(true);
                    commitLogRepository.save(log);
                } catch (Exception exc){
                    // lancio un'eccezione che non mi fa fare il retry del metodo, dato che in realtà tutte le
                    // operazioni sono andati a buon fine
                    throw new IllegalStateException("Not possible to update the commit_log");
                }

                return newPurchaseDTO.getLatestPurchase();

            } catch (JedisException e) {
                // Errore durante la transazione di Redis: viene provato il rollback su Mongo DB
                try {
                    System.out.println("id: " + newPurchaseDTO.getPurchaseIds() + " log: " + log);
                    rollbackProcessor.rollbackPurchases(newPurchaseDTO.getPurchaseIds(), log);
                    throw new RetryException("Retry to do the operations after that Mongo rollback succeeded", e);

                // se ho problemi con il rollback di Mongo
                } catch (MongoException ex){
                    throw new TransactionSystemException("Retry to do the operations after the error in the Mongo rollback", ex);
                }
            } finally {
                // viene restituito il pool di connessione
                if(jedis != null)
                    jedis.close();
            }
        }

        public Pharmacy getPharmacyById(String id) {
            return (Pharmacy) userService.getUserIfExists(id, UserType.PHARMACY);
        }

        public Pharmacy modifyPharmacy(Pharmacy pharmacy) {
            if(pharmacyRepository.existsById(pharmacy.getId())) {
                Pharmacy pharmacy_ = getPharmacyById(pharmacy.getId());
                pharmacy_.setOwnerTaxCode(pharmacy.getOwnerTaxCode());
                if(pharmacy.getPassword() != null){
                    String hash = PasswordHasher.hash(pharmacy.getPassword());
                    pharmacy_.setPassword(hash);
                }
                pharmacyRepository.save(pharmacy_);
                return pharmacy_;
            } else throw new NotFoundException("Pharmacy "+pharmacy.getId()+" does not exist");
        }

        public Pharmacy deletePharmacy(String id) {
            Pharmacy pharmacy = getPharmacyById(id);
            pharmacyRepository.deleteById(id);
            return pharmacy;
        }
}
