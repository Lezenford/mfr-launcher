package ru.fullrest.mfr.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.fullrest.mfr.server.config.LinkConfiguration;
import ru.fullrest.mfr.server.exception.UploadFileException;
import ru.fullrest.mfr.server.model.entity.Application;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.repository.ApplicationRepository;
import ru.fullrest.mfr.server.model.repository.UpdateRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Log4j2
@Controller
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    private final UpdateRepository updateRepository;

    private final ApplicationRepository applicationRepository;

    private final LinkConfiguration linkConfiguration;

    @RequestMapping(value = "/")
    public String welcome() {
        return "admin";
    }

    @RequestMapping(value = "/game/update", method = RequestMethod.GET)
    public String gamePlatforms(Model model) {
        model.addAttribute("platforms", getAllPlatforms());
        return "game";
    }

    @RequestMapping(value = "/game/update/{platform}", method = RequestMethod.GET)
    public String gameUpdatesList(Model model, @PathVariable String platform) {
        model.addAttribute("platforms", getAllPlatforms());
        model.addAttribute("platform", platform);
        model.addAttribute("updates", updateRepository.findAllByPlatformOrderByIdAsc(platform));
        return "game_update";
    }

    @RequestMapping(value = "/game/update/{platform}/{id}/change", method = RequestMethod.GET)
    public String changeGameUpdate(@PathVariable("id") int id, @PathVariable("platform") String platform,
                                   @RequestParam(value = "active", required = false) Boolean active) {
        Optional<Update> optionalUpdate = updateRepository.findById(id);
        Update update;
        if (optionalUpdate.isPresent()) {
            update = optionalUpdate.get();
            if (active != null) {
                optionalUpdate.get().setActive(active);
            }
            updateRepository.save(update);
        }
        return "redirect:/admin/game/update/" + platform;
    }

    @RequestMapping(value = "/game/update/upload", method = RequestMethod.GET)
    public String uploadNewGamePatch(Model model, @RequestParam(value = "error", required = false) String error) {
        Update update = new Update();
        model.addAttribute("update", update);
        model.addAttribute("error", error);
        return "game_update_upload";
    }

    @RequestMapping(value = "/game/update/upload", method = RequestMethod.POST)
    public String uploadNewGamePatch(@ModelAttribute("update") Update update,
                                     @RequestParam("file") MultipartFile file) {
        try {
            if (updateRepository.findByVersionAndPlatform(update.getVersion(), update.getPlatform()) != null) {
                return "redirect:/admin/game/update/upload?error=Version already exists";
            }
            String path = uploadData(file, true);
            update.setPath(path);
            updateRepository.save(update);
        } catch (UploadFileException e) {
            log.error(e);
            return "redirect:/admin/game/update/upload?error=" + e.getMessage();
        }
        return "redirect:/admin/game/update";
    }

    @RequestMapping(value = "/application", method = RequestMethod.GET)
    public String applicationsList(Model model) {
        List<Application> applications = (List<Application>) applicationRepository.findAll();
        model.addAttribute("applications", applications);
        return "application";
    }

    @RequestMapping(value = "/application/update/{platform}", method = RequestMethod.GET)
    public String updateApplication(Model model, @PathVariable("platform") String platform, @RequestParam(value =
            "error", required = false) String error) {
        Application application = applicationRepository.findByShortName(platform);
        model.addAttribute("app", application);
        model.addAttribute("error", error);
        return "application_update";
    }

    @RequestMapping(value = "/application/update", method = RequestMethod.POST)
    public String updateApplication(@ModelAttribute("app") Application app, @RequestParam("file") MultipartFile file) {
        try {
            String s = uploadData(file, false);
            app.setPath(s);
            applicationRepository.save(app);
        } catch (UploadFileException e) {
            log.error(e.getMessage());
            return "redirect:/admin/application/update/" + app.getShortName() + "?error=" + e.getMessage();
        }
        return "redirect:/admin/application";
    }

    @RequestMapping(value = "/application/upload", method = RequestMethod.GET)
    public String uploadNewApplication(Model model, @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("app", new Application());
        model.addAttribute("error", error);
        return "application_upload";
    }

    @RequestMapping(value = "/application/upload", method = RequestMethod.POST)
    public String uploadNewApplication(@ModelAttribute("app") Application application,
                                       @RequestParam("file") MultipartFile file) {
        try {
            if (applicationRepository.findByShortName(application.getShortName()) != null) {
                return "redirect:/admin/application/upload?error=Platform already exists";
            }
            String path = uploadData(file, false);
            application.setPath(path);
            applicationRepository.save(application);
        } catch (UploadFileException e) {
            log.error(e);
            return "redirect:/admin/application/upload?error=" + e.getMessage();
        }
        return "redirect:/admin/application";
    }

    private Set<String> getAllPlatforms() {
        List<Update> all = updateRepository.findAllByOrderByIdAsc();
        Set<String> platforms = new TreeSet<>();
        all.forEach(update -> platforms.add(update.getPlatform()));
        return platforms;
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
                    .format("%s%s_%s", localPath, file.getOriginalFilename(), UUID.randomUUID());
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
}