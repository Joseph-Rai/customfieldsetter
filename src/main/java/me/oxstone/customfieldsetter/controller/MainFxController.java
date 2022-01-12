package me.oxstone.customfieldsetter.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import me.oxstone.customfieldsetter.JavaFxApplication;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@FxmlView("MainFx.fxml")
public class MainFxController {

    @FXML
    private MenuItem menuClose;

    @FXML
    private MenuItem menuAbout;

    @FXML
    private Tab tabSourceEditEnabler;

    @FXML
    private Label lblFolderPath;

    @FXML
    private TextField txtFolderPath;

    @FXML
    private Button btnSearch;

    @FXML
    private Button btnDetect;

    @FXML
    private Button btnEnabler;

    @FXML
    private GridPane gridPane;

    @FXML
    private HBox hBox;

    private StringBuilder builder;
    private File file;
    private String msg;
    private static final String DESKTOP_PATH = System.getProperty("user.home") + "\\Desktop";
//    private static final String DEFAULT_PATH = "C:\\Users\\zbflz\\Documents\\Studio 2021\\Projects";

    @Autowired
    public MainFxController() {
    }

    @FXML
    void clickBtnDetect(ActionEvent event) {
        if (txtFolderPath.getText().equals("")) {
            String title = "Error";
            String header = "Error";
            String msg = "No file selected.";
            showMsgbox(title, header, msg, Alert.AlertType.ERROR);
            return;
        }

        try {
            String filePath = txtFolderPath.getText();
            file = new File(filePath);
            if (file.exists()) { // 파일이 존재하지 않으면
                //파일 읽기
                BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
                String line = null;
                builder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append(System.lineSeparator());
                }

                // 기존에 추가된 필드 제거
                for (int i = 0; i < gridPane.getRowCount() * gridPane.getColumnCount(); i++) {
                    gridPane.getChildren().remove(0);
                }

                // 커스텀 필드 정보 얻어오기
                Set<String> customFields = new HashSet<>();
                customFields = detectCustomFields(builder);
                if (customFields.size() <= 0) {
                    throw new Exception();
                }

                // 커스텀 필드 개수만큼 Grid 추가
                for (String fieldName : customFields) {
                    int rowIndex = gridPane.getRowCount();
                    gridPane.addRow(rowIndex, new Label("Field" + (rowIndex + 1) + ":") , new Label(fieldName),
                        // 컴보박스 준비
                        new ComboBox(
                            FXCollections.observableArrayList(
                                "Text",
                                "Multiple Text",
                                "Multiple Text",
                                "Date/Time",
                                "List",
                                "Multiple List",
                                "Number"
                            )
                        )
                    );
                }
                btnEnabler.setVisible(true);
                hBox.setPrefHeight(30);
                hBox.setPadding(new Insets(10));
                gridPane.setPadding(new Insets(10));
                gridPane.getScene().getWindow().sizeToScene();

                reader.close();
            } else {
                // 파일이 존재하지 않을 시 에러발생
                throw new Exception();
            }

            btnEnabler.setDisable(false);

        } catch(Exception exception){
            // 실패 메세지 띄우기
            String title = "Fail!";
            String header = "Fail to detect custom fields.";
            msg = "It seems that there are no custom fields.";
            showMsgbox(title, header, msg, Alert.AlertType.ERROR);
        }
    }

    private Set<String> detectCustomFields(StringBuilder sb) {
        Set<String> result = new HashSet<>();
        String pattern = "(prop type=\")(?!x-)([^\":]+)(\")";
        Matcher matcher = Pattern.compile(pattern).matcher(sb.toString());
        while (matcher.find()) {
            String customField = matcher.group().replaceFirst(pattern,"$2");
            result.add(customField);
        }
        return result;
    }

    @FXML
    void clickBtnEnabler(ActionEvent event) throws IOException {
        // Field Type 맵
        Map<String, String> fieldTypes = new HashMap<>();
        fieldTypes.put("Text", "SingleString");
        fieldTypes.put("Multiple Text", "MultipleString");
        fieldTypes.put("Date/Time", "DateTime");
        fieldTypes.put("List", "SinglePicklist");
        fieldTypes.put("Multiple List", "MultiplePicklist");
        fieldTypes.put("Number", "Integer");

        // 정규식 패턴설정
        String patternCreationTool = "(creationtool=\")([^\"]+)(\")";
        String patternCreationToolVersion = "(creationtoolversion=\")([^\"]+)(\")";
        String patternOTmf = "(o-tmf=\")([^\"]+)(\")";
        String result = builder.toString();

        // Header 정보 수정
        result = result.replaceFirst(patternCreationTool, "$1SDL Language Platform$3");
        result = result.replaceFirst(patternCreationToolVersion, "$18.1$3");
        result = result.replaceFirst(patternOTmf, "$1SDL TM8 Format$3");

        // Custom Field 정보 수정
        for (int i = 0; i < gridPane.getRowCount(); i++) {
            Label field = (Label) getNode(i,1);
            ComboBox<String> fieldType = (ComboBox<String>) getNode(i, 2);
            String patternProp = "(prop type=\")(" + field.getText() + ")(\")";
            result = result.replaceAll(patternProp, "$1x-$2:" + fieldTypes.get(fieldType.getValue()) + "$3");
        }

        //파일 쓰기
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
        writer.write(result);
        writer.flush();

        // 스트림 종료
        writer.close();

        // 성공 메세지 띄우기
        String title = "Succeed";
        String header = "Custom field setup has been completed successfully.";
        msg = "Your custom fields are set successfully for TRADOS STUDIO 2021.";
        showMsgbox(title, header, msg, Alert.AlertType.INFORMATION);

    }

    Node getNode(int row, int column) {
        Node result = null;
        ObservableList<Node> childrens = gridPane.getChildren();
        for (Node node : childrens) {
            int rowIndex = 0;
            if(gridPane.getRowIndex(node) == row && gridPane.getColumnIndex(node) == column) {
                result = node;
                break;
            }
        }
        return result;
    }

    @FXML
    void clickBtnSearch (ActionEvent event){
        String title = "Select the settings file...";
        File targetDir;
        if (txtFolderPath.getText().equals("")) {
            targetDir = showFileChooser(title, btnSearch.getScene().getWindow(), DESKTOP_PATH);
        } else {
            targetDir = showFileChooser(title, btnSearch.getScene().getWindow(), txtFolderPath.getText());
        }
        Platform.runLater(() -> {
            txtFolderPath.setText(targetDir.getPath());
        });

        btnDetect.setDisable(false);
    }

    private File showDirectoryChooser (String title, Window ownerWindow, String path){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(new File(path));
        return chooser.showDialog(ownerWindow);
    }

    private File showFileChooser (String title, Window ownerWindow, String path){
        FileChooser chooser = new FileChooser();
        File dir = new File(path);
        chooser.setTitle(title);
        FileChooser.ExtensionFilter tmxFileExtensions =
                new FileChooser.ExtensionFilter(
                        "TMX files", "*.tmx");
        chooser.getExtensionFilters().add(tmxFileExtensions);
        if (dir.isDirectory()) {
            chooser.setInitialDirectory(dir);
        } else {
            chooser.setInitialDirectory(dir.getParentFile());
        }
        return chooser.showOpenDialog(ownerWindow);
    }

    @FXML
    void clickMenuClose (ActionEvent event){
        Platform.exit();
    }

    @FXML
    void clickMenuAbout (ActionEvent event){
        String title = JavaFxApplication.PROGRAM_VER;
        String header = "" +
                "Program Author: " + JavaFxApplication.PROGRAM_AUTHOR +
                "\n\nCopy Right: " + JavaFxApplication.PROGRAM_COPYRIGHT +
                "\n\nLast Modified Date: " + JavaFxApplication.PROGRAM_LAST_MODIFIED;
        String msg = "" +
                "It converts custom fields of general tmx files \n\n" +
                "to fit the TM format used in TRADOS STUDIO 2021.";
        showMsgbox(title, header, msg, Alert.AlertType.INFORMATION);
    }

    private void showMsgbox (String title, String header, String content, Alert.AlertType alertType){
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("Stylesheet.css").toExternalForm());
        dialogPane.getStyleClass().add("myDialog");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            alert.close();
        } else {
            // ... user chose CANCEL or closed the dialog
        }
    }

}
