package ru.fullrest.mfr.plugins_configuration_utility;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.fullrest.mfr.plugins_configuration_utility.javafx.StartApplicationInstruction;

/**
 * Created on 01.11.2018
 *
 * @author Alexey Plekhanov
 */
@Log4j2
public abstract class AbstractJavaFxApplicationSupport extends Application {
    private static String[] savedArgs;

    @Autowired
    private StartApplicationInstruction startApplicationInstruction;

    private ConfigurableApplicationContext context;

    static void launchApp(String[] args) {
        AbstractJavaFxApplicationSupport.savedArgs = args;
        Application.launch(PluginsConfigurationUtilityApplication.class, args);
    }

    @Override
    public void init() {
        context = SpringApplication.run(getClass(), savedArgs);
        context.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        context.close();
    }

    @Override
    public void start(Stage primaryStage) {
        startApplicationInstruction.startApplication(primaryStage, savedArgs);
    }
}