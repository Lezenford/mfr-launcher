package ru.fullrest.mfr.plugins_configuration_utility;

import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PluginsConfigurationUtilityApplication.class)
public class PluginsConfigurationUtilityApplicationTests {

    @BeforeClass
    public static void bootstrapJavaFx() {
        // implicitly initializes JavaFX Subsystem
        // see http://stackoverflow.com/questions/14025718/javafx-toolkit-not-initialized-when-trying-to-play-an-mp3
        // -file-through-mediap
        new JFXPanel();
    }

    @Test
    public void contextLoads() {
    }

}
