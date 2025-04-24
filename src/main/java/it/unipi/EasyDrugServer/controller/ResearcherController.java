package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import it.unipi.EasyDrugServer.dto.*;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.exception.NotFoundException;
import it.unipi.EasyDrugServer.model.Researcher;
import it.unipi.EasyDrugServer.service.ResearcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/api/researchers")
@RequiredArgsConstructor
public class ResearcherController {
    private final ResearcherService researcherService;
    private final GlobalExceptionHandler exceptionHandler;

    @Operation(summary = "Get researcher by id.", description = "Fetch the researcher's private information using their unique identify code: identify code, password, city, district, region, name, surname, date of birth, gender, tax code, researcher register code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: researcher data retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Researcher not found."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getResearcherById(@PathVariable("id") @Parameter(name = "Identify code", description = "Researcher identify code.", example = "RDNGDNL81C03E402P") String id){
        try {
            Researcher researcher = researcherService.getResearcherById(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, researcher);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (NotFoundException e) {
            return exceptionHandler.handleNotFoundException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @Operation(summary = "Update researcher information", description = "Modify the researcher's private information, including city, district, region, and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: researcher private area modified."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyResearcher(@RequestBody @Parameter(name = "Researcher struct", description = "Researcher private area") Researcher researcher){
        try {
            Researcher researcher_ = researcherService.modifyResearcher(researcher);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, researcher_);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @Operation(summary = "Delete researcher account", description = "Permanently remove a researcher's account from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: researcher account successfully deleted."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteResearcher(@PathVariable("id") @Parameter(name = "Identify code", description = "Researcher identify code.", example = "RDNGDNL81C03E402P") String id){
        try {
            Researcher researcher = researcherService.deleteResearcher(id);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, researcher);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @Operation(summary = "Get patients/doctors ratio", description = "Obtain the ratio of patients to doctors for each region in Italy, sorted by the specified order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: list of regional ratio returned."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/ratios/patients-to-doctors/order/{order}")
    public ResponseEntity<ResponseDTO> getPatientsToDoctorsRatio(@PathVariable("order") @Parameter(name = "Sorting order", description = "Sorting order: ASC (ascending), DESC (descending).", example = "ASC") Order order) {
        try {
            List<PatientDoctorRatioDTO> list = researcherService.getPatientsToDoctorsRatio(order);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, list);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
    @Operation(summary = "Get most purchased drugs", description = "Retrieve the top N most frequently purchased drugs within a specified date range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: list of most purchased drugs returned."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/purchases/top/{top}/from/{from}/to/{to}")
    public ResponseEntity<ResponseDTO> getTopPurchases(@PathVariable("top") @Parameter(name = "Top", description = "Number of top-purchased drugs to retrieve.", example = "50") int top,
                                                       @PathVariable("from") @Parameter(name = "Start date", description = "Start date for filtering purchases (YYYY-MM-DD format).", example = "2024-10-10") LocalDateTime from,
                                                       @PathVariable("to") @Parameter(name = "End date", description = "End date for filtering purchases (YYYY-MM-DD format).", example = "2024-12-12") LocalDateTime to){
        try{
            List<TopDrugDTO> topDrugs = researcherService.getTopPurchases(top, from, to);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, topDrugs);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
    @Operation(summary = "Get drug distribution data", description = "Fetch distribution statistics for a specific drug within a given time frame, ordered by the specified criteria.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: drug statistics returned."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Server cannot find the requested resource (valid endpoint but resource doesn't exist)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/drugs/{id_drug}/distribution/from/{from}/to/{to}/order/{order}")
    public ResponseEntity<ResponseDTO> getDistributionByDrug(@PathVariable("id_drug") @Parameter(name = "Drug id", description = "Drug id.", example = "67aba9215da6705a000d3f45") String id_drug,
                                                             @PathVariable("from") @Parameter(name = "Start date", description = "Start date for filtering purchases (YYYY-MM-DD format).", example = "2024-10-10") LocalDateTime from,
                                                             @PathVariable("to") @Parameter(name = "End date", description = "End date for filtering purchases (YYYY-MM-DD format).", example = "2024-12-12") LocalDateTime to,
                                                             @PathVariable("order") @Parameter(name = "Sorting order", description = "Sorting order: ASC (ascending), DESC (descending).", example = "ASC") Order order){
        try {
            List<DrugDistributionDTO> list = researcherService.getDistributionByDrug(id_drug, order, from, to);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, list);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotFoundException e) {
            return exceptionHandler.handleNotFoundException(e);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    // Il ricercatore può vedere per quali indicazioni ci sono meno farmaci (top 15) e
    // mostrare quanti farmaci e quali farmaci per ognuna.
    @Operation(summary = "Get underrepresented indications", description = "Fetch the top N indications associated with the fewest available drugs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: list of indication returned."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/indications/less-drugs/top/{top}")
    public ResponseEntity<ResponseDTO> getIndicationsWithLessDrugs(@PathVariable("top") @Parameter(name = "Top", description = "Number of top-indication to retrieve.", example = "10") int top){
        try{
            List<TopRareIndicationDTO> diseases = researcherService.getIndicationsWithLessDrugs(top);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, diseases);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
