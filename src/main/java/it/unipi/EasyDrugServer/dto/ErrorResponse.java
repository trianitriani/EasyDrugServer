package it.unipi.EasyDrugServer.dto;

import org.springframework.http.HttpStatus;

public class ErrorResponse {
    private final int status;
    private final String message;

    public ErrorResponse(HttpStatus status, String message) {
        this.status = status.value();
        this.message = message;
    }
}
