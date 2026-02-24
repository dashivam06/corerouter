package com.fleebug.corerouter.exception.documentation;

public class DocumentationNotFoundException extends RuntimeException {
    public DocumentationNotFoundException(Integer docId) {
        super("Documentation with ID '" + docId + "' not found");
    }

    public DocumentationNotFoundException(String message) {
        super(message);
    }
}
