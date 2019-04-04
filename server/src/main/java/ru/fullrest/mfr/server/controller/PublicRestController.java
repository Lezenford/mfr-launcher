package ru.fullrest.mfr.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.fullrest.mfr.common.Links;
import ru.fullrest.mfr.common.UpdatePlan;
import ru.fullrest.mfr.common.Version;
import ru.fullrest.mfr.server.model.entity.Application;
import ru.fullrest.mfr.server.model.entity.FilePathEntity;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.repository.ApplicationRepository;
import ru.fullrest.mfr.server.model.repository.UpdateRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@RestController
@RequestMapping(Links.PUBLIC_API_LINK)
@RequiredArgsConstructor
public class PublicRestController {

    private final UpdateRepository updateRepository;

    private final ApplicationRepository applicationRepository;

    /**
     * Check the game gameVersion and sends update plan if necessary
     *
     * @param platform the client platform of the game
     * @param version  the client current gameVersion
     * @return current server gameVersion and update plan
     */
    @RequestMapping(value = Links.PUBLIC_API_GAME_VERSION_LINK, method = RequestMethod.GET)
    public ResponseEntity<?> getRepackVersion(@RequestHeader(required = false) String version,
                                              @RequestHeader(required = false) String platform) {
        Version result = new Version();
        if (version != null && platform != null) {
            Update lastUpdate = updateRepository.findFirstByPlatformAndActiveIsTrueOrderByIdDesc(platform);
            if (lastUpdate != null) {
                result.setVersion(lastUpdate.getVersion());
                Update userUpdate = updateRepository.findByVersionAndPlatform(version, platform);
                result.setClientVersionIsDefined(userUpdate != null);
                if (userUpdate != null && !version.equals(lastUpdate.getVersion())) {
                    result.setNeedUpdate(true);
                    result.setClientVersionIsDefined(true);
                    List<Update> updates = updateRepository
                            .findByPlatformAndActiveIsTrueAndIdIsGreaterThanOrderByIdAsc(platform, userUpdate.getId());
                    List<String> plan = new ArrayList<>();
                    UpdatePlan updatePlan = new UpdatePlan();
                    result.setUpdatePlan(updatePlan);
                    updatePlan.setRefreshApplied(false);
                    updatePlan.setRefreshSchema(false);
                    updatePlan.setUpdates(plan);
                    updates.forEach(update -> {
                        plan.add(update.getVersion());
                        if (update.isSchemaUpdate()) {
                            updatePlan.setRefreshSchema(true);
                        }
                        if (update.isAppliedUpdate()) {
                            updatePlan.setRefreshApplied(true);
                        }
                    });
                }
            }
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    /**
     * Check the game gameVersion and sends update plan if necessary
     *
     * @param version  the client current gameVersion
     * @param platform the client operation system
     * @return current server gameVersion and update plan
     */
    @RequestMapping(value = Links.PUBLIC_API_APPLICATION_VERSION_LINK, method = RequestMethod.GET)
    public ResponseEntity<?> getApplicationVersion(@RequestHeader String version, @RequestHeader String platform) {
        Version result = new Version();
        Application application = applicationRepository.findByShortName(platform);
        if (application == null) {
            result.setClientVersionIsDefined(false);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
        }
        result.setClientVersionIsDefined(true);
        result.setVersion(application.getVersion());
        if (!application.getVersion().equals(version)) {
            result.setNeedUpdate(true);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    /**
     * Download path for selected gameVersion
     *
     * @param platform the platform of the game
     * @param version  the patch gameVersion
     * @return path archive entity
     */
    @RequestMapping(value = Links.PUBLIC_API_GAME_UPDATE_LINK + "{platform}" + "/" + "{version}", method =
            RequestMethod.GET)
    public ResponseEntity<?> downloadPatch(@PathVariable String platform, @PathVariable String version) {
        Update update = updateRepository.findByVersionAndPlatform(version, platform);
        if (update != null) {//TODO проверить логику
            update.setDownloadCount(update.getDownloadCount() + 1);
            updateRepository.save(update);
        }
        return sendFile(update);
    }

    /**
     * Download desktop application
     *
     * @param operationSystem operating system required. win, mac, linux etc.
     * @return application archive
     */
    @RequestMapping(value = Links.PUBLIC_API_APPLICATION_UPDATE_LINK + "{operationSystem}", method = RequestMethod.GET)
    public ResponseEntity<?> downloadApplication(@PathVariable String operationSystem) {
        Application application = applicationRepository.findByShortName(operationSystem);
        return sendFile(application);
    }

    private ResponseEntity<Resource> sendFile(FilePathEntity filePath) {
        try {
            if (filePath != null) {
                File file = new File(new File("").getAbsoluteFile(), filePath.getPath());
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                return ResponseEntity.ok()
                                     .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                                     .contentLength(file.length())
                                     .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                     .body(resource);
            }
        } catch (FileNotFoundException e) {
            log.error(String.format("File not found. %s", filePath), e);
        } catch (NullPointerException e) {
            log.error(String.format("Empty path for %s", filePath), e);
        }
        return ResponseEntity.noContent().build();
    }
}
