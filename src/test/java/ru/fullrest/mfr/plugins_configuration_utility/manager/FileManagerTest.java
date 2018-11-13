package ru.fullrest.mfr.plugins_configuration_utility.manager;

import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PluginsConfigurationUtilityApplication.class)
public class FileManagerTest {

    @Autowired
    private FileManager fileManager;

    @BeforeClass
    public static void bootstrapJavaFx() {
        new JFXPanel();
    }

    @Before
    public void createFileManager() {
//        fileManager.setGamePath("C:\\Users\\Famaly\\Desktop\\Новая папка");
    }

    @Test
    public void test() {

    }

//    @Test
//    public void removeFromGameDirectory() {
//        Details details = new Details();
//        details.setGamePath("1.txt");
//        fileManager.removeFromGameDirectory(Collections.singletonList(details));
//        assertFalse(new File("C:\\Users\\Famaly\\Desktop\\Новая папка\\1.txt").exists());
//    }
//
//    @Test
//    public void copyToGameDirectory() {
//        Details details = new Details();
//        details.setGamePath("1.txt");
//        details.setStoragePath("1.txt");
//        fileManager.copyToGameDirectory(Collections.singletonList(details));
//        assertTrue(new File("C:\\Users\\Famaly\\Desktop\\Новая папка\\1.txt").exists());
//    }
}