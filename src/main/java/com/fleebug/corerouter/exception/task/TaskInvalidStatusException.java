package com.fleebug.corerouter.exception.task;

/**
 * Thrown when attempting invalid task status transitions or operations on wrong status
 */
public class TaskInvalidStatusException extends RuntimeException {

    public TaskInvalidStatusException(String taskId, String currentStatus, String attemptedStatus) {
        super("Invalid status transition for task '" + taskId + "' from '" + currentStatus + "' to '" + attemptedStatus + "'");
    }

    public TaskInvalidStatusException(String message) {
        super(message);
    }
}