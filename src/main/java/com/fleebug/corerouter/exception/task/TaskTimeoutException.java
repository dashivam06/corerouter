package com.fleebug.corerouter.exception.task;

/**
 * Thrown when a task exceeds its maximum processing time limit
 */
public class TaskTimeoutException extends RuntimeException {

    public TaskTimeoutException(String taskId, long timeoutMinutes) {
        super("Task '" + taskId + "' exceeded timeout limit of " + timeoutMinutes + " minutes");
    }

    public TaskTimeoutException(String taskId) {
        super("Task '" + taskId + "' timed out during processing");
    }
}