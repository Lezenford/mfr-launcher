package ru.fullrest.mfr.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.fullrest.mfr.api.Links;
import ru.fullrest.mfr.server.model.entity.Property;
import ru.fullrest.mfr.server.model.entity.PropertyType;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.service.HistoryService;
import ru.fullrest.mfr.server.service.PropertyService;
import ru.fullrest.mfr.server.service.UpdateService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@RestController
@RequiredArgsConstructor
public class ClientController {

    private final PropertyService propertyService;
    private final UpdateService updateService;
    private final HistoryService historyService;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(1);

    @GetMapping(value = Links.GAME_DOWNLOAD)
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadGame(
            @RequestHeader(required = false, name = "Range-From") Long range,
            @RequestHeader(required = false, name = HttpHeaders.COOKIE) String cookie
                                                           ) throws IOException {
        threadPool.execute(() -> historyService.gameDownload(cookie));
        return downloadFile(PropertyType.GAME_ARCHIVE, range);
    }

    @GetMapping(value = Links.GAME_VERSION_HISTORY)
    @ResponseBody
    public ResponseEntity<List<String>> getGameVersionHistory() {
        List<String> versions = updateService.findAllActive();
        return ResponseEntity.ok(versions);
    }

    @GetMapping(value = Links.GAME_UPDATE_DOWNLOAD)
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadGameUpdate(
            @RequestHeader(required = false, name = "Range-From") Long range,
            @RequestHeader(required = false, name = HttpHeaders.COOKIE) String cookie,
            @RequestParam String version) throws IOException {
        Update update = updateService.findByVersion(version);
        if (update == null) {
            return ResponseEntity.badRequest().build();
        }
        threadPool.execute(() -> historyService.updateDownload(cookie, version));
        File file = new File(update.getPath());
        return downloadFile(file, range);
    }

    @GetMapping(value = Links.LAUNCHER_DOWNLOAD)
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadLauncher(
            @RequestHeader(required = false, name = "Range-From") Long range,
            @RequestHeader(required = false, name = HttpHeaders.COOKIE) String cookie
                                                               ) throws IOException {
        threadPool.execute(() -> historyService.applicationDownload(cookie));
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
        Property version = propertyService.findByType(PropertyType.LAUNCHER_VERSION);
        if (version == null) {
            throw new NullPointerException("Value is not set");
        }
        return ResponseEntity.ok().body(version.getValue());
    }

    private ResponseEntity<InputStreamResource> downloadFile(PropertyType type, Long range) throws IOException {
        Property download = propertyService.findByType(type);
        if (download == null) {
            throw new NullPointerException("Value is not set");
        }
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
