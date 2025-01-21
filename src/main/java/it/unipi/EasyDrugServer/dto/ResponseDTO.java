package it.unipi.EasyDrugServer.dto;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResponseDTO {
    private final int status;
    private Object data;

    public ResponseDTO(HttpStatus status, Object data) {
        this.status = status.value();
        this.data = data;
    }

    public ResponseDTO(HttpStatus status) {
        this.status = status.value();
    }
}
