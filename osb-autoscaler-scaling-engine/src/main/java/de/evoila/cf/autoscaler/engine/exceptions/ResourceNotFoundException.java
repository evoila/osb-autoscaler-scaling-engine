package de.evoila.cf.autoscaler.engine.exceptions;

public class ResourceNotFoundException extends Exception {

    public ResourceNotFoundException(String resource_guid) {
        super("Could not find a resource with the id '"+resource_guid+"'.");
    }
}
