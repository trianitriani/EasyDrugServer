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
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.util.List;


@RestController
@RequestMapping("/api/researchers")
@RequiredArgsConstructor
public class ResearcherController {
    private final ResearcherService researcherService;
    private final GlobalExceptionHandler exceptionHandler;
    /*
    @PathVariable("") @Parameter(name = "", description = "", example = "")
    @RequestBody("") @Parameter(name = "", description = "", example = "")
    @PathVariable("id") @Parameter(name = "id", description = "Product id", example = "1") type var


    OK                          200     Request succeeded.
    CREATED                     201     New resource created successfully.
    BAD_REQUEST                 400     Not processable request due to a client error (malformed, invalid or deceptive syntax).
    UNAUTHORIZED                401     Client doesn't have access rights to the content (unauthorized).
    FORBIDDEN                   403     Server refuse client request because violate business logic.
    NOT_FOUND                   404     Server cannot find the requested resource (valid endpoint but resource doesn't exist).
    INTERNAL_SERVER_ERROR       500     Server encountered a situation it does not know ho to handle (generic error).
    SERVICE_UNAVAILABLE         503     Server not ready to handle request (maintenance or overloaded).

    @Operation(summary = "", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "201", description = "New resource created successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "401", description = "Client doesn't have access rights to the content (unauthorized)."),
            @ApiResponse(responseCode = "403", description = "Server refuse client request because violate business logic."),
            @ApiResponse(responseCode = "404", description = "Server cannot find the requested resource (valid endpoint but resource doesn't exist)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    */

    @Operation(summary = "Update researcher data.", description = "Update researcher private area: city, district, region and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: researcher private area modified."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PutMapping()
    public ResponseEntity<ResponseDTO> modifyResearcher(@RequestBody @Parameter(name = "researcher", description = "researcher private area") Researcher researcher){
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
    //tecnicamente qui c'è anche una not found sollevata che però viene gestita da internal server error (forse semplicemente perchè è impossibile che non si ritrobi l'account che si vuole modificare, visto che modifichi quello che hai davanti)

    @Operation(summary = "Delete researcher.", description = "Delete researcher account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteResearcher(@PathVariable("id") @Parameter(name = "id", description = "Researcher identify code.", example = "RDNGDNL81C03E402P") String id){
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

    /*
    Le delete non causano inconsistenza poichè:
    -tutti, tranne medico, non causa inconsistenze in DocumentDB
    -id medico è inserito come campo nel document del patient ma verrebbe usato solo dal medico stesso per ritrovare i suoi pazienti (e se si è eliminato l'account allora non può controllarli per antonomasia e quindi si può benissimo non mettere a null quell'attributo in patient. Ci penserà poi il paziente a cambiare medico e risolvere inconsistenza formale)
     */

    @Operation(summary = "Get researcher by id.", description = "Get researcher private area.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getResearcherById(@PathVariable("id") @Parameter(name = "id", description = "Researcher identify code.", example = "RDNGDNL81C03E402P") String id){
        try {
            Researcher researcher = researcherService.getResearcherById(id);
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
    @Operation(summary = "Get patients/doctors ratio", description = "Get patients/doctors ratio for every region in Italy")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/ratios/patients-to-doctors/order/{order}")
    public ResponseEntity<ResponseDTO> getPatientsToDoctorsRatio(@PathVariable("order") @Parameter(name = "order", description = "sorting order", example = "ASC") Order order) {
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
    @Operation(summary = "Get most purchased drugs", description = "Get first n-elements in the list of most purchased drugs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/purchases/top/{top}/from/{from}/to/{to}")
    public ResponseEntity<ResponseDTO> getTopPurchases(@PathVariable("top") @Parameter(name = "top", description = "rank dimension", example = "50") int top,
                                                       @PathVariable("from") @Parameter(name = "from", description = "starting date", example = "") LocalDateTime from,
                                                       @PathVariable("to") @Parameter(name = "to", description = "ending date", example = "") LocalDateTime to){
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
    @Operation(summary = "Get ", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "404", description = "Server cannot find the requested resource (valid endpoint but resource doesn't exist)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/drugs/{id_drug}/distribution/from/{from}/to/{to}/order/{order}")
    public ResponseEntity<ResponseDTO> getDistributionByDrug(@PathVariable("id_drug") @Parameter(name = "id_drug", description = "drug id", example = "67aba9215da6705a000d3f45") String id_drug,
                                                             @PathVariable("from") @Parameter(name = "from", description = "starting date", example = "") LocalDateTime from,
                                                             @PathVariable("to") @Parameter(name = "to", description = "ending date", example = "") LocalDateTime to,
                                                             @PathVariable("order") @Parameter(name = "order", description = "rank dimension", example = "ASC") Order order){
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
    @Operation(summary = "Get indications with less drugs.", description = "Get a list of n-indications with a low amount of drugs correlated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded:."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error (malformed, invalid or deceptive syntax)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know ho to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @GetMapping("/indications/less-drugs/top/{top}")
    public ResponseEntity<ResponseDTO> getIndicationsWithLessDrugs(@PathVariable("top") @Parameter(name = "top", description = "rank dimension", example = "10") int top){
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
