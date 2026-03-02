package com.fleebug.corerouter.exception.task;

/**
 * Thrown when a task exceeds its maximum retry attempts
 */
public class TaskRetryExceededException extends RuntimeException {

    public TaskRetryExceededException(String taskId, int maxRetries) {
        super("Task '" + taskId + "' exceeded maximum retry limit of " + maxRetries + " attempts");
    }

    public TaskRetryExceededException(String taskId, int currentRetry, int maxRetries, String lastError) {
        super("Task '" + taskId + "' failed after " + currentRetry + "/" + maxRetries + " retries. Last error: " + lastError);
    }
}