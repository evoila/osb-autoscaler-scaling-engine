package de.evoila.cf.autoscaler.engine.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "engine")
public class EngineBean {

    private EnginePlatform enginePlatform;

    public EnginePlatform getEnginePlatform() {
        return enginePlatform;
    }

    public void setEnginePlatform(EnginePlatform enginePlatform) {
        this.enginePlatform = enginePlatform;
    }
}
