package ru.fullrest.mfr.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.fullrest.mfr.api.Links;
import ru.fullrest.mfr.server.model.entity.*;
import ru.fullrest.mfr.server.model.repository.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Log4j2
@RestController
@RequiredArgsConstructor
public class ClientController {

    private final PropertyRepository propertyRepository;
    private final UpdateRepository updateRepository;
    private final GameDownloadHistoryRepository gameDownloadHistoryRepository;
    private final ApplicationDownloadHistoryRepository applicationDownloadHistoryRepository;
    private final UpdateDownloadHistoryRepository updateDownloadHistoryRepository;

    @GetMapping(value = Links.GAME_DOWNLOAD)
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadGame(
            @RequestHeader(required = false, name = "Range-From") Long range,
            @RequestHeader(required = false, name = HttpHeaders.COOKIE) String cookie
                                                           ) throws IOException {
        if (cookie != null) {
            String key = getKeyFromCookie(cookie);
            if (key != null) {
                if (!gameDownloadHistoryRepository.existsByClientKey(key)) {
                    GameDownloadHistory gameDownloadHistory = new GameDownloadHistory();
                    gameDownloadHistory.setClientKey(key);
                    gameDownloadHistoryRepository.save(gameDownloadHistory);
                }
            }
        }
        return downloadFile(PropertyType.GAME_ARCHIVE, range);
    }

    @GetMapping(value = Links.GAME_VERSION_HISTORY)
    @ResponseBody
    public ResponseEntity<List<String>> getGameVersionHistory() {
        List<String> versions = updateRepository.findAllActive();
        return ResponseEntity.ok(versions);
    }

    @GetMapping(value = Links.GAME_UPDATE_DOWNLOAD)
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadGameUpdate(
            @RequestHeader(required = false, name = "Range-From") Long range,
            @RequestHeader(required = false, name = HttpHeaders.COOKIE) String cookie,
            @RequestParam String version) throws IOException {
        Update update = updateRepository.findByVersion(version);
        if (update == null) {
            return ResponseEntity.badRequest().build();
        }
        File file = new File(update.getPath());
        if (cookie != null) {
            String key = getKeyFromCookie(cookie);
            if (key != null) {
                if (!updateDownloadHistoryRepository.existsByClientKey(key)) {
                    UpdateDownloadHistory updateDownloadHistory = new UpdateDownloadHistory();
                    updateDownloadHistory.setClientKey(key);
                    updateDownloadHistoryRepository.save(updateDownloadHistory);
                }
            }
        }
        return downloadFile(file, range);
    }

    @GetMapping(value = Links.LAUNCHER_DOWNLOAD)
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadLauncher(
            @RequestHeader(required = false, name = "Range-From") Long range,
            @RequestHeader(required = false, name = HttpHeaders.COOKIE) String cookie
                                                               ) throws IOException {
        if (cookie != null) {
            String key = getKeyFromCookie(cookie);
            if (key != null) {
                if (!applicationDownloadHistoryRepository.existsByClientKey(key)) {
                    ApplicationDownloadHistory applicationDownloadHistory = new ApplicationDownloadHistory();
                    applicationDownloadHistory.setClientKey(key);
                    applicationDownloadHistoryRepository.save(applicationDownloadHistory);
                }
            }
        }
        return downloadFile(PropertyType.LAUNCHER, range);
    }

    @GetMapping(value = Links.LAUNCHER_UPDATER_DOWNLOAD)
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadLauncherUpdater(
            @RequestHeader(required = false, name = "Range-From") Long range
                                                                      ) throws IOException {
        return downloadFile(PropertyType.LAUNCHER_UPDATER, range);
    }

    @GetMapping(value = Links.LAUNCHER_VERSION)
    @ResponseBody
    public ResponseEntity<String> checkLauncherVersion() {
        Property version = propertyRepository.findByType(PropertyType.LAUNCHER_VERSION).orElseThrow(NullPointerException::new);
        return ResponseEntity.ok().body(version.getValue());
    }

    private String getKeyFromCookie(String cookie) {
        Optional<String> first = Arrays.stream(cookie.split(";")).filter(it -> it.contains("Key=")).findFirst();
        return first.map(s -> s.trim().replace("Key=", "")).orElse(null);
    }

    private ResponseEntity<InputStreamResource> downloadFile(PropertyType type, Long range) throws IOException {
        Property download = propertyRepository.findByType(type).orElseThrow(NullPointerException::new);
        File file = new File(download.getValue());
        return downloadFile(file, range);
    }

    private ResponseEntity<InputStreamResource> downloadFile(File file, Long range) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        if (range != null) {
            fileInputStream.skip(range);
        }
        InputStreamResource fileSystemResource = new InputStreamResource(fileInputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(fileSystemResource);
    }
}
