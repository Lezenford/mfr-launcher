package ru.fullrest.mfr.test_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.fullrest.mfr.test_server.model.entity.Property;
import ru.fullrest.mfr.test_server.model.entity.PropertyType;
import ru.fullrest.mfr.test_server.model.repository.PropertyRepository;
import ru.fullrest.mfr.test_server.security.SecurityService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class ClientController {

    private final SecurityService securityService;
    private final PropertyRepository propertyRepository;

    @GetMapping("auth")
    public String auth(@RequestParam String key) {
        return securityService.createToken(key);
    }

    @PreAuthorize("hasRole('TESTER')")
    @GetMapping("/api/game")
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadGame(
            @RequestHeader(required = false, name = "Range-From") Long range
                                                  ) throws IOException {
        return downloadFile(PropertyType.GAME_ARCHIVE, range);
    }

    @PreAuthorize("hasRole('TESTER')")
    @GetMapping("/api/launcher")
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadLauncher(
            @RequestHeader(required = false, name = "Range-From") Long range
                                                  ) throws IOException {
        return downloadFile(PropertyType.LAUNCHER, range);
    }

    @PreAuthorize("hasRole('TESTER')")
    @GetMapping("/api/launcherUpdater")
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadLauncherUpdater(
            @RequestHeader(required = false, name = "Range-From") Long range
                                                               ) throws IOException {
        return downloadFile(PropertyType.LAUNCHER_UPDATER, range);
    }

    @PreAuthorize("hasRole('TESTER')")
    @GetMapping("/api/launcherVersion")
    @ResponseBody
    public ResponseEntity<String> checkLauncherVersion() throws IOException {
        Property version = propertyRepository.findByType(PropertyType.LAUNCHER_VERSION).orElseThrow(NullPointerException::new);
        return ResponseEntity.ok().body(version.getValue());
    }

    private ResponseEntity<InputStreamResource> downloadFile(PropertyType type, Long range) throws IOException {
        Property download = propertyRepository.findByType(type).orElseThrow(NullPointerException::new);
        File file = new File(download.getValue());
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
