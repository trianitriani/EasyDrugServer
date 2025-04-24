package it.unipi.EasyDrugServer.exception;

import com.mongodb.MongoException;
import it.unipi.EasyDrugServer.dto.ResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import redis.clients.jedis.exceptions.JedisException;

@Component
public class GlobalExceptionHandler {
    public static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseDTO> handleNotFoundException(NotFoundException ex) {
        logger.warn("NotFoundException: {}", ex.getMessage());
        ResponseDTO error = new ResponseDTO(HttpStatus.NOT_FOUND, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseDTO> handleUnauthorizedException(UnauthorizedException ex) {
        logger.warn("UnauthorizedException: {}", ex.getMessage());
        ResponseDTO error = new ResponseDTO(HttpStatus.UNAUTHORIZED, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseDTO> handleForbiddenException(ForbiddenException ex) {
        logger.warn("ForbiddenException: {}", ex.getMessage());
        ResponseDTO error = new ResponseDTO(HttpStatus.FORBIDDEN, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseDTO> handleBadRequestException(BadRequestException ex) {
        logger.warn("BadRequestException: {}", ex.getMessage());
        ResponseDTO error = new ResponseDTO(HttpStatus.BAD_REQUEST, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<ResponseDTO> handleRedisException(String message, HttpStatus status){
        logger.warn("RedisException: {}", message);
        ResponseDTO error = new ResponseDTO(status, message);
        return new ResponseEntity<>(error, status);
    }

    public ResponseEntity<ResponseDTO> handleMongoDBException(MongoException ex, HttpStatus status){
        logger.warn("MongoException: {}", ex.getMessage());
        ResponseDTO error = new ResponseDTO(status, ex.getMessage());
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleException(Exception ex) {
        logger.warn("Exception: {}", ex.getMessage());
        ResponseDTO error = new ResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
