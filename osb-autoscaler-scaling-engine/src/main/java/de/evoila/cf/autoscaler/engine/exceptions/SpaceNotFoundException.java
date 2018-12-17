package de.evoila.cf.autoscaler.engine.exceptions;

public class SpaceNotFoundException extends Exception {

    public SpaceNotFoundException(String space_guid) {
        super("Could not find a space with the id '"+space_guid+"'.");
    }
}
