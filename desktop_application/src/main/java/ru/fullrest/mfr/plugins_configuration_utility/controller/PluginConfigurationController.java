package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.config.PropertiesConfiguration;
import ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor.DetailsEditorController;
import ru.fullrest.mfr.plugins_configuration_utility.javafx.View;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.ReleaseRepository;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@Log4j2
public class PluginConfigurationController implements AbstractController {

    @Autowired
    private PropertiesConfiguration configuration;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private View<ProgressController> progressView;

    @Autowired
    private View<DetailsEditorController> detailsEditorView;

    @Autowired
    private StageManager stageManager;

    @Autowired
    private FileManager fileManager;

    private Map<Group, ToggleGroup> groupMap;

    @FXML
    private Button acceptButton;

    @FXML
    private Button cancelButton;

    @FXML
    private VBox groupVBox;

    @FXML
    private VBox releaseVBox;

    @FXML
    private ImageView imageView;

    @FXML
    private VBox descriptionVBox;

    @FXML
    private Button editButton;

    @SuppressWarnings("unchecked")
    @Override
    public void beforeOpen() {
        editButton.setDisable(!configuration.isExtendedMod());
        groupMap = new HashMap<>();
        acceptButton.setDisable(true);
        groupVBox.getChildren().removeAll(groupVBox.getChildren());
        releaseVBox.getChildren().removeAll(releaseVBox.getChildren());
        ToggleGroup groupButtons = new ToggleGroup();
        for (Group group : groupRepository.findAllByOrderByValue()) {
            VBox space = new VBox(); //space between buttons
            space.setMinHeight(10);
            space.setMaxHeight(10);
            groupVBox.getChildren().add(space);
            ToggleButton groupButton = new ToggleButton();
            groupButton.setToggleGroup(groupButtons);
            groupButton.setText(group.getValue());
            groupButton.getStyleClass().add("longConfigPage");
            groupVBox.getChildren().add(groupButton);
            ToggleGroup toggleGroup = new ToggleGroup();
            List<RadioButton> buttons = new ArrayList<>();
            for (Release release : releaseRepository.findAllByGroup(group)) {
                RadioButton button = new RadioButton(release.getValue());
                button.setToggleGroup(toggleGroup);
                button.setUserData(release);
                if (release.isApplied()) {
                    button.setSelected(true);
                }
                buttons.add(button);
                button.setOnMouseClicked(event -> {
                    setRelease(release);
                    acceptButton.setDisable(false);
                });
            }
            groupMap.put(group, toggleGroup);
            groupButton.setUserData(buttons);
        }
        groupButtons.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                releaseVBox.getChildren().removeAll(releaseVBox.getChildren());
                descriptionVBox.getChildren().removeAll(descriptionVBox.getChildren());
                for (RadioButton button : (List<RadioButton>) newValue.getUserData()) {
                    releaseVBox.getChildren().add(button);
                    if (button.isSelected()) {
                        setRelease((Release) button.getUserData());
                    }
                }
            } else {
                oldValue.setSelected(true); //Do not allow to disable all buttons
            }
        });
        if (groupButtons.getToggles().size() > 0) {
            groupButtons.getToggles().get(0).setSelected(true);
        }
    }

    public void accept() {
        progressView.getController().beforeOpen();
        Map<Group, Release> resultMap = new HashMap<>();
        groupMap.forEach((group, toggleGroup) -> {
            if (toggleGroup.getSelectedToggle() != null &&
                    toggleGroup.getSelectedToggle().getUserData() instanceof Release) {
                resultMap.put(group, (Release) toggleGroup.getSelectedToggle().getUserData());
            }
        });
        progressView.getController().acceptPluginChanges(resultMap);
        stageManager.getProgressStage().show();
        acceptButton.setDisable(true);
    }

    public void openScriptCreator() {
        detailsEditorView.getController().beforeOpen();
        stageManager.getPluginConfigurationStage().hide();
        stageManager.getApplicationStage().hide();
        stageManager.getDetailsEditorStage().show();
    }

    public void close() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    private void setRelease(Release release) {
        descriptionVBox.getChildren().removeAll(descriptionVBox.getChildren());
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setMaxWidth(205);
        if (release.getDescription() != null) {
            textArea.setText(release.getDescription());
        }
        descriptionVBox.getChildren().add(textArea);
        if (release.getImage() != null && !release.getImage().isBlank()) {
            String path = null;
            try {
                path = new File(fileManager.getOptionalPath(true) + release.getImage()).toURI().toURL().toString();
            } catch (MalformedURLException e) {
                log.error(String.format("Can't use image for %s\n", release.getImage()), e);
            }
            Image image;
            if (path != null) {
                image = new Image(path);
                imageView.setImage(image);
            } else {
                imageView.setImage(new Image(""));
            }
        }
    }
}