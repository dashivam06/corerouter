package com.fleebug.corerouter.exception.task;

/**
 * Thrown when a task with the given ID is not found.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(String taskId) {
        super("Task with ID '" + taskId + "' not found");
    }
}
