package ru.fullrest.mfr.server.model.entity.mock;

import lombok.Getter;
import ru.fullrest.mfr.server.model.entity.Application;

@Getter
public class ApplicationData {
    private final Application application;

    public ApplicationData() {
        application = new Application();
        application.setOperationSystem("Windows");
        application.setShortName("win");
        application.setPath("testApplication.zip");
        application.setVersion("1.0");
    }
}
