package com.example.bankingprojectfinal.Exception;

public class UserHasBeenDisabledException extends RuntimeException {
    public UserHasBeenDisabledException(String message) {
        super(message);
    }
}
