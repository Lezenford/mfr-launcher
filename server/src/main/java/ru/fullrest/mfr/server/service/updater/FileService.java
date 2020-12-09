package ru.fullrest.mfr.server.service.updater;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.fullrest.mfr.api.GameUpdate;
import ru.fullrest.mfr.server.exception.PatcherException;
import ru.fullrest.mfr.server.model.repository.UpdateRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@Service
@RequiredArgsConstructor
class FileService {
    private static final String VERSION_FILE_PATH = "Optional/version";
    private static final String PATCH_FOLDER_NAME = "patch";

    private final ObjectMapper objectMapper;
    private final UpdateRepository updateRepository;

    @Value("${git.repository.local}")
    private String repositoryPath;

    @Value("${local.update-folder}")
    private String updatesFolder;

    public GameUpdate checkVersion(GameUpdate gameUpdate) throws IOException {
        String version;
        try (BufferedReader fileReader = new BufferedReader(new FileReader(new File(repositoryPath + File.separator + VERSION_FILE_PATH)))) {
            version = fileReader.readLine();
        }
        if (updateRepository.findByVersion(version) != null) {
            throw new PatcherException("Такое обновление уже существует");
        }
        return gameUpdate.copy(
                version, gameUpdate.getAddFiles(), gameUpdate.getMoveFiles(), gameUpdate.getRemoveFiles(), gameUpdate.getChangeLog());
    }

    public File createPatch(GameUpdate gameUpdate) throws IOException {
        if (gameUpdate.getVersion().isBlank()) {
            throw new PatcherException("Версия не определена");
        }

        File archive = new File(updatesFolder + File.separator + gameUpdate.getVersion());

        if (archive.exists()) {
            throw new IllegalArgumentException("Archive already exists");
        }


        Path tempDirectory = Files.createTempDirectory("mfr_patch");
        File updateFile = new File(tempDirectory.toFile().getAbsolutePath() + File.separator + GameUpdate.FILE_NAME);
        try (FileWriter fileWriter = new FileWriter(updateFile)) {
            fileWriter.write(objectMapper.writeValueAsString(gameUpdate));
        }

        File patchFile = new File(tempDirectory.toFile().getAbsolutePath() + File.separator + PATCH_FOLDER_NAME);
        for (String addFile : gameUpdate.getAddFiles()) {
            File source = new File(repositoryPath + File.separator + addFile);
            File target = new File(patchFile.getAbsolutePath() + File.separator + addFile);
            if (!target.getParentFile().exists()) {
                target.getParentFile().mkdirs();
            }
            Files.copy(source.toPath(), target.toPath());
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(archive))) {
            Collection<File> files = FileUtils.listFiles(tempDirectory.toFile(), null, true);
            for (File file : files) {
                zipOutputStream.putNextEntry(
                        new ZipEntry(file.getAbsolutePath().replaceFirst(tempDirectory.toFile().getAbsolutePath() + File.separator, "")));
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    zipOutputStream.write(fileInputStream.readAllBytes());
                }
                zipOutputStream.closeEntry();
            }
            FileUtils.deleteDirectory(tempDirectory.toFile());

        }
        return archive;


    }
}
