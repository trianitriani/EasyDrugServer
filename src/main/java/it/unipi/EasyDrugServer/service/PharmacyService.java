package it.unipi.EasyDrugServer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.BadRequestException;
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
        public NewPurchaseDTO insertPurchases(String patientCode, String pharmacyRegion, List<PurchaseCartDrugDTO> purchasedDrugs){
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

            Optional<Patient> optPatient = patientRepository.findById(patientCode);
            if (optPatient.isEmpty())
                throw new NotFoundException("Patient not found: " + patientCode);
            Patient patient = optPatient.get();

            // Se ha già 5 farmaci, rimuove l'ultimo
            List<LatestPurchase> latestDrugs = new ArrayList<>(patient.getLatestPurchasedDrugs());
            if (latestDrugs.size() >= 5)
                latestDrugs.remove(latestDrugs.size() - 1);

            // Aggiunge il nuovo farmaco in prima posizione
            latestDrugs.add(0, latestPurchase);
            patient.setLatestPurchasedDrugs(latestDrugs);

            // Converti ObjectId in String
            List<String> purchaseDrugsIdStr = purchaseDrugsId.stream()
                    .map(ObjectId::toHexString)
                    .toList();

            List<String> prescribedDrugsIdStr = prescribedDrugsId.stream()
                    .map(ObjectId::toHexString)
                    .toList();

            // Aggiorna le liste "purchases" e "prescriptions"
            patient.getPurchases().addAll(purchaseDrugsIdStr);
            patient.getPrescriptions().addAll(prescribedDrugsIdStr);

            // Salva il paziente aggiornato
            patientRepository.save(patient);

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
                        
                        local keyPresDrugList = "pres-drug:" .. presId .. ":set"
                        for _, id_pres_drug in ipairs(drugList) do
                            local keyPresDrug = "pres-drug:" .. id_pres_drug .. ":" .. presId .. ":"
                            redis.call("DEL", keyPresDrug .. "id")
                            redis.call("DEL", keyPresDrug .. "info")
                            redis.call("DEL", keyPresDrug .. "purchased")
                            redis.call("LPUSH", "available_pres-drug_ids", id_pres_drug)
                        end
                        redis.call("DEL", keyPresDrugList)
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
                             RetryException.class },
                maxAttempts = 3,
                backoff = @Backoff(delay = 2000)
        )
        public LatestPurchase confirmPurchase(String id_pat, String id_pharm) {
            if(id_pat == null || id_pat.isEmpty())
                throw new BadRequestException("The patient id can not be null");

            Jedis jedis = null;
            CommitLog log = new CommitLog();
            NewPurchaseDTO newPurchaseDTO;

            try {
                // Lettura delle informazioni dei farmaci da comprare
                ConfirmPurchaseCartDTO confirmPurchaseCartDTO = purchaseCartRedisRepository.confirmPurchaseCart(id_pat);

                List<PurchaseCartDrugDTO> purchasedDrugs = confirmPurchaseCartDTO.getPurchaseDrugs();
                // eseguiamo la transazione atomica di MongoDB
                newPurchaseDTO = insertPurchasesTransaction(id_pat, id_pharm, purchasedDrugs, log);

                // Eseguiamo le modifiche su Redis in modo atomico utilizzando uno script Lua
                confirmPurchaseCart(id_pat, confirmPurchaseCartDTO, log.getId());
                jedis = confirmPurchaseCartDTO.getJedis();

                return newPurchaseDTO.getLatestPurchase();

            } catch (JedisException e) {
                // non viene effettuato il rollback perché non abbiamo la sicurezza che le informazioni
                // su redis non siano state eseguite realmente
                throw new RetryException(e.getMessage());
                
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
