package ru.fullrest.mfr.server.mvc;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Data
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AdminSession {
    private ContentType contentType;
    private String platform;

    public void setParameters(ContentType contentType, String platform) {
        this.contentType = contentType;
        this.platform = platform;
    }
}