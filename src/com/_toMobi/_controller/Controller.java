package com._toMobi._controller;

import animatefx.animation.SlideInLeft;
import com._toMobi.Main;
import com._toMobi._custom.WatchDog;
import com._toMobi._object.UploadFile;
import com._toMobi.api.Server;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class Controller extends WatchDog implements Initializable {

    public static final Map<String, UploadFile> UPLOAD_FILE_MAP = new TreeMap<>();
    private String recentDirectory = null;

    @FXML
    private Label ipAddressLbl;

    @FXML
    private VBox waitingBox;

    @FXML
    void choose_files(ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose your desired file");
        if (recentDirectory != null) {
            File file = new File(recentDirectory);
            if (file.exists()) {
                fileChooser.setInitialDirectory(file);
            }
        }
        //fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP files only", "*.zip"));
        final List<File> selectedFiles = fileChooser.showOpenMultipleDialog(Main.stage);
        if (selectedFiles == null) {
            error_message("Invalid directory!", "Sadly no directory was selected to save the selected media").show();
            event.consume();
            return;
        }
        for (File selectedFile : selectedFiles) {
            if (the_file_has_zero_bytes(selectedFile)) {
                error_message("Hmmh, a bad file found!", "A file has zero bytes and it will be ignored, meanwhile stay tuned for more info").show();
                error_message("DETAILS", selectedFile.getName().concat(" has zero bytes.")).show();
            } else {
                try {
                    recentDirectory = selectedFile.getParentFile().getAbsolutePath();
                    final String absolutePath = selectedFile.getAbsolutePath();
                    push_to_waiting_bay(absolutePath);
                } catch (Exception e) {
                    e.printStackTrace();
                    programmer_error(e).show();
                }
            }
        }
        event.consume();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            new Server();
        } catch (Exception e) {
            e.printStackTrace();
            new Thread(stack_trace_printing(e)).start();
            programmer_error(e).show();
        } finally {
            Task<Object> objectTask = addressTask();
            objectTask.exceptionProperty().addListener(((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    Exception e = (Exception) newValue;
                    e.printStackTrace();
                    programmer_error(e).show();
                    new Thread(stack_trace_printing(e)).start();
                }
            }));
            new Thread(objectTask).start();
        }
    }

    @Contract(value = " -> new", pure = true)
    final @NotNull Task<Object> addressTask() {
        return new Task<Object>() {
            @Override
            protected Object call() {
                while (true) {
                    try {
                        final String ipV4 = get_first_nonLoopback_address(true, false).getHostAddress();
                        if (!ipAddressLbl.getText().trim().contains(ipV4)) {
                            Platform.runLater(() -> ipAddressLbl.setText("http://".concat(ipV4).concat(":" + Spark.port()).concat("/toMobi/api/download")));
                        }
                        Thread.sleep(500);
                    } catch (UnknownHostException e) {
                        Platform.runLater(() -> ipAddressLbl.setText("Host Address was not found!"));
                    } catch (InterruptedException | SocketException e) {
                        e.printStackTrace();
                        new Thread(stack_trace_printing(e)).start();
                        Platform.runLater(() -> programmer_error(e).show());
                        break;
                    }
                }
                return null;
            }
        };
    }

    private void push_to_waiting_bay(String filePath) {
        final UploadFile uploadFile = new UploadFile();
        uploadFile.setName(new File(filePath).getName());
        uploadFile.setFilePath(filePath);
        uploadFile.setSourceSize(get_size_of_the_selected_file(new File(filePath)));
        if (Controller.UPLOAD_FILE_MAP.containsKey(uploadFile.getName())) {
            warning_message("Can't continue!", "The file is already at the waiting bay").show();
            return;
        }
        try {
            Controller.UPLOAD_FILE_MAP.put(uploadFile.getName(), uploadFile);
            UploadTask.fileName = uploadFile.getName();
            final Node node = FXMLLoader.load(getClass().getResource("/com/_toMobi/_fxml/upload.fxml"));
            Platform.runLater(() -> {
                waitingBox.getChildren().add(node);
                new SlideInLeft(node).play();
            });
        } catch (IOException e) {
            e.printStackTrace();
            programmer_error(e).show();
            new Thread(stack_trace_printing(e)).start();
        }
    }

    private double get_size_of_the_selected_file(File file) {
        double bytes = 0;
        bytes += get_size_of_the_provided_file_or_folder(file);
        return bytes;
    }

}
