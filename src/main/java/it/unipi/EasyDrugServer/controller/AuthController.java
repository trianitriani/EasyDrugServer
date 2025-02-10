package it.unipi.EasyDrugServer.controller;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import it.unipi.EasyDrugServer.dto.LoginUserDTO;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import it.unipi.EasyDrugServer.dto.SessionUserDTO;
import it.unipi.EasyDrugServer.dto.SignupUserDTO;
import it.unipi.EasyDrugServer.exception.BadRequestException;
import it.unipi.EasyDrugServer.exception.ForbiddenException;
import it.unipi.EasyDrugServer.exception.GlobalExceptionHandler;
import it.unipi.EasyDrugServer.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final GlobalExceptionHandler exceptionHandler;

    @PostMapping("/signup")
    public ResponseEntity<ResponseDTO> signup(@RequestBody SignupUserDTO user) {
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

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO> login(@RequestBody LoginUserDTO user){
        try {
            SessionUserDTO sessionUserDTO = authService.login(user);
            ResponseDTO response = new ResponseDTO(HttpStatus.OK, sessionUserDTO);
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
}
