package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.LoginUserDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.dto.SessionUserDTO;
import it.unipi.EasyDrugServer.dto.SignupUserDTO;
import it.unipi.EasyDrugServer.exception.*;
import it.unipi.EasyDrugServer.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final GlobalExceptionHandler exceptionHandler;

    @Operation(summary = "Sign-up", description = "Create a new account if it doesn't exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New account created successfully."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error: Mandatory fields omitted; unknown type of data; invalid date of birth."),
            @ApiResponse(responseCode = "403", description = "Server refuse client request because violate business logic: account already existing; patient's doctor not existing."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PostMapping("/signup")
    public ResponseEntity<ResponseDTO> signup(@RequestBody  @Parameter(name = "user", description = "Data extracted from the sign-up form.", example = "") SignupUserDTO user) {
        try {
            SessionUserDTO sessionUserDTO = authService.signup(user);
            ResponseDTO response = new ResponseDTO(HttpStatus.CREATED, sessionUserDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            return exceptionHandler.handleBadRequestException(e);
        } catch (ForbiddenException e){
            return exceptionHandler.handleForbiddenException(e);
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }

    @Operation(summary = "Login", description = "Give access to the application using valid and existing credential: identify code and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request succeeded: access granted."),
            @ApiResponse(responseCode = "400", description = "Not processable request due to a client error: wrong password or not existing account; unknown type of data; identify code or password cannot be null."),
            @ApiResponse(responseCode = "404", description = "Server cannot find the requested resource (valid endpoint but resource doesn't exist)."),
            @ApiResponse(responseCode = "500", description = "Server encountered a situation it does not know how to handle (generic error)."),
            @ApiResponse(responseCode = "503", description = "Server not ready to handle request (maintenance or overloaded).")
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO> login(@RequestBody @Parameter(name = "user", description = "Struct conteining identify code and password.", example = "") LoginUserDTO user){
        try {
            SessionUserDTO sessionUserDTO = authService.login(user);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, sessionUserDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e){
            return exceptionHandler.handleBadRequestException(e);
        } catch (NotFoundException | UnauthorizedException e) {
            NotFoundException b=new NotFoundException("Identify code or password  wrong or not existing account");
            return exceptionHandler.handleNotFoundException(b);//Login rule: user should not know if the combination account-password exist (brute force attack)
        } catch (MongoSocketException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MongoException e) {
            return exceptionHandler.handleMongoDBException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e){
            return exceptionHandler.handleException(e);
        }
    }
}
