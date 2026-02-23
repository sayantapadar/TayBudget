package com.example.taybudget.tools.exceptions;

public class NotInThreadException extends RuntimeException {
    public NotInThreadException() {
        super("Must run in a new thread from ThreadUtil");
    }
}
