package ru.fullrest.mfr.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ru.fullrest.mfr.common.Version;
import ru.fullrest.mfr.server.ServerApplication;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.entity.mock.ApplicationData;
import ru.fullrest.mfr.server.model.entity.mock.UpdateData;
import ru.fullrest.mfr.server.model.repository.ApplicationRepository;
import ru.fullrest.mfr.server.model.repository.UpdateRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServerApplication.class)
@AutoConfigureMockMvc
@TestPropertySource("classpath:application.properties")
public class PublicRestControllerTest {

    private final UpdateData updateData = new UpdateData();
    private final ApplicationData applicationData = new ApplicationData();

    private final String downloadPatchUrl = "/api/game/download/update/original/";
    private final String downloadApplicationUrl = "/api/application/download/";
    private final String checkGameVersionUrl = "/api/game/version";
    private final String checkApplicationVersionUrl = "/api/application/version";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UpdateRepository updateRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Before
    public void setUp()  {
        updateRepository.saveAll(updateData.getUpdates());
        for (Update update : updateData.getUpdates()) {
            try {
                Files.createFile(Paths.get(update.getPath()));
            } catch (IOException ignored) {

            }
        }
        applicationRepository.save(applicationData.getApplication());
        try {
            Files.createFile(Paths.get(applicationData.getApplication().getPath()));
        } catch (IOException ignored) {

        }
    }

    @After
    public void clean()  {
        updateRepository.deleteAll();
        for (Update update : updateData.getUpdates()) {
            try {
                Files.deleteIfExists(Paths.get(update.getPath()));
            } catch (IOException ignored) {
            }
        }
        applicationRepository.deleteAll();
        try {
            Files.deleteIfExists(Paths.get(applicationData.getApplication().getPath()));
        } catch (IOException ignored) {
        }
    }

    @Test
    public void successfulDownloadApplication() throws Exception {
        this.mockMvc.perform(get(String
                .format("%s%s", downloadApplicationUrl, applicationData.getApplication().getShortName())))
                    .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(status().isOk());
    }

    @Test
    public void incorrectDownloadApplication() throws Exception {
        this.mockMvc.perform(get(String
                .format("%s%s", downloadApplicationUrl, applicationData.getApplication().getShortName() + "test")))
                    .andExpect(status().is(204));
    }

    @Test
    public void noVersionDownloadApplication() throws Exception {
        this.mockMvc.perform(get(downloadApplicationUrl))
                    .andExpect(status().isNotFound());
    }

    @Test
    public void successfulDownloadPatch() throws Exception {
        this.mockMvc.perform(get(String.format("%s%s", downloadPatchUrl, updateData.getUpdates().get(0).getVersion())))
                    .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(status().isOk());
    }

    @Test
    public void incorrectVersionDownloadPatch() throws Exception {
        this.mockMvc
                .perform(get(String
                        .format("%s%s", downloadPatchUrl, updateData.getUpdates().get(0).getVersion() + "test")))
                .andExpect(status().is(204));
    }

    @Test
    public void noVersionDownloadPatch() throws Exception {
        this.mockMvc.perform(get(downloadPatchUrl))
                    .andExpect(status().isNotFound());
    }

