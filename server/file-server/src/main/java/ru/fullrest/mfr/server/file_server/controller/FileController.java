package ru.fullrest.mfr.server.file_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.fullrest.mfr.server.file_server.model.entity.Download;
import ru.fullrest.mfr.server.file_server.model.repository.DownloadRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class FileController {
    private final DownloadRepository repository;

    @GetMapping("/{number}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> get(
            @PathVariable int number,
            @RequestHeader(required = false, name = "Range-From") Long range
                                                  ) throws IOException {
        Download download = repository.findById(number).orElseThrow();
        File file = new File(download.getPath());
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
