package ru.fullrest.mfr.server.file_server.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fullrest.mfr.server.file_server.model.entity.Download;

public interface DownloadRepository extends JpaRepository<Download, Integer> {
}