    @Test
    public void checkLastGameVersion() throws Exception {
        this.mockMvc.perform(get(checkGameVersionUrl)
                .header("version", updateData.getUpdates().get(updateData.getUpdates().size() - 1).getVersion())
                .header("platform", "original"))
                    .andExpect(status().isOk())
                    .andDo(result -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Version version = objectMapper
                                .readValue(result.getResponse().getContentAsString(), Version.class);
                        Assert.assertTrue(version.isClientVersionIsDefined());
                        Assert.assertEquals(version.getVersion(), updateData.getUpdates()
                                                                            .get(updateData.getUpdates().size() - 1)
                                                                            .getVersion());
                        Assert.assertFalse(version.isNeedUpdate());
                        Assert.assertNull(version.getUpdatePlan());
                    });
    }

    @Test
    public void checkNotLastGameVersion() throws Exception {
        this.mockMvc.perform(get(checkGameVersionUrl).header("version", updateData.getUpdates().get(0).getVersion())
                                                     .header("platform", "original"))
                    .andExpect(status().isOk())
                    .andDo(result -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Version version = objectMapper
                                .readValue(result.getResponse().getContentAsString(), Version.class);
                        Assert.assertTrue(version.isClientVersionIsDefined());
                        Assert.assertNotEquals(version.getVersion(), updateData.getUpdates().get(0).getVersion());
                        Assert.assertNotNull(version.getUpdatePlan());
                        Assert.assertTrue(version.isNeedUpdate());
                        boolean sorted = true;
                        for (int i = 0; i < updateData.getMaxCount() - 1; i++) {
                            if (!version.getUpdatePlan().getUpdates().get(i)
                                        .equals(updateData.getUpdates().get(i + 1).getVersion())) {
                                sorted = false;
                                break;
                            }
                        }
                        Assert.assertTrue(sorted);
                    });
    }

    @Test
    public void checkUnknownGameVersion() throws Exception {
        this.mockMvc.perform(get(checkGameVersionUrl)
                .header("version", updateData.getUpdates().get(0).getVersion() + "test"))
                    .andExpect(status().isOk())
                    .andDo(result -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        Version version = objectMapper
                                .readValue(result.getResponse().getContentAsString(), Version.class);
                        Assert.assertFalse(version.isNeedUpdate());
                        Assert.assertFalse(version.isClientVersionIsDefined());
                        Assert.assertNull(version.getUpdatePlan());
                    });
    }

    @Test
    public void checkLastApplicationVersion() throws Exception {
        this.mockMvc
                .perform(get(checkApplicationVersionUrl)
                        .header("version", applicationData.getApplication().getVersion())
                        .header("platform", applicationData.getApplication()
                                                           .getShortName()))
                .andExpect(status().isOk())
                .andDo(result -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Version version = objectMapper.readValue(result.getResponse().getContentAsString(), Version.class);
                    Assert.assertTrue(version.isClientVersionIsDefined());
                    Assert.assertFalse(version.isNeedUpdate());
                    Assert.assertEquals(version.getVersion(), applicationData.getApplication().getVersion());
                });
    }

    @Test
    public void checkNotLastApplicationVersion() throws Exception {
        this.mockMvc
                .perform(get(checkApplicationVersionUrl)
                        .header("version", applicationData.getApplication().getVersion() + "test")
                        .header("platform", applicationData.getApplication()
                                                           .getShortName()))
                .andExpect(status().isOk())
                .andDo(result -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Version version = objectMapper.readValue(result.getResponse().getContentAsString(), Version.class);
                    Assert.assertTrue(version.isClientVersionIsDefined());
                    Assert.assertTrue(version.isNeedUpdate());
                    Assert.assertNotEquals(version.getVersion(), applicationData.getApplication()
                                                                                .getVersion() + "test");
                });
    }

    @Test
    public void checkIncorrectApplicationVersion() throws Exception {
        this.mockMvc
                .perform(get(checkApplicationVersionUrl)
                        .header("version", applicationData.getApplication().getVersion())
                        .header("platform", applicationData.getApplication()
                                                           .getShortName() + "test"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Version version = objectMapper.readValue(result.getResponse().getContentAsString(), Version.class);
                    Assert.assertFalse(version.isClientVersionIsDefined());
                    Assert.assertFalse(version.isNeedUpdate());
                    Assert.assertNull(version.getVersion());
                });
    }

    @Test
    public void checkApplicationVersionWithoutParameters() throws Exception {
        this.mockMvc
                .perform(get(checkApplicationVersionUrl))
                .andExpect(status().isBadRequest());
    }
}