package com.volunnear.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BadDataInRequestException extends RuntimeException {
    public BadDataInRequestException(String message) {
        super(message);
    }
}
