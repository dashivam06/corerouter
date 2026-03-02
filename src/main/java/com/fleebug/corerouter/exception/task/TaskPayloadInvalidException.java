package com.fleebug.corerouter.exception.task;

/**
 * Thrown when task request payload is malformed or contains invalid data
 */
public class TaskPayloadInvalidException extends RuntimeException {

    public TaskPayloadInvalidException(String message) {
        super("Invalid task payload: " + message);
    }

    public TaskPayloadInvalidException(String field, String reason) {
        super("Invalid payload field '" + field + "': " + reason);
    }

    public TaskPayloadInvalidException(String message, Throwable cause) {
        super("Invalid task payload: " + message, cause);
    }
}