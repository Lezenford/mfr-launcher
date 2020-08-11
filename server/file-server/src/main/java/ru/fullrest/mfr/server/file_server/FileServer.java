package ru.fullrest.mfr.server.file_server;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.fullrest.mfr.server.file_server.model.entity.Download;
import ru.fullrest.mfr.server.file_server.model.repository.DownloadRepository;

@RequiredArgsConstructor
@SpringBootApplication
public class FileServer implements CommandLineRunner {

    private final DownloadRepository downloadRepository;

    public static void main(String[] args) {
        SpringApplication.run(FileServer.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Download download = new Download();
        download.setPath("morrowind-fullrest-repack.zip");
        downloadRepository.save(download);
        download = new Download();
        download.setPath("OpenMW.zip");
        downloadRepository.save(download);
    }
}
