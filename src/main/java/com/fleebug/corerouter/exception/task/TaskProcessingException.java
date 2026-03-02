package com.fleebug.corerouter.exception.task;

/**
 * Thrown when task processing encounters an unrecoverable error
 */
public class TaskProcessingException extends RuntimeException {

    public TaskProcessingException(String taskId, String message) {
        super("Processing failed for task '" + taskId + "': " + message);
    }

    public TaskProcessingException(String taskId, String message, Throwable cause) {
        super("Processing failed for task '" + taskId + "': " + message, cause);
    }
}