package de.evoila.cf.autoscaler.engine.exceptions;

public class OrgNotFoundException extends Exception {

    public OrgNotFoundException(String org_guid) {
        super("Could not find an organization with the id '"+org_guid+"'.");
    }
}
