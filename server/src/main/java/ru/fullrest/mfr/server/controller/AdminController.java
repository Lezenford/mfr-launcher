package ru.fullrest.mfr.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.fullrest.mfr.server.config.LinkConfiguration;
import ru.fullrest.mfr.server.exception.BadRequestException;
import ru.fullrest.mfr.server.exception.InternalServerException;
import ru.fullrest.mfr.server.exception.NotFoundException;
import ru.fullrest.mfr.server.exception.UploadFileException;
import ru.fullrest.mfr.server.model.entity.Application;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.repository.ApplicationRepository;
import ru.fullrest.mfr.server.model.repository.UpdateRepository;
import ru.fullrest.mfr.server.mvc.AdminSession;
import ru.fullrest.mfr.server.mvc.ContentType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Log4j2
@Controller
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private AdminSession session;

    private final UpdateRepository updateRepository;

    private final ApplicationRepository applicationRepository;

    private final LinkConfiguration linkConfiguration;

    @GetMapping(value = {"/", ""})
    public String welcome() {
        session.setParameters(null, null);
        return "main";
    }

    @GetMapping(value = "/game/update")
    public String gamePlatforms() {
        session.setParameters(ContentType.GAME, null);
        return "game";
    }

    @GetMapping(value = "/game/update/{platform}")
    public String gameUpdatesList(Model model, @PathVariable String platform) throws NotFoundException {
        session.setParameters(ContentType.GAME, platform);
        List<Update> updates = updateRepository.findAllByPlatformOrderByIdAsc(platform);
        if (updates.isEmpty()) {
            throw new NotFoundException(String.format("Platform %s does not exist", platform));
        }
        model.addAttribute("updates", updates);
        return "game";
    }

    @GetMapping(value = "/game/update/{platform}/{id}/change")
    public String changeGameUpdate(@PathVariable("id") int id, @PathVariable("platform") String platform,
                                   @RequestParam(value = "active") boolean active) {
        session.setParameters(ContentType.GAME, platform);
        Optional<Update> optionalUpdate = updateRepository.findById(id);
        Update update;
        if (optionalUpdate.isPresent() && optionalUpdate.get().getPlatform().equals(platform)
                && optionalUpdate.get().getId() == id) {
            update = optionalUpdate.get();
            optionalUpdate.get().setActive(active);
            updateRepository.save(update);
        } else {
            throw new BadRequestException(String
                    .format("Update with platform=%s and id=%s not found in database", platform, id));
        }
        return "redirect:/admin/game/update/" + platform;
    }

    @GetMapping(value = "/game/update/create")
    public String createPlatform(Model model) {
        session.setParameters(ContentType.GAME, null);
        return uploadGameUpdate(model);
    }

    @GetMapping(value = "/game/update/upload")
    public String uploadGameUpdate(Model model) {
        return uploadGameUpdate(model, 1, null);
    }

    private String uploadGameUpdate(Model model, int step, String error) {
        session.setContentType(ContentType.GAME);
        model.addAttribute("error", error);
        model.addAttribute("step", step);
        boolean exist = session.getPlatform() != null;
        model.addAttribute("title", exist ?
                String.format("Добавление обновления в %s", session.getPlatform()) : "Создание новой платформы");
        model.addAttribute("exist", exist);
        return "game_upload";
    }

    @PostMapping(value = "/game/update/upload/step1")
    public String uploadGameUpdateStepOne(Model model, @RequestParam("platform") String platform,
                                          @RequestParam("exist") boolean exist,
                                          @RequestParam("file") MultipartFile file) throws UploadFileException {
        String error;
        int step = 1;
        if (platform == null || file == null) {
            throw new NullPointerException(String
                    .format("One of required parameters is null. platform: %s file: %s", platform, file));
        }
        List<?> existingPlatform = updateRepository.findAllPlatformByOrderByPlatformAsc();
        if (exist && !existingPlatform.contains(platform)) {
            error = String.format("Платформа %s не существует", platform);
        } else {
            if (!exist && existingPlatform.contains(platform)) {
                error = String.format("Платформа %s уже существует", platform);
            } else {
                Update update = new Update();
                String path = uploadData(file, true);
                String version = getUploadFileVersion(path);
                update.setPlatform(platform);
                update.setVersion(version);
                update.setPath(path);
                error = checkUpdate(update.getPlatform(), update.getVersion());
                if (error.isBlank()) { //continue without error
                    model.addAttribute("update", update);
                    step = 2;
                }
            }
        }
        return uploadGameUpdate(model, step, error);
    }

    @PostMapping(value = "/game/update/upload/step2")
    public String uploadGameUpdateStepTwo(Model model, @ModelAttribute(value = "update") Update update) throws InternalServerException {
        session.setContentType(ContentType.GAME);
        String error = checkUpdate(update.getPlatform(), update.getVersion());
        if (error.isBlank()) {
            try {
                updateRepository.save(update);
                session.setPlatform(update.getPlatform());
                return "redirect:/admin/game/update/" + session.getPlatform();
            } catch (Exception e) {
                throw new InternalServerException(e);
            }
        }
        return uploadGameUpdate(model, 1, error);
    }

    @GetMapping(value = "/application")
    public String applicationsList(Model model) {
        session.setParameters(ContentType.APPLICATION, null);
        List<Application> applications = (List<Application>) applicationRepository.findAll();
        model.addAttribute("applications", applications);
        return "application";
    }

    @GetMapping(value = "/application/upload")
    public String uploadNewApplication(Model model, @RequestParam(value = "error", required = false) String error) {
        session.setParameters(ContentType.APPLICATION, null);
        model.addAttribute("app", new Application());
        model.addAttribute("error", error);
        model.addAttribute("title", "Загрузить новое приложение");
        return "application_update";
    }

    @GetMapping(value = "/application/update/{platform}")
    public String updateApplication(Model model, @PathVariable("platform") String platform,
                                    @RequestParam(value = "error", required = false) String error) throws NotFoundException {
        session.setParameters(ContentType.APPLICATION, null);
        Application application = applicationRepository.findByShortName(platform);
        if (application == null) {
            throw new NotFoundException(String.format("Application platform %s does not exist", platform));
        }
        model.addAttribute("app", application);
        model.addAttribute("error", error);
        model.addAttribute("title", String.format("Обновление приложения %s", application.getShortName()));
        return "application_update";
    }

    @PostMapping(value = {"/application/update", "/application/upload"})
    public String updateApplication(Model model, @ModelAttribute("app") Application app,
                                    @RequestParam("file") MultipartFile file) throws NotFoundException {
        session.setParameters(ContentType.APPLICATION, null);
        String error;
        try {
            String s = uploadData(file, false);
            app.setPath(s);
            error = checkApplication(app);
            if (error.isBlank()) {
                applicationRepository.save(app);
                return "redirect:/admin/application";
            }

        } catch (UploadFileException e) {
            log.error(e.getMessage());
            error = e.getMessage();
        }
        return updateApplication(model, app.getShortName(), error);
    }

    private String uploadData(MultipartFile file, boolean gameUpdate) throws UploadFileException {
        if (file.isEmpty()) {
            throw new UploadFileException("File is empty");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".zip")) {
            throw new UploadFileException("File is not zip archive");
        }
        try {
            byte[] bytes = file.getBytes();
            String localPath;
            if (gameUpdate) {
                localPath = linkConfiguration.getGameUpdateStoragePath();
            } else {
                localPath = linkConfiguration.getApplicationStoragePath();
            }
            String relativePath = String
                    .format("%s%s_%s", localPath, UUID.randomUUID(), file.getOriginalFilename());
            File localFile = new File(new File("").getAbsoluteFile(), relativePath);
            if (Files.notExists(localFile.getParentFile().toPath())) {
                if (!localFile.getParentFile().mkdirs() && Files.notExists(localFile.getParentFile().toPath())) {
                    throw new UploadFileException("Can't create parent folder. " + localFile.getParent());
                }
            }
            if (Files.exists(localFile.toPath())) {
                throw new UploadFileException("File already exist!");
            } else {
                Files.write(localFile.toPath(), bytes);
                return relativePath;
            }
        } catch (IOException e) {
            throw new UploadFileException("Can't save new file", e);
        }
    }

    private String getUploadFileVersion(String relativePath) {
        try {
            ZipFile zipFile = new ZipFile(new File("").getAbsolutePath() + relativePath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (!zipEntry.isDirectory() && zipEntry.getName().toLowerCase().equals("optional/version")) {
                    try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                        return new String(inputStream.readAllBytes());
                    }
                }
            }
            return null;
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    private String checkUpdate(String platform, String version) {
        String result = null;
        if (platform == null || version == null) {
            result = "Некорректные данные";
            log.error(String.format("Incorrect platform or version. Platform: %s version: %s", platform, version));
        } else {
            if (updateRepository
                    .findByVersionAndPlatform(version, platform) != null) {
                result = String.format("Версия обновления %s уже существует для платформы %s.", version, platform);
                log.error(result);
            }
        }
        return result == null ? "" : result;
    }

    private String checkApplication(Application application) {
        String result = null;
        if (application.getId() == 0) {
            if (applicationRepository.findByShortName(application.getShortName()) != null) {
                result = String.format("Приложения для %s уже существует!", application.getShortName());
            }
        } else {
            if (applicationRepository.findByShortName(application.getShortName()).getVersion()
                                     .equals(application.getVersion())) {
                result = String.format("Номер версии %s совпадает с текущим!", application.getVersion());
            }
        }
        return result == null ? "" : result;
    }
}