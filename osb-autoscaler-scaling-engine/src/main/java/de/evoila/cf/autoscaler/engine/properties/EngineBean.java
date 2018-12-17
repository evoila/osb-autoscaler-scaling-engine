package de.evoila.cf.autoscaler.engine.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "engine")
public class EngineBean {

    private EnginePlatform platforms;

    public EnginePlatform getPlatforms() {
        return platforms;
    }

    public void setPlatforms(EnginePlatform platforms) {
        this.platforms = platforms;
    }
}
