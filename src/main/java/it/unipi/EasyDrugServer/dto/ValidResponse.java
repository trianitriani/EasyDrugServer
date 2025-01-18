package it.unipi.EasyDrugServer.dto;

import org.springframework.http.HttpStatus;

public class ValidResponse {
    private final int status;
    private Object data;

    public ValidResponse(HttpStatus status, Object data) {
        this.status = status.value();
        this.data = data;
    }

    public ValidResponse(HttpStatus status) {
        this.status = status.value();
    }
}
