package ru.fullrest.mfr.plugins_configuration_utility.manager;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.fullrest.mfr.plugins_configuration_utility.config.ConfigurationControllers;
import ru.fullrest.mfr.plugins_configuration_utility.config.StageControllers;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@Log4j2
@Component
public class FileManager {

    @Autowired
    private PropertiesRepository repository;

    @Autowired
    private ConfigurationControllers configurationControllers;

    @Autowired
    private StageControllers stageControllers;

    public static final String MORROWIND_EXE = "Morrowind.exe";

    public static final String MORROWIND_LAUNCHER_EXE = "Morrowind Launcher.exe";

    public static final String MORROWIND_MCP_EXE = "Morrowind Code Patch.exe";

    public static final String MORROWIND_MGE_EXE = "MGEXEgui.exe";

    public static final String MORROWIND_README = "Manual\\MFR_main_readme.xlsx";

    public static final String MORROWIND_OPTIONAL = "Optional";

    public static final String SEPARATOR = "\\";

    public static final String VERSION = "version";

    @Getter
    @Setter
    private String gamePath;

    public File openDirectoryChooser(boolean optionalFolder) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(getGamePathForChooser(optionalFolder));
        return directoryChooser.showDialog(stageControllers.getApplicationStage());
    }

    public List<File> openFilesChooser(boolean optionalFolder) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(getGamePathForChooser(optionalFolder));
        return fileChooser.showOpenMultipleDialog(stageControllers.getApplicationStage());
    }

    public File openFileChooser(boolean optionalFolder) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(getGamePathForChooser(optionalFolder));
        return fileChooser.showOpenDialog(stageControllers.getApplicationStage());
    }

    public String getGamePath(boolean withSeparator) {
        return withSeparator ? getGamePath() + SEPARATOR : getGamePath();
    }

    public String getOptionalPath(boolean withSeparator) {
        return withSeparator ? getGamePath() + SEPARATOR + MORROWIND_OPTIONAL + SEPARATOR :
               getGamePath() + SEPARATOR + MORROWIND_OPTIONAL;
    }

    private File getGamePathForChooser(boolean optionalFolder) {
        if (gamePath != null) {
            if (optionalFolder) {
                File file = new File(getOptionalPath(false));
                if (file.exists()) {
                    return file;
                } else {
                    return new File(gamePath);
                }
            } else {
                return new File(gamePath);
            }
        } else {
            return new File("");
        }
    }

    public boolean gameFolderCheck(File folder) {
        if (folder == null && gamePath == null) {
            System.exit(0); //При старте, если не выбрать каталог - приложение закрывается
        }
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().equals(MORROWIND_EXE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private File gameDirectoryChooseDialog() {
        while (true) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            if (gamePath != null) {
                directoryChooser.setInitialDirectory(new File(gamePath));
            }
            File file = directoryChooser.showDialog(stageControllers.getApplicationStage());
            if (gameFolderCheck(file)) {
                return file;
            }
        }
    }

    public void initGameDirectory() {
        try {
            File result = gameDirectoryChooseDialog();
            Properties properties = repository.findByKey(PropertyKey.GAME_DIRECTORY_PATH);
            properties.setValue(result.toString());
            repository.save(properties);
            gamePath = result.getAbsolutePath();
            configurationControllers.getMainView().getController().getGamePath().setText(repository.findByKey(PropertyKey.GAME_DIRECTORY_PATH).getValue());
            checkVersion();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkFileMD5(Details details) {
        File file = new File(getGamePath(true) + details.getGamePath());
        if (file.exists()) {
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(bufferedInputStream.readAllBytes());
                return Arrays.equals(digest.digest(), details.getMd5());
            } catch (NoSuchAlgorithmException | IOException e) {
                log.error("Ошибка проверки MD5 у файла " + file.getAbsolutePath());
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean checkMGEFilesForUnique() {
        try (BufferedInputStream bufferedInputStreamTop =
                     new BufferedInputStream(new FileInputStream(new File(getOptionalPath(true) + "MGE\\top_PK\\MGE" + ".ini"))); BufferedInputStream bufferedInputStreamMiddle = new BufferedInputStream(new FileInputStream(new File(getOptionalPath(true) + "MGE\\mid_PK\\MGE.ini"))); BufferedInputStream bufferedInputStreamLow = new BufferedInputStream(new FileInputStream(new File(getOptionalPath(true) + "MGE\\low_PK\\MGE.ini"))); BufferedInputStream bufferedInputStreamNekro = new BufferedInputStream(new FileInputStream(new File(getOptionalPath(true) + "MGE\\necro_PK\\MGE.ini"))); BufferedInputStream bufferedInputStreamCurrent = new BufferedInputStream(new FileInputStream(new File(getGamePath(true) + "mge3\\MGE.ini")))) {
            MessageDigest top = MessageDigest.getInstance("MD5");
            MessageDigest middle = MessageDigest.getInstance("MD5");
            MessageDigest low = MessageDigest.getInstance("MD5");
            MessageDigest necro = MessageDigest.getInstance("MD5");
            MessageDigest current = MessageDigest.getInstance("MD5");
            top.update(bufferedInputStreamTop.readAllBytes());
            middle.update(bufferedInputStreamMiddle.readAllBytes());
            low.update(bufferedInputStreamLow.readAllBytes());
            necro.update(bufferedInputStreamNekro.readAllBytes());
            current.update(bufferedInputStreamCurrent.readAllBytes());
            if (Arrays.equals(current.digest(), top.digest()) || Arrays.equals(current.digest(), middle.digest()) || Arrays.equals(current.digest(), low.digest()) || Arrays.equals(current.digest(), necro.digest())) {
                return false;
            } else {
                return true;
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean removeFromGameDirectory(Details details) {
        if (!Files.isWritable(Paths.get(getGamePath(true) + details.getGamePath()))) {
            log.error("Нет файла! " + details.getGamePath());
            return false;
        }
        try {
            Files.deleteIfExists(Paths.get(getGamePath(true) + details.getGamePath()));
        } catch (IOException e) {
            log.error("removeFromGameDirectory " + e.getMessage());
            return false;
        }
        return true;
    }

    public void copyToGameDirectory(Details details) {
        try {
            File file = new File(getGamePath(true) + details.getGamePath());
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            Files.copy(Paths.get(getOptionalPath(true) + details.getStoragePath()),
                    Paths.get(getGamePath(true) + details.getGamePath()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("copyToGameDirectory " + e.getMessage() + " " + e);
        }
    }

    public InputStream getInputStreamFromFile(String path) {
        if (path != null) {
            try {
                return Files.newInputStream(Paths.get(getGamePath(true) + path));
            } catch (IOException e) {
                log.error("getInputStreamFromFile " + e.getMessage());
            }
        }
        return null;
    }

    public String getAbsolutePath(String path) {
        return "\"" + getGamePath(true) + path + "\"";
    }

    public void checkVersion() {
        if (gamePath != null) {
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(getOptionalPath(true) + VERSION))))) {
                if (reader.ready()) {
                    configurationControllers.getMainView().getController().getVersion().setText("Версия: " + reader.readLine());
                }
            } catch (IOException e) {
                log.error("checkVersion " + e.getMessage());
            }
        }
    }

    public boolean saveMGEBackup() {
        File mgeDirectory = new File(getGamePath(true) + "mge3");
        File mgeBackupDirectory = new File(getGamePath(true) + "mge3" + SEPARATOR + "backup");
        if (mgeDirectory.exists()) {
            File mgeIniFile = new File(getGamePath(true) + "mge3" + SEPARATOR + "MGE.ini");
            if (mgeIniFile.exists()) {
                if (!mgeBackupDirectory.exists()) {
                    if (!mgeBackupDirectory.mkdirs()) {
                        log.error("Can't create MGE backup folder!");
                        return false;
                    }
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_HH.mm.ss");
                Date now = new Date();
                try {
                    Files.copy(Paths.get(mgeIniFile.toURI()), Paths.get(getGamePath(true) + "mge3" + SEPARATOR +
                            "backup" + SEPARATOR + "MGE_" + dateFormat.format(now) + ".ini"),
                            StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (IOException e) {
                    log.error("Can't copy! ", e);
                    return false;
                }
            } else {
                log.error("MGE.ini has not found!");
                return false;
            }
        } else {
            log.error("MGE directory has not found!");
            return false;
        }
    }

//    public boolean restoreMGEBackup(File backup) {
//        if (backup.exists()) {
//            try {
//                Files.copy(Paths.get(backup.toURI()), Paths.get(getGamePath(true) + "mge3" + SEPARATOR + "MGE.ini"),
//                        StandardCopyOption.REPLACE_EXISTING);
//                return true;
//            } catch (IOException e) {
//                e.printStackTrace();
//                return false;
//            }
//        }
//        return false;
//    }

    public boolean setMGEConfig(String source) {
        File file = new File(source);
        if (file.exists()) {
            try {
                Files.copy(Paths.get(file.toURI()), Paths.get(getGamePath(true) + "mge3" + SEPARATOR + "MGE.ini"),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public List<File> getMGEBackup() {
        File mgeBackupDirectory = new File(getGamePath(true) + "mge3" + SEPARATOR + "backup");
        List<File> result = new ArrayList<>();
        if (mgeBackupDirectory.exists()) {
            File[] files = mgeBackupDirectory.listFiles();
            if (files != null) {
                Collections.addAll(result, files);
            }
        }
        return result;
    }
}
