package com._toMobi._controller;

import animatefx.animation.SlideOutLeft;
import animatefx.animation.SlideOutRight;
import com._toMobi._custom.WatchDog;
import com._toMobi._object.UploadFile;
import com._toMobi.api.Server;
import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import static spark.Spark.port;

/**
 * @author Mandela aka puumInc
 * @version 1.0.0
 */

public class UploadTask extends WatchDog implements Initializable {

    protected static String fileName;
    private Thread myThread;
    private UploadFile myUploadFile;
    private String myFileName;
    private File QR_CODE_FILE_FOR_DOWNLOAD_URL;

    @FXML
    private Label infoLbl;

    @FXML
    private Label statusLbl;

    @FXML
    private JFXProgressBar progressBar;

    @FXML
    void show_qr(ActionEvent event) {
        try {
            String ipV4 = get_first_nonLoopback_address(true, false).getHostAddress();
            String downloadUrl = "http://".concat(ipV4).concat(":" + port()).concat(
                    CONTEXT_PATH.concat("/").concat(URLEncoder.encode(this.myFileName, StandardCharsets.UTF_8.name())));
            System.out.println("downloadUrl = " + downloadUrl);
            this.QR_CODE_FILE_FOR_DOWNLOAD_URL = get_a_qr_image_file_that_has_an_embedded_logo(downloadUrl);
        } catch (SocketException | UnknownHostException | UnsupportedEncodingException e) {
            e.printStackTrace();
            new Thread(stack_trace_printing(e)).start();
            programmer_error(e).show();
        } finally {
            if (this.QR_CODE_FILE_FOR_DOWNLOAD_URL != null) {
                try {
                    show_qr_image_for_upload(this.QR_CODE_FILE_FOR_DOWNLOAD_URL).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    new Thread(stack_trace_printing(e)).start();
                    programmer_error(e).show();
                }
            }
        }
        event.consume();
    }

    @FXML
    void terminate(ActionEvent event) {
        if (myThread == null) {
            event.consume();
            return;
        }
        myThread.interrupt();
        myThread = null;
        if (Controller.UPLOAD_FILE_MAP.remove(this.myUploadFile.getName()) != null) {
            StackPane stackPane = (StackPane) statusLbl.getParent().getParent().getParent();
            new SlideOutRight(stackPane).play();
            VBox vBox = (VBox) stackPane.getParent();
            Platform.runLater(() -> {
                new SlideOutLeft(stackPane);
                VBox.clearConstraints(stackPane);
                vBox.getChildren().remove(stackPane);
            });
            information_message(this.myUploadFile.getName().concat(" has been removed"));
            this.myUploadFile = null;
            if (this.QR_CODE_FILE_FOR_DOWNLOAD_URL != null) {
                if (!this.QR_CODE_FILE_FOR_DOWNLOAD_URL.delete()) {
                    this.QR_CODE_FILE_FOR_DOWNLOAD_URL.deleteOnExit();
                }
            }
            event.consume();
            return;
        }
        warning_message("Incomplete!", this.myUploadFile.getName().concat(" has is still at yhe waiting bay")).show();
        event.consume();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.myFileName = UploadTask.fileName;
        this.myUploadFile = Controller.UPLOAD_FILE_MAP.get(this.myFileName);
        infoLbl.setText(this.myUploadFile.getName());

        progressBar.setProgress(0);
        Task<Object> animatedTask = upload_progress(this.myUploadFile.getSourceSize());
        animatedTask.setOnSucceeded(event -> {
            progressBar.progressProperty().unbind();
            success_notification(this.myFileName.concat(" has been downloaded"));
            terminate(new ActionEvent());
        });
        animatedTask.exceptionProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Exception exception = (Exception) newValue;
                exception.printStackTrace();
                if (!exception.toString().contains("sleep interrupted")) {
                    programmer_error(exception).show();
                }
                new Thread(stack_trace_printing(exception)).start();
            }
        }));
        progressBar.progressProperty().bind(animatedTask.progressProperty());
        myThread = new Thread(animatedTask);
        myThread.start();
    }


    private Task<Object> upload_progress(double jobSizeAsBytes) {
        statusLbl.setText("Size: ".concat(make_bytes_more_presentable(jobSizeAsBytes)));
        final double TOTAL_SIZE_IN_MB = ((jobSizeAsBytes / 1024) / 1024);
        return new Task<Object>() {
            @Override
            protected Object call() {
                while (true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // restore interrupted status
                    }
                    updateProgress(0, 1);
                    String fileRequested = Server.NAMES_OF_FILES_REQUESTED_TO_BE_UPLOADED.stream()
                            .filter(nameOfAFileThatIsReadyForUpload -> nameOfAFileThatIsReadyForUpload
                                    .equals(UploadTask.this.myFileName))
                            .findAny().orElse(null);
                    if (fileRequested != null) {
                        if (Controller.UPLOAD_FILE_MAP.containsKey(fileRequested)) {
                            if (Controller.UPLOAD_FILE_MAP.get(UploadTask.this.myFileName).getName().equals(fileRequested)) {
                                Server.NAMES_OF_FILES_REQUESTED_TO_BE_UPLOADED.remove(UploadTask.this.myFileName);
                                break;
                            }
                        }
                    }
                }
                double megabytes = 0;
                while (megabytes <= TOTAL_SIZE_IN_MB) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // restore interrupted status
                    }
                    UploadTask.this.myUploadFile.setByteSent(Controller.UPLOAD_FILE_MAP.get(UploadTask.this.myFileName).getByteSent());
                    double bytes = UploadTask.this.myUploadFile.getByteSent();
                    double kilobytes = (bytes / 1024);
                    megabytes = (kilobytes / 1024);
                    updateProgress(megabytes, TOTAL_SIZE_IN_MB);
                    final double currentBytes = bytes;
                    Platform.runLater(() -> statusLbl.setText("Uploaded ".concat(make_bytes_more_presentable(currentBytes)).concat(" of ").concat(make_bytes_more_presentable(jobSizeAsBytes))));
                    if (megabytes == TOTAL_SIZE_IN_MB) {
                        Controller.UPLOAD_FILE_MAP.remove(UploadTask.this.myFileName, UploadTask.this.myUploadFile);
                        break;
                    }
                }
                return null;
            }
        };
    }

}
