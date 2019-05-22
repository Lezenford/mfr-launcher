package ru.fullrest.mfr.server.controller;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ru.fullrest.mfr.server.ServerApplication;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.entity.mock.ApplicationData;
import ru.fullrest.mfr.server.model.entity.mock.UpdateData;
import ru.fullrest.mfr.server.model.repository.ApplicationRepository;
import ru.fullrest.mfr.server.model.repository.UpdateRepository;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource("classpath:application.properties")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServerApplication.class)
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class AdminControllerTest {

    private final UpdateData updateData = new UpdateData();
    private final ApplicationData applicationData = new ApplicationData();

    private final String baseUrl = "/admin/";
    private final String updateUrl = "/admin/game/update";
    private final String applicationUrl = "/admin/application";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UpdateRepository updateRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Before
    public void setUp() {
        updateRepository.saveAll(updateData.getUpdates());
        applicationRepository.save(applicationData.getApplication());
    }

    @After
    public void clean() {
        updateRepository.deleteAll();
        applicationRepository.deleteAll();
    }

    @Test
    public void incorrectPage() throws Exception {
        this.mockMvc.perform(get(baseUrl + "/test"))
                    .andExpect(status().isNotFound());
    }

    @Test
    public void welcome() throws Exception {
        this.mockMvc.perform(get(baseUrl))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"));
    }

    @Test
    public void gamePlatforms() throws Exception {
        this.mockMvc.perform(get(updateUrl))
                    .andExpect(status().isOk())
                    .andExpect(view().name("game"));
    }

    @Test
    public void gameUpdatesList() throws Exception {
        for (Update update : updateData.getUpdates()) {
            this.mockMvc.perform(get(String.format("%s/%s", updateUrl, update.getPlatform())))
                        .andExpect(status().isOk())
                        .andExpect(view().name("game"))
                        .andExpect(model().attributeExists("updates"))
                        .andExpect(model().attribute("updates", hasSize(updateRepository
                                .findAllByPlatformOrderByIdAsc(update.getPlatform()).size())));
        }
    }

    @Test
    public void gameUpdatesListForIncorrectPlatform() throws Exception {
        this.mockMvc.perform(get(String.format("%s/%s", updateUrl, "test")))
                    .andExpect(status().isNotFound());
    }

    @Test
    public void changeGameUpdate() throws Exception {
        Update update = updateData.getUpdates().get(0);
        this.mockMvc.perform(get(String
                .format("%s/%s/%s/change?active=%s", updateUrl, update.getPlatform(), update.getId(), !update
                        .isActive())))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl(String.format("%s/%s", updateUrl, update.getPlatform())))
                    .andDo(result ->
                            Assert.assertEquals(updateRepository.findById(update.getId()).get().isActive(),
                                    !update.isActive()));
    }

    @Test
    public void changeGameUpdateIncorrectPlatform() throws Exception {
        Update update = updateData.getUpdates().get(0);
        boolean active = update.isActive();
        String wrongPlatform = update.getPlatform() + "_1";
        this.mockMvc.perform(get(String
                .format("%s/%s/%s/change?active=%s", updateUrl, wrongPlatform, update.getId(),
                        !update.isActive())))
                    .andExpect(status().isBadRequest())
                    .andExpect(redirectedUrl(null))
                    .andDo(result ->
                            Assert.assertEquals(active, update.isActive()));
    }

    @Test
    public void changeGameUpdateIncorrectIdForSelectedPlatform() throws Exception {
        Update firstPlatform = updateData.getUpdates().get(0);
        Update secondPlatform = new Update();
        for (Update update : updateData.getUpdates()) {
            if (!firstPlatform.getPlatform().equals(update.getPlatform())) {
                secondPlatform = update;
                break;
            }
        }
        boolean active = firstPlatform.isActive();
        this.mockMvc.perform(get(String
                .format("%s/%s/%s/change?active=%s", updateUrl, secondPlatform.getPlatform(), firstPlatform.getId(),
                        !firstPlatform.isActive())))
                    .andExpect(status().isBadRequest())
                    .andExpect(redirectedUrl(null))
                    .andDo(result ->
                            Assert.assertEquals(active, firstPlatform.isActive()));
    }

    @Test
    public void createPlatform() throws Exception {
        this.mockMvc.perform(get(String.format("%s/create", updateUrl)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("game_upload"));
    }

    @Test
    public void uploadNewPatch() throws Exception {
        this.mockMvc.perform(get(String.format("%s/upload", updateUrl)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("game_upload"))
                    .andExpect(model().attributeExists("step"))
                    .andExpect(model().attribute("step", equalTo(1)));
    }

    @Test
    public void uploadNewPatchToExistingPlatformStepTwo() throws Exception {
        String platform = updateData.getUpdates().get(0).getPlatform();
        String version = updateData.getUpdates().get(0).getVersion() + "test";
        this.mockMvc.perform(post(String.format("%s/upload/step2", updateUrl))
                .param("platform", platform)
                .param("version", version)
                .with(csrf()))
                    .andExpect(status().isFound())
                    .andDo(result -> Assert
                            .assertNotNull(updateRepository.findByVersionAndPlatform(version, platform)));
    }

    @Test
    public void uploadNewPatchToUnknownPlatformStepTwo() throws Exception {
        String platform = updateData.getUpdates().get(0).getPlatform() + "test";
        String version = updateData.getUpdates().get(0).getVersion() + "test";
        this.mockMvc.perform(post(String.format("%s/upload/step2", updateUrl))
                .param("platform", platform)
                .param("version", version)
                .with(csrf()))
                    .andExpect(status().isFound())
                    .andDo(result -> Assert
                            .assertNotNull(updateRepository.findByVersionAndPlatform(version, platform)));
    }

    @Test
    public void applicationPage() throws Exception {
        this.mockMvc.perform(get(applicationUrl))
                    .andExpect(status().isOk())
                    .andExpect(view().name("application"))
                    .andExpect(model().attributeExists("applications"));
    }

    @Test
    public void uploadNewApplication() throws Exception {
        this.mockMvc.perform(get(String.format("%s/upload", applicationUrl)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("application_update"))
                    .andExpect(model().attributeExists("app"))
                    .andExpect(model().attributeDoesNotExist("error"))
                    .andExpect(model().attributeExists("title"));
    }

    @Test
    public void uploadNewApplicationWithError() throws Exception {
        String error = "test";
        this.mockMvc.perform(get(String.format("%s/upload?error=%s", applicationUrl, error)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("application_update"))
                    .andExpect(model().attributeExists("app"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", equalTo(error)))
                    .andExpect(model().attributeExists("title"));
    }

    @Test
    public void updateExistPlatform() throws Exception {
        String shortName = applicationData.getApplication().getShortName();

        this.mockMvc.perform(get(String.format("%s/update/%s", applicationUrl, shortName)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("application_update"))
                    .andExpect(model().attributeExists("app"))
                    .andExpect(model().attributeDoesNotExist("error"))
                    .andExpect(model().attributeExists("title"));
    }

    @Test
    public void updateUnknownPlatform() throws Exception {
        String shortName = applicationData.getApplication().getShortName() + "test";
        this.mockMvc.perform(get(String.format("%s/update/%s", applicationUrl, shortName)))
                    .andExpect(status().isNotFound());
    }
}