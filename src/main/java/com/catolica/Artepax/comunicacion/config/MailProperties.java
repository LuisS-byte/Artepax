package com.catolica.Artepax.comunicacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for email sender settings.
 * <p>
 * Maps the {@code app.mail.*} prefix from application properties.
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Email address used as the sender for all outgoing messages. */
    private String from;

    /** Display name shown as the sender for all outgoing messages. */
    private String fromName;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
