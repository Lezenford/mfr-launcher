package ru.fullrest.mfr.plugins_configuration_utility.model.repository;

import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication;

import static org.junit.Assert.assertNotNull;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PluginsConfigurationUtilityApplication.class)
public class GroupRepositoryTest {

    @Autowired
    private GroupRepository repository;

    @BeforeClass
    public static void bootstrapJavaFx() {
        new JFXPanel();
    }

    @Test
    public void getAll() {
        assertNotNull(repository.findAll());
    }
}