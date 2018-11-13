package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.config.ConfigurationControllers;
import ru.fullrest.mfr.plugins_configuration_utility.config.StageControllers;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.DetailsRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.ReleaseRepository;

import java.io.IOException;
import java.io.InputStream;
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
public class PluginConfigurationController extends AbstractController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private DetailsRepository detailsRepository;

    @Autowired
    private ConfigurationControllers configurationControllers;

    @Autowired
    private StageControllers stageControllers;

    @Autowired
    private FileManager fileManager;

    private Map<Group, ToggleGroup> groupMap;

    @Override
    public void beforeOpen() {
        groupMap = new HashMap<>();
        acceptButton.setDisable(true);
        groupVBox.getChildren().removeAll(groupVBox.getChildren());
        releaseVBox.getChildren().removeAll(releaseVBox.getChildren());
        ToggleGroup groupButtons = new ToggleGroup();
        for (Group group : groupRepository.findAllByActiveIsTrue()) {
            ToggleButton groupButton = new ToggleButton();
            groupButton.setToggleGroup(groupButtons);
            groupButton.setText(group.getValue());
            groupButton.getStyleClass().add("longConfigPage");
            groupButton.setPadding(new Insets(0, 0, 3, 0));
            groupVBox.getChildren().add(groupButton);
            ToggleGroup toggleGroup = new ToggleGroup();
            List<RadioButton> buttons = new ArrayList<>();
            for (Release release : releaseRepository.findAllByGroupAndActiveIsTrue(group)) {
                RadioButton button = new RadioButton(release.getValue());
                button.setToggleGroup(toggleGroup);
                button.setUserData(release);
                if (release.isApplied()) {
                    button.setSelected(true);
                }
                buttons.add(button);
                button.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        setRelease(release);
                        acceptButton.setDisable(false);
                    }
                });
            }
            groupMap.put(group, toggleGroup);
            groupButton.setUserData(buttons);
        }
        groupButtons.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
                if (newValue != null) {
                    releaseVBox.getChildren().removeAll(releaseVBox.getChildren());
                    descriptionVBox.getChildren().removeAll(descriptionVBox.getChildren());
                    for (RadioButton button : (List<RadioButton>) newValue.getUserData()) {
                        releaseVBox.getChildren().add(button);
                        if (button.isSelected()) {
                            setRelease((Release) button.getUserData());
                        }
                    }
                }
            }
        });
        if (groupButtons.getToggles().size() > 0) {
            groupButtons.getToggles().get(0).setSelected(true);
        }
    }

    @FXML
    private Button acceptButton;

    @FXML
    private Button cancelButton;

    @FXML
    private VBox groupVBox;

    @FXML
    private VBox releaseVBox;

    @FXML
    private HBox iconHBox;

    @FXML
    private VBox descriptionVBox;

    public void accept() {
        try {
            configurationControllers.getProgressView().getController().beforeOpen();
            configurationControllers.getProgressView().getController().acceptPluginChanges(groupMap);
            stageControllers.getProgressStage().show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openScriptCreator() {
        try {
            configurationControllers.getDetailsEditorView().getController().beforeOpen();
            stageControllers.getPluginConfigurationStage().hide();
            stageControllers.getApplicationStage().hide();
            stageControllers.getDetailsEditorStage().show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        iconHBox.getChildren().removeAll(iconHBox.getChildren());
        if (release.getImage() != null && !release.getImage().isBlank()) {
            InputStream imageInputStream = fileManager.getInputStreamFromFile(release.getImage());
            Image image;
            if (imageInputStream != null) {
                image = new Image(imageInputStream);
                ImageView imageView = new ImageView(image);
                imageView.setFitHeight(200.0);
                imageView.setFitWidth(200.0);
                iconHBox.getChildren().add(imageView);
            }
        }
    }
}
