package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.config.StageControllers;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.DetailsRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.ReleaseRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

@Log4j2
public class DetailsEditorController extends AbstractController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private DetailsRepository detailsRepository;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private StageControllers stageControllers;

    @Override
    public void init() {
        directoryListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        exceptionListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        groupComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Group>() {
            @Override
            public void changed(ObservableValue<? extends Group> observable, Group oldValue, Group newValue) {
                releaseComboBox.getItems().removeAll(releaseComboBox.getItems());
                Map<Release, List<Details>> releaseListMap = map.get(newValue);
                if (newValue != null) {
                    groupName.setText(newValue.toString());
                }
                if (releaseListMap == null) {
                    return;
                }
                for (Release release : releaseListMap.keySet()) {
                    if (release.isActive()) {
                        releaseComboBox.getItems().add(release);
                    }
                }
                if (releaseComboBox.getItems().size() > 0) {
                    releaseComboBox.getSelectionModel().select(0);
                }
            }
        });
        groupComboBox.setCellFactory(new Callback<ListView<Group>, ListCell<Group>>() {
            @Override
            public ListCell<Group> call(ListView<Group> param) {
                return new ListCell<Group>() {
                    @Override
                    protected void updateItem(Group item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.toString());
                            if (!item.isActive()) {
                                setTextFill(Color.RED);
                            }
                        }
                    }
                };
            }
        });
        releaseComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Release>() {
            @Override
            public void changed(ObservableValue<? extends Release> observable, Release oldValue, Release newValue) {
                if (newValue == null) {
                    releaseName.setText("");
                    return;
                }
                releaseName.setText(newValue.toString());
                defaultCheckBox.setSelected(newValue.isDefaultRelease());
                applyCheckBox.setSelected(newValue.isApplied());
                mainPane.getChildren().removeAll(mainPane.getChildren());
                directoryListView.getItems().removeAll(directoryListView.getItems());
                exceptionListView.getItems().removeAll(exceptionListView.getItems());
                descriptionArea.setText(newValue.getDescription());
            }
        });
        releaseComboBox.setCellFactory(new Callback<ListView<Release>, ListCell<Release>>() {
            @Override
            public ListCell<Release> call(ListView<Release> param) {
                return new ListCell<Release>() {
                    @Override
                    protected void updateItem(Release item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.toString());
                            if (!item.isActive()) {
                                setTextFill(Color.RED);
                            }
                        }
                    }
                };
            }
        });
        groupName.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (map.size() > 0) {
                    for (Group group : map.keySet()) {
                        if (group.getValue().equals(newValue) && group.isActive()) {
                            addGroupButton.setDisable(true);
                            removeGroupButton.setDisable(false);
                            return;
                        }
                    }
                }
                if (newValue.isBlank()) {
                    addGroupButton.setDisable(true);
                } else {
                    addGroupButton.setDisable(false);
                }
                removeGroupButton.setDisable(true);

            }
        });
        releaseName.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (map.size() > 0) {
                    Map<Release, List<Details>> releaseListMap =
                            map.get(groupComboBox.getSelectionModel().getSelectedItem());
                    if (releaseListMap != null) {
                        for (Release release : releaseListMap.keySet()) {
                            if (release.getValue().equals(newValue) && release.isActive()) {
                                addReleaseButton.setDisable(true);
                                removeReleaseButton.setDisable(false);
                                return;
                            }
                        }
                    }
                }
                if (newValue.isBlank()) {
                    addReleaseButton.setDisable(true);
                } else {
                    addReleaseButton.setDisable(false);
                }
                removeReleaseButton.setDisable(true);
            }

        });
        defaultCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    Map<Release, List<Details>> releaseListMap =
                            map.get(groupComboBox.getSelectionModel().getSelectedItem());
                    for (Release release : releaseListMap.keySet()) {
                        release.setDefaultRelease(false);
                    }
                }
                releaseComboBox.getSelectionModel().getSelectedItem().setDefaultRelease(newValue);

            }
        });
        applyCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    Map<Release, List<Details>> releaseListMap =
                            map.get(groupComboBox.getSelectionModel().getSelectedItem());
                    for (Release release : releaseListMap.keySet()) {
                        release.setApplied(false);
                    }
                }
                releaseComboBox.getSelectionModel().getSelectedItem().setApplied(newValue);
            }
        });
    }

    @Override
    public void beforeOpen() {
        mainPane.getChildren().removeAll(mainPane.getChildren());
        map = new TreeMap<>(new Comparator<Group>() {
            @Override
            public int compare(Group o1, Group o2) {
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                if (o1.equals(o2)) {
                    return 0;
                }
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        for (Group group : groupRepository.findAll()) {
            Map<Release, List<Details>> innerMap = new TreeMap<>(new Comparator<Release>() {
                @Override
                public int compare(Release o1, Release o2) {
                    if (o1 == null) {
                        return -1;
                    }
                    if (o2 == null) {
                        return 1;
                    }
                    if (o1.equals(o2)) {
                        return 0;
                    }
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
            for (Release release : releaseRepository.findAllByGroupAndActiveIsTrue(group)) {
                innerMap.put(release, detailsRepository.findAllByReleaseAndActiveIsTrue(release));
            }
            map.put(group, innerMap);
        }
        groupComboBox.getItems().removeAll(groupComboBox.getItems());
        for (Group group : map.keySet()) {
            groupComboBox.getItems().add(group);
        }
        if (map.size() > 0) {
            groupComboBox.getSelectionModel().select(0);
        }
        suffixGameDirectory.setText("Data Files");
        suffixOptional.setText("");
        icon.setText("");
        ObservableList<Node> children = mainPane.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof HBox) {
                children.remove(i);
                i--;
            }
        }
        directoryListView.getItems().removeAll(directoryListView.getItems());
    }

    @FXML
    private ComboBox<Group> groupComboBox;

    @FXML
    private TextField groupName;

    @FXML
    private Button removeGroupButton;

    @FXML
    private Button addGroupButton;

    @FXML
    private ComboBox<Release> releaseComboBox;

    @FXML
    private TextField releaseName;

    @FXML
    private Button removeReleaseButton;

    @FXML
    private Button addReleaseButton;

    @FXML
    private CheckBox defaultCheckBox;

    @FXML
    private CheckBox applyCheckBox;

    @FXML
    private TextField suffixGameDirectory;

    @FXML
    private TextField suffixOptional;

    @FXML
    private ListView<File> directoryListView;

    @FXML
    private ListView<File> exceptionListView;

    @FXML
    private TextField icon;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private VBox mainPane;

    private Map<Group, Map<Release, List<Details>>> map;

    public void addGroup() {
        if (groupComboBox.getSelectionModel().getSelectedItem() != null && groupComboBox.getSelectionModel().getSelectedItem().getValue().equals(groupName.getText())) {
            groupComboBox.getSelectionModel().getSelectedItem().setActive(true);
        } else {
            if (groupName.getText().isBlank()) {
                return;
            }
            Group group = new Group();
            group.setValue(groupName.getText());
            group.setActive(true);
            map.put(group, new TreeMap<>(new Comparator<Release>() {
                @Override
                public int compare(Release o1, Release o2) {
                    if (o1 == null) {
                        return -1;
                    }
                    if (o2 == null) {
                        return 1;
                    }
                    if (o1.equals(o2)) {
                        return 0;
                    }
                    return o1.getValue().compareTo(o2.getValue());
                }
            }));
            groupComboBox.getItems().add(group);
            groupComboBox.getSelectionModel().select(group);
        }
        addGroupButton.setDisable(true);
        removeGroupButton.setDisable(false);
    }

    public void removeGroup() {
        groupComboBox.getSelectionModel().getSelectedItem().setActive(false);
        addGroupButton.setDisable(false);
        removeGroupButton.setDisable(true);
    }

    public void addRelease() {
        if (releaseComboBox.getSelectionModel().getSelectedItem() != null && releaseComboBox.getSelectionModel().getSelectedItem().getValue().equals(releaseName.getText())) {
            releaseComboBox.getSelectionModel().getSelectedItem().setActive(true);
        } else {
            if (releaseName.getText().isBlank()) {
                return;
            }
            Release release = new Release();
            release.setValue(releaseName.getText());
            release.setActive(true);
            map.get(groupComboBox.getSelectionModel().getSelectedItem()).put(release, new ArrayList<>());
            releaseComboBox.getItems().add(release);
            releaseComboBox.getSelectionModel().select(release);
        }
        addReleaseButton.setDisable(true);
        removeReleaseButton.setDisable(false);
    }

    public void removeRelease() {
        releaseComboBox.getSelectionModel().getSelectedItem().setActive(false);
        addReleaseButton.setDisable(false);
        removeReleaseButton.setDisable(true);
    }

    public void setIconPath() {
        File file;
        do {
            file = fileManager.openFileChooser(true);
        } while (file != null && !file.getName().equals("example.jpg"));
        if (file != null) {
            icon.setText(getOptionalPath(file.getAbsolutePath()));
        }
    }

    public void plusDirectoryButton() {
        List<File> files = fileManager.openFilesChooser(true);
        if (files == null || files.size() == 0 || directoryListView.getItems().containsAll(files)) {
            return;
        }
        for (File file : files) {
            if (!directoryListView.getItems().contains(file)) {
                directoryListView.getItems().add(file);
            }
        }
    }

    public void plusDirectoryFolderButton() {
        File file = fileManager.openDirectoryChooser(true);
        if (file == null || directoryListView.getItems().contains(file)) {
            return;
        }
        directoryListView.getItems().add(file);
    }

    public void removeDirectoryButton() {
        directoryListView.getItems().remove(directoryListView.getSelectionModel().getSelectedIndex());
    }

    public void plusExceptionButton() {
        List<File> files = fileManager.openFilesChooser(true);
        if (files == null || files.size() == 0 || exceptionListView.getItems().containsAll(files)) {
            return;
        }
        for (File file : files) {
            if (!exceptionListView.getItems().contains(file)) {
                exceptionListView.getItems().add(file);
            }
        }
    }

    public void plusExceptionFolderButton() {
        File file = fileManager.openDirectoryChooser(true);
        if (file == null || exceptionListView.getItems().contains(file)) {
            return;
        }
        exceptionListView.getItems().add(file);
    }

    public void removeExceptionButton() {
        exceptionListView.getItems().remove(exceptionListView.getSelectionModel().getSelectedIndex());
    }

    public void refresh() {
        List<Details> details =
                map.get(groupComboBox.getSelectionModel().getSelectedItem()).get(releaseComboBox.getSelectionModel().getSelectedItem());
        mainPane.getChildren().removeAll(mainPane.getChildren());
        if (details != null) {
            for (Details detail : details) {
                File file = new File(fileManager.getOptionalPath(true) + detail.getStoragePath());
                boolean temp = true;
                for (File item : exceptionListView.getItems()) {
                    if (file.getAbsolutePath().contains(item.getAbsolutePath())) {
                        temp = false;
                        break;
                    }
                }
                if (temp) {
                    ResultRecord resultRecord = new ResultRecord(file);
                    resultRecord.getOptionalPath().setText(detail.getStoragePath());
                    resultRecord.getGamePath().setText(detail.getGamePath());
                    resultRecord.setDetails(detail);
                    detail.setActive(true);
                    mainPane.getChildren().add(resultRecord);
                }
            }
        }
        for (File item : directoryListView.getItems()) {
            scanDirectory(item);
        }
    }

    public void saveTemporarily() {
        List<Details> details =
                map.get(groupComboBox.getSelectionModel().getSelectedItem()).get(releaseComboBox.getSelectionModel().getSelectedItem());
        if (releaseComboBox.getSelectionModel().getSelectedItem() != null) {
            releaseComboBox.getSelectionModel().getSelectedItem().setImage(icon.getText());
            releaseComboBox.getSelectionModel().getSelectedItem().setDescription(descriptionArea.getText());
        }
        for (Object o : mainPane.getChildren()) {
            if (o instanceof ResultRecord) {
                ResultRecord resultRecord = (ResultRecord) o;
                if (resultRecord.getDetails() == null) {
                    Details newDetails = new Details();
                    newDetails.setActive(true);
                    newDetails.setGamePath(resultRecord.gamePath.getText());
                    newDetails.setStoragePath(resultRecord.optionalPath.getText());
                    newDetails.setRelease(releaseComboBox.getSelectionModel().getSelectedItem());
                    details.add(newDetails);
                } else {
                    resultRecord.getDetails().setStoragePath(resultRecord.optionalPath.getText());
                    resultRecord.getDetails().setGamePath(resultRecord.gamePath.getText());
                    resultRecord.getDetails().setRelease(releaseComboBox.getSelectionModel().getSelectedItem());
                }
            }
        }
    }

    public void saveInBase() {
        saveTemporarily();
        for (Map.Entry<Group, Map<Release, List<Details>>> groupMapEntry : map.entrySet()) {
            groupRepository.save(groupMapEntry.getKey());
            for (Map.Entry<Release, List<Details>> releaseListEntry : groupMapEntry.getValue().entrySet()) {
                releaseListEntry.getKey().setGroup(groupMapEntry.getKey());
                releaseRepository.save(releaseListEntry.getKey());
                for (Details details : releaseListEntry.getValue()) {
                    details.setRelease(releaseListEntry.getKey());
                    detailsRepository.save(details);
                }

            }
        }
    }

    public void cancel() throws IOException {
        stageControllers.getDetailsEditorStage().close();
    }

    public void createScript() {
        File temp = new File("");
        File file = new File(temp.getAbsolutePath() + FileManager.SEPARATOR + "script" + FileManager.SEPARATOR +
                "script.sql");
        if (!file.exists()) {
            try {
                if (!file.getParentFile().exists()) {
                    if (file.getParentFile().mkdirs()) {
                        Files.createFile(file.toPath());
                    } else {
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, Charset.forName("UTF-8")))) {
            for (Map.Entry<Group, Map<Release, List<Details>>> groupMapEntry : map.entrySet()) {
                if (!groupMapEntry.getKey().isActive()) {
                    continue;
                }
                String addGroup = String.format("INSERT INTO PLUGIN_GROUP (VALUE) VALUES('%s');\n",
                        groupMapEntry.getKey().getValue());
                writer.write(addGroup);
                for (Map.Entry<Release, List<Details>> releaseListEntry : groupMapEntry.getValue().entrySet()) {
                    Release release = releaseListEntry.getKey();
                    if (!release.isActive()) {
                        continue;
                    }
                    String addRelease = String.format("INSERT INTO RELEASE (PLUGIN_GROUP_ID, VALUE, DEFAULT, APPLIED,"
                            + " IMAGE_PATH, DESCRIPTION) " + "SELECT ID, " + "'%s', %S, %S, %s, '%s' FROM " +
                            "PLUGIN_GROUP " + "WHERE VALUE IN ('%s');\n", release.getValue(),
                            release.isDefaultRelease(), release.isApplied(), release.getImage() == null ? "NULL" :
                                                                             "'" + release.getImage() + "'",
                            release.getDescription(), groupMapEntry.getKey().getValue());
                    writer.write(addRelease);
                    releaseListEntry.getKey().setGroup(groupMapEntry.getKey());
                    for (Details details : releaseListEntry.getValue()) {
                        if (details.isActive()) {
                            String addDetails = String.format("INSERT INTO DETAILS (RELEASE_ID, STORAGE_PATH, " +
                                    "GAME_PATH) SELECT release.ID, '%s', '%s' FROM RELEASE release INNER " + "JOIN " + "PLUGIN_GROUP plugin_group ON release.PLUGIN_GROUP_ID = " + "plugin_group.ID " + "WHERE plugin_group.VALUE IN ('%s') AND release.VALUE IN ('%s');\n", details.getStoragePath(), details.getGamePath(), groupMapEntry.getKey().getValue(), release.getValue());
                            writer.write(addDetails);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scanDirectory(File file) {
        if (file != null && file.exists()) {
            if (!exceptionListView.getItems().contains(file)) {
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File innerFile : files) {
                            scanDirectory(innerFile);
                        }
                    }
                } else {
                    checkSingleFile(file);
                }
            }
        }
    }

    private void checkSingleFile(File file) {
        if (file.getName().equals("example.jpg")) {
            return;
        }
        if (file.isFile()) {
            ResultRecord resultRecord = new ResultRecord(file);
            if (!mainPane.getChildren().contains(resultRecord)) {
                resultRecord.getGamePath().setText(getGamePath(file.getAbsolutePath()));
                resultRecord.getOptionalPath().setText(getOptionalPath(file.getAbsolutePath()));
                mainPane.getChildren().add(resultRecord);
            }
        }
    }

    private String getGamePath(String path) {
        return path.replace(fileManager.getOptionalPath(true) + suffixOptional.getText() + FileManager.SEPARATOR,
                suffixGameDirectory.getText() + FileManager.SEPARATOR);
    }

    private String getOptionalPath(String path) {
        return path.replace(fileManager.getOptionalPath(true), "");
    }

    private class ResultRecord extends HBox {

        @Setter
        @Getter
        private Details details;

        @Getter
        private File path;

        private ResultRecord resultRecord;

        private Button button = new Button();

        @Getter
        private TextField gamePath = new TextField();

        @Getter
        private TextField optionalPath = new TextField();

        ResultRecord(File path) {
            super();
            this.path = path;
            resultRecord = this;
            button.setText("-");
            button.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    mainPane.getChildren().remove(resultRecord);
                    exceptionListView.getItems().add(path);
                    if (details != null) {
                        details.setActive(false);
                    }
                }
            });
            getChildren().addAll(gamePath, optionalPath, button);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ResultRecord that = (ResultRecord) o;
            return Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }
}