package com.example.bankingprojectfinal.Exception;

public class PasswordsMatchException extends RuntimeException {
    public PasswordsMatchException() {
        super("New password cannot be the same as old password");
    }
}
