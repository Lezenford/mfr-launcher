package ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.fullrest.mfr.plugins_configuration_utility.controller.AbstractController;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.BaseEntity;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.DetailsRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.ReleaseRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.service.RepositoryService;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class DetailsEditorController implements AbstractController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private DetailsRepository detailsRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private AddDetailsController addDetailsController;

    @Autowired
    private AddBaseEntityController addBaseEntityController;

    @Autowired
    private StageManager stageManager;

    private List<Group> groups;

    private List<BaseEntity> removedEntity;

    @FXML
    private ComboBox<Group> groupComboBox;

    @FXML
    private ComboBox<Release> releaseComboBox;

    @FXML
    private Button deleteGroupButton;

    @FXML
    private Button deleteReleaseButton;

    @FXML
    private Button newGroupButton;

    @FXML
    private Button newReleaseButton;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button addNewFilesButton;

    @FXML
    private ListView<DetailsHBox> detailsList;

    @FXML
    private Button applyButton;

    @FXML
    private ImageView image;

    @FXML
    private Button imageButton;

    @FXML
    private Button deleteImageButton;

    @FXML
    private Button closeButton;

    @FXML
    private Button createSchemaButton;

    @Transactional
    public void init() {
        //Group change listener
        groupComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            releaseComboBox.getItems().clear();
            if (newValue != null) {
                releaseComboBox.getItems().addAll(newValue.getReleases());
                if (releaseComboBox.getItems().size() > 0) {
                    releaseComboBox.getSelectionModel().select(0);
                }
                deleteGroupButton.setDisable(false);
            } else {
                deleteGroupButton.setDisable(true);
            }
        });

        //Add new group
        newGroupButton.setOnAction(action -> {
            createSchemaButton.setDisable(true);
            addBaseEntityController.beforeOpen();
            addBaseEntityController.getLabel().setText("Добавить новую группу");
            stageManager.getAddBaseEntityStage().showAndWait();
            if (addBaseEntityController.getText() != null) {
                Group group = new Group();
                group.setReleases(new ArrayList<>());
                group.setValue(addBaseEntityController.getText());
                groups.add(group);
                groupComboBox.getItems().clear();
                groupComboBox.getItems().addAll(groups);
                groupComboBox.getSelectionModel().select(group);
            }
        });

        //Release change listener
        releaseComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            detailsList.getItems().clear();
            image.setImage(null);
            if (newValue != null) {
                newValue.getDetails().forEach(details -> detailsList.getItems().add(new DetailsHBox(details)));
                descriptionArea.setText(newValue.getDescription());
                try {
                    image.setImage(new Image(new File(fileManager.getOptionalPath(true) +
                            newValue.getImage()).toURI().toURL().toString()));
                } catch (MalformedURLException e) {
                    log.error(String.format("Can't use image for %s\n", newValue.getImage()), e);
                }
                deleteReleaseButton.setDisable(false);
            } else {
                deleteReleaseButton.setDisable(true);
                descriptionArea.setText("");
            }
        });

        //Add new release
        newReleaseButton.setOnAction(action -> {
            createSchemaButton.setDisable(true);
            addBaseEntityController.beforeOpen();
            addBaseEntityController.getLabel().setText("Добавить новый пакет");
            stageManager.getAddBaseEntityStage().showAndWait();
            if (addBaseEntityController.getText() != null) {
                Release release = new Release();
                release.setDetails(new ArrayList<>());
                release.setGroup(groupComboBox.getSelectionModel().getSelectedItem());
                release.setValue(addBaseEntityController.getText());
                groupComboBox.getSelectionModel().getSelectedItem().getReleases().add(release);
                releaseComboBox.getItems().clear();
                releaseComboBox.getItems().addAll(groupComboBox.getSelectionModel().getSelectedItem().getReleases());
                releaseComboBox.getSelectionModel().select(release);
            }
        });

        //DeleteGroupButton event
        deleteGroupButton.setOnAction(event -> {
            createSchemaButton.setDisable(true);
            Group selectedItem = groupComboBox.getSelectionModel().getSelectedItem();
            groups.remove(selectedItem);
            removedEntity.add(selectedItem);
            groupComboBox.getItems().remove(selectedItem);
        });

        //DeleteReleaseButton event
        deleteReleaseButton.setOnAction(event -> {
            createSchemaButton.setDisable(true);
            Release selectedItem = releaseComboBox.getSelectionModel().getSelectedItem();
            groupComboBox.getSelectionModel().getSelectedItem().getReleases().remove(selectedItem);
            removedEntity.add(selectedItem);
            releaseComboBox.getItems().remove(selectedItem);
        });

        //Add new details
        addNewFilesButton.setOnAction(event -> {
            List<Details> newDetails = new ArrayList<>();
            addDetailsController.beforeOpen();
            addDetailsController.setDetails(newDetails);
            stageManager.getAddDetailsStage().showAndWait();
            newDetails.forEach(details -> {
                Release release = releaseComboBox.getSelectionModel().getSelectedItem();
                release.getDetails().add(details);
                details.setRelease(release);
                detailsList.getItems().add(new DetailsHBox(details));
            });
        });

        //Apply all changes
        applyButton.setOnAction(event -> {
            groupRepository.saveAll(groups);
            removedEntity.forEach(baseEntity -> {
                if (baseEntity instanceof Details) {
                    detailsRepository.deleteById(baseEntity.getId());
                }
                if (baseEntity instanceof Release) {
                    releaseRepository.deleteById(baseEntity.getId());
                }
                if (baseEntity instanceof Group) {
                    groupRepository.deleteById(baseEntity.getId());
                }
            });
            removedEntity.clear();
            beforeOpen();
        });

        //Choose new image
        imageButton.setOnAction(event -> {
            createSchemaButton.setDisable(true);
            File file = fileManager.openFileChooser(true);
            if (file != null && file.exists()) {
                String relativePath = fileManager.getRelativePath(file, "", "", true);
                try {
                    image.setImage(new Image(file.toURI().toURL().toString()));
                    releaseComboBox.getSelectionModel().getSelectedItem().setImage(relativePath);
                } catch (MalformedURLException e) {
                    log.error("Can't use new image\n", e);
                }
            }
        });

        //Remove current image
        deleteImageButton.setOnAction(event -> {
            createSchemaButton.setDisable(true);
            image.setImage(null);
            releaseComboBox.getSelectionModel().getSelectedItem().setImage("");
        });

        //Close stage
        closeButton.setOnAction(event -> stageManager.getDetailsEditorStage().close());

        //Create new schema
        createSchemaButton.setOnAction(event -> {
            fileManager.createSchemaFile(groups);
            createSchemaButton.setDisable(true);
        });

        //Edit description
        descriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            releaseComboBox.getSelectionModel().getSelectedItem().setDescription(newValue);
        });

        image.setFitHeight(200);
        image.setFitWidth(200);
    }

    @Override
    public void
    beforeOpen() {
        //Init information
        groups = repositoryService.getAllGroupsWithLazyInit();
        removedEntity = new ArrayList<>();

        //Init elements
        groupComboBox.getItems().clear();
        groupComboBox.getItems().addAll(groups);
        if (groupComboBox.getItems().size() > 0) {
            groupComboBox.getSelectionModel().select(0);
        }
        createSchemaButton.setDisable(false);
    }

    private class DetailsHBox extends HBox {

        @Getter
        private final Details details;

        private final TextField gamePath;

        private final TextField optionalPath;

        private final Button removeButton = new Button("-");

        DetailsHBox(Details details) {
            this.details = details;
            gamePath = new TextField(details.getGamePath());
            optionalPath = new TextField(details.getStoragePath());

            removeButton.setOnAction(event -> {
                createSchemaButton.setDisable(true);
                detailsList.getItems().remove(this);
                releaseComboBox.getSelectionModel().getSelectedItem().getDetails().remove(details);
                removedEntity.add(details);
            });
            gamePath.textProperty().addListener((observable, oldValue, newValue) -> {
                createSchemaButton.setDisable(true);
                details.setGamePath(newValue);
            });
            optionalPath.textProperty().addListener((observable, oldValue, newValue) -> {
                createSchemaButton.setDisable(true);
                details.setStoragePath(newValue);
            });
            this.getChildren().addAll(gamePath, optionalPath, removeButton);
            removeButton.getStyleClass().add("short");
            this.setAlignment(Pos.CENTER);
        }
    }
}