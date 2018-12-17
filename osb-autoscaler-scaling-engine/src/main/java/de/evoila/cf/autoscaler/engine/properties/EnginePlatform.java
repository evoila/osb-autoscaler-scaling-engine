package de.evoila.cf.autoscaler.engine.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "engine.platforms")
public class EnginePlatform {

    private List<String> supported;

    public List<String> getSupported() {
        return supported;
    }

    public void setSupported(List<String> supported) {
        this.supported = supported;
    }
}
