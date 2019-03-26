package ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.controller.AbstractController;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
public class AddDetailsController implements AbstractController {

    @Autowired
    private StageManager stageManager;

    @Autowired
    private FileManager fileManager;

    @Setter
    @Getter
    private List<Details> details;

    @FXML
    private Button addFolderButton;

    @FXML
    private Button addFilesButton;

    @FXML
    private TextField gamePrefix;

    @FXML
    private TextField optionalPrefix;

    @FXML
    private ListView<InnerHBox> fileList;

    @FXML
    private Button applyButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label gamePath;

    @FXML
    private Label optionalPath;

    public void init() {
        cancelButton.setOnAction(event -> stageManager.getAddDetailsStage().close());

        //init folder buttons
        addFolderButton.setOnAction(event -> addFilesToPreviousDetails(fileManager.openDirectoryChooser(true)));
        addFilesButton.setOnAction(event -> addFilesToPreviousDetails(fileManager.openFilesChooser(true)));

        optionalPrefix.textProperty().addListener((observable, oldValue, newValue) ->
                fileList.getItems().forEach(InnerHBox::updateRelativePath));
        gamePrefix.textProperty().addListener((observable, oldValue, newValue) ->
                fileList.getItems().forEach(InnerHBox::updateRelativePath));

        applyButton.setOnAction(event -> {
            fileList.getItems().forEach(innerHBox -> details.add(innerHBox.getDetails()));
            stageManager.getAddDetailsStage().close();
        });
    }

    @Override
    public void beforeOpen() {
        //Init prefixes
        optionalPrefix.setText("");
        gamePrefix.setText("Data Files");
        gamePath.setText(fileManager.getGamePath(true) + "...");
        optionalPath.setText(fileManager.getOptionalPath(true) + "...");

        //Clean list
        fileList.getItems().clear();

    }

    private void addFilesToPreviousDetails(File file) {
        if (file != null) {
            if (file.isFile()) {
                addFilesToPreviousDetails(Collections.singletonList(file));
            }
            if (file.isDirectory()) {
                List<File> files = fileManager.getFilesFromDirectory(file, new ArrayList<>());
                addFilesToPreviousDetails(files);
            }
        }
    }

    private void addFilesToPreviousDetails(List<File> files) {
        for (File file : files) {
            if (file != null && file.exists() && file.isFile()) {
                InnerHBox innerHBox = new InnerHBox(file);
                fileList.getItems().add(innerHBox);
            }
        }
    }

    private class InnerHBox extends HBox {
        private final File file;

        @Setter
        @Getter
        private TextField gamePath;

        @Setter
        @Getter
        private TextField optionalPath;

        private final Button removeButton = new Button("-");

        InnerHBox(File file) {
            this.file = file;
            gamePath = new TextField(fileManager
                    .getRelativePath(file, gamePrefix.getText(), optionalPrefix.getText(), false));
            gamePath.setMinWidth(300);
            optionalPath = new TextField(fileManager
                    .getRelativePath(file, gamePrefix.getText(), optionalPrefix.getText(), true));
            optionalPath.setMinWidth(300);
            removeButton.setOnAction(event -> fileList.getItems().remove(this));
            this.getChildren().addAll(gamePath, optionalPath, removeButton);
            this.setAlignment(Pos.CENTER);
        }

        void updateRelativePath() {
            gamePath.setText(fileManager.getRelativePath(file, gamePrefix.getText(), optionalPrefix.getText(), false));
        }

        Details getDetails() {
            Details details = new Details();
            details.setGamePath(gamePath.getText());
            details.setStoragePath(optionalPath.getText());
            return details;
        }
    }
}
