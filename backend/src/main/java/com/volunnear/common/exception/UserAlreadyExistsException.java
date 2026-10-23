package com.volunnear.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data
@Getter
@EqualsAndHashCode(callSuper = true)
public class UserAlreadyExistsException extends RuntimeException {
    private final String field;

    public UserAlreadyExistsException(String field, String message) {
        super(message);
        this.field = field;
    }
}
