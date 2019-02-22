package ru.fullrest.mfr.plugins_configuration_utility.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.fullrest.mfr.plugins_configuration_utility.config.PropertiesConfiguration;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;

import java.io.*;
import java.nio.charset.Charset;
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
    private PropertiesConfiguration propertiesConfiguration;

    @Autowired
    private StageManager stageManager;

    public File openDirectoryChooser(boolean optionalFolder) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(getGamePathForChooser(optionalFolder));
        return directoryChooser.showDialog(stageManager.getApplicationStage());
    }

    public List<File> openFilesChooser(boolean optionalFolder) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(getGamePathForChooser(optionalFolder));
        return fileChooser.showOpenMultipleDialog(stageManager.getApplicationStage());
    }

    public File openFileChooser(boolean optionalFolder) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(getGamePathForChooser(optionalFolder));
        return fileChooser.showOpenDialog(stageManager.getApplicationStage());
    }

    public String getGamePath(boolean withSeparator) {
        return withSeparator ? propertiesConfiguration.getGamePath() + File.separator : propertiesConfiguration.getGamePath();
    }

    public String getOptionalPath(boolean withSeparator) {
        return withSeparator ? propertiesConfiguration.getGamePath() + File.separator + propertiesConfiguration.getOptional() + File.separator :
                propertiesConfiguration.getGamePath() + File.separator + propertiesConfiguration.getOptional();
    }

    private File getGamePathForChooser(boolean optionalFolder) {
        if (propertiesConfiguration.getGamePath() != null) {
            if (optionalFolder) {
                File file = new File(getOptionalPath(false));
                if (file.exists()) {
                    return file;
                } else {
                    return new File(propertiesConfiguration.getGamePath());
                }
            } else {
                return new File(propertiesConfiguration.getGamePath());
            }
        } else {
            return new File("");
        }
    }

    public boolean gameFolderCheck(File folder) {
        if (folder == null) {
            log.error("Game folder is null!");
            System.exit(0); //При старте, если не выбрать каталог - приложение закрывается
        }
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().equals(propertiesConfiguration.getMorrowind_exe())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkFileMD5(Details details) {
        File file = new File(getGamePath(true) + details.getGamePath());
        if (file.exists()) {
            return Arrays.equals(getFileMD5(file), details.getMd5());
        } else {
            return false;
        }
    }

    public byte[] getFileMD5(File file) {
        if (file != null) {
            if (file.exists()) {
                if (file.isFile()) {
                    try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                        MessageDigest digest = MessageDigest.getInstance("MD5");
                        digest.update(inputStream.readAllBytes());
                        return digest.digest();
                    } catch (IOException | NoSuchAlgorithmException e) {
                        log.error("File MD5 check error!\n", e);
                    }
                } else {
                    log.error("File is not a file: " + file.getAbsolutePath());
                }
            } else {
                log.error("File does not exist: " + file.getAbsolutePath());
            }
        } else {
            log.error("File is null");
        }
        return null;
    }

    public boolean checkMGEFilesForUnique() {
        try (BufferedInputStream inputStreamTop =
                     new BufferedInputStream(new FileInputStream(new File(getOptionalPath(true) + "MGE\\top_PK\\MGE" + ".ini")));
             BufferedInputStream inputStreamMiddle =
                     new BufferedInputStream(new FileInputStream(new File(getOptionalPath(true) + "MGE\\mid_PK\\MGE.ini")));
             BufferedInputStream inputStreamLow =
                     new BufferedInputStream(new FileInputStream(new File(getOptionalPath(true) + "MGE\\low_PK\\MGE.ini")));
             BufferedInputStream inputStreamNekro =
                     new BufferedInputStream(new FileInputStream(new File(getOptionalPath(true) + "MGE\\necro_PK\\MGE.ini")));
             BufferedInputStream inputStreamCurrent =
                     new BufferedInputStream(new FileInputStream(new File(getGamePath(true) + "mge3\\MGE.ini")))) {
            MessageDigest top = MessageDigest.getInstance("MD5");
            MessageDigest middle = MessageDigest.getInstance("MD5");
            MessageDigest low = MessageDigest.getInstance("MD5");
            MessageDigest necro = MessageDigest.getInstance("MD5");
            MessageDigest current = MessageDigest.getInstance("MD5");
            top.update(inputStreamTop.readAllBytes());
            middle.update(inputStreamMiddle.readAllBytes());
            low.update(inputStreamLow.readAllBytes());
            necro.update(inputStreamNekro.readAllBytes());
            current.update(inputStreamCurrent.readAllBytes());
            return !Arrays.equals(current.digest(), top.digest()) && !Arrays.equals(current.digest(), middle.digest())
                    && !Arrays.equals(current.digest(), low.digest()) && !Arrays.equals(current.digest(), necro.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error to check MGE files.\n", e);
            return true;
        }
    }

    public void removeFromGameDirectory(Details details) {
        if (!Files.isWritable(Paths.get(getGamePath(true) + details.getGamePath()))) {
            log.error("File does not exists: " + details.getGamePath());
        }
        try {
            Files.deleteIfExists(Paths.get(getGamePath(true) + details.getGamePath()));
        } catch (IOException e) {
            log.error("Error deleting file.\n", e);
        }
    }

    public void copyToGameDirectory(Details details) {
        try {
            File file = new File(getGamePath(true) + details.getGamePath());
            if (!file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    log.error("Can't create directory!");
                    return;
                }
            }
            Files.copy(Paths.get(getOptionalPath(true) + details.getStoragePath()),
                    Paths.get(getGamePath(true) + details.getGamePath()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error coping to game directory.", e);
        }
    }

    public InputStream getInputStreamFromFile(String path) {
        if (path != null && !path.isBlank()) {
            try {
                return Files.newInputStream(Paths.get(getGamePath(true) + path));
            } catch (IOException e) {
                log.error("Error to getting file input stream: " + path + "\n", e);
            }
        }
        return null;
    }

    public String getAbsolutePath(String path) {
        return "\"" + getGamePath(true) + path + "\"";
    }

    public String checkVersion() {
        String version = null;
        if (propertiesConfiguration.getGamePath() != null) {
            File versionFile = new File(getOptionalPath(true) + propertiesConfiguration.getVersionFileName());
            if (versionFile.exists()) {
                if (versionFile.isFile()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(versionFile))) {
                        version = reader.readLine();
                    } catch (IOException e) {
                        log.error("CheckVersion  error.\n", e);
                    }
                } else {
                    log.error("Version file is not a file: " + versionFile.getAbsolutePath());
                }
            } else {
                log.error("Version file doesn't exist: " + versionFile.getAbsolutePath());
            }
        } else {
            log.error("GamePath is null!");
        }
        return version;
    }

    public File getSchemaFile() {
        if (propertiesConfiguration.getGamePath() != null) {
            File schema = new File(getOptionalPath(true) + propertiesConfiguration.getSchemaFileName());
            if (schema.exists()) {
                if (schema.isFile()) {
                    return schema;
                } else {
                    log.error("Schema file is not a file");
                }
            } else {
                log.error("Schema file doesn't exist.");
            }
        } else {
            log.error("GamePath is null!");
        }
        return null;
    }

    public void saveMGEBackup() {
        File mgeDirectory = new File(getGamePath(true) + "mge3");
        File mgeBackupDirectory = new File(getGamePath(true) + "mge3" + File.separator + "backup");
        if (mgeDirectory.exists()) {
            File mgeIniFile = new File(getGamePath(true) + "mge3" + File.separator + "MGE.ini");
            if (mgeIniFile.exists()) {
                if (!mgeBackupDirectory.exists()) {
                    if (!mgeBackupDirectory.mkdirs()) {
                        log.error("Can't create MGE backup folder.");
                        return;
                    }
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_HH.mm.ss");
                Date now = new Date();
                try {
                    Files.copy(Paths.get(mgeIniFile.toURI()), Paths.get(getGamePath(true) + "mge3" + File.separator +
                                    "backup" + File.separator + "MGE_" + dateFormat.format(now) + ".ini"),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error("Can't copy file.\n ", e);
                }
            } else {
                log.error("MGE.ini has not found!");
            }
        } else {
            log.error("MGE directory has not found!");
        }
    }

    public void setMGEConfig(String source) {
        File file = new File(source);
        if (file.exists()) {
            try {
                Files.copy(Paths.get(file.toURI()), Paths.get(getGamePath(true) + "mge3" + File.separator + "MGE.ini"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Can't copy file.\n", e);
            }
        } else {
            log.error("MGE.ini doesn't exist");
        }
    }

    public List<File> getMGEBackup() {
        File mgeBackupDirectory = new File(getGamePath(true) + "mge3" + File.separator + "backup");
        List<File> result = new ArrayList<>();
        if (mgeBackupDirectory.exists()) {
            File[] files = mgeBackupDirectory.listFiles();
            if (files != null) {
                Collections.addAll(result, files);
            }
        }
        return result;
    }

    public List<File> getFilesFromDirectory(File file, List<File> result) {
        if (file == null || !file.exists()) {
            return result;
        } else {
            if (file.isFile()) {
                result.add(file);
                return result;
            } else {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File listFile : files) {
                        getFilesFromDirectory(listFile, result);
                    }
                }
                return result;
            }
        }
    }

    public String getRelativePath(File file, String gamePrefix, String optionalPrefix, boolean optional) {
        String result = "";
        if (file != null && file.exists()) {
            String fullPrefix;
            if (optionalPrefix.isBlank()) {
                fullPrefix = getOptionalPath(true);
            } else {
                fullPrefix = getOptionalPath(true) + optionalPrefix + File.separator;
            }
            if (optional) {
                result = file.getAbsolutePath().replace(getOptionalPath(true), "");
            } else {
                if (gamePrefix.isBlank()) {
                    result = file.getAbsolutePath().replace(fullPrefix, "");
                } else {
                    result = file.getAbsolutePath().replace(fullPrefix, gamePrefix + File.separator);
                }
            }
        }
        return result;
    }

    public void createSchemaFile(List<Group> groups) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(getOptionalPath(true)
                + propertiesConfiguration.getNewSchemaFileName()), Charset.forName("UTF-8")))) {
            ObjectMapper mapper = new ObjectMapper();
            for (Group group : groups) {
                String value = mapper.writeValueAsString(group);
                writer.write(value + "\n");
            }
        } catch (IOException e) {
            log.error("Error creating new schema file", e);
        }
    }
}