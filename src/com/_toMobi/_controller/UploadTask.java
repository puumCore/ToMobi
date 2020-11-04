package com._toMobi._controller;

import animatefx.animation.SlideOutLeft;
import animatefx.animation.SlideOutRight;
import com._toMobi._custom.WatchDog;
import com._toMobi._object.Job;
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

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author Mandela aka puumInc
 * @version 1.0.0
 */

@SuppressWarnings("unused")
public class UploadTask extends WatchDog implements Initializable {

    private Thread myThread;
    protected static String fileName;
    private Job myJob;
    private String myFileName;

    @FXML
    private Label infoLbl;

    @FXML
    private Label statusLbl;

    @FXML
    private JFXProgressBar progressBar;

    @FXML
    void terminate(ActionEvent event) {
        if (myThread == null) {
            return;
        }
        myThread.interrupt();
        myThread = null;
        if (Controller.jobList.remove(this.myJob.getJobName()) != null) {
            StackPane stackPane = (StackPane) statusLbl.getParent().getParent().getParent();
            new SlideOutRight(stackPane).play();
            VBox vBox = (VBox) stackPane.getParent();
            Platform.runLater(() -> {
                new SlideOutLeft(stackPane);
                VBox.clearConstraints(stackPane);
                vBox.getChildren().remove(stackPane);
            });
            information_message(this.myJob.getJobName().concat(" has been removed"));
            this.myJob = null;
            return;
        }
        warning_message("Incomplete!", this.myJob.getJobName().concat(" has is still at yhe waiting bay")).show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.myFileName = fileName;
        this.myJob = Controller.jobList.get(this.myFileName);
        infoLbl.setText(this.myJob.getJobName());
        progressBar.setProgress(0);
        Task<Object> animatedTask = copy_progress(this.myJob.getSourceSize(), this.myJob.getJobName());
        animatedTask.setOnSucceeded(event -> {
            System.out.println(fileName + " just ended!");
            progressBar.progressProperty().unbind();
            success_notification(this.myFileName.concat(" has been downloaded"));
            terminate(new ActionEvent());
        });
        animatedTask.exceptionProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Exception e = (Exception) newValue;
                e.printStackTrace();
                if (!e.getLocalizedMessage().contains("sleep interrupted")) {
                    programmer_error(e).show();
                }
                new Thread(stack_trace_printing(e)).start();
            }
        }));
        progressBar.progressProperty().bind(animatedTask.progressProperty());
        myThread = new Thread(animatedTask);
        myThread.start();
    }

    protected Task<Object> copy_progress(double jobSizeAsBytes, String fileName) {
        statusLbl.setText("Size: ".concat(make_bytes_more_presentable(jobSizeAsBytes)));
        final double TOTAL_SIZE_IN_MB = ((jobSizeAsBytes / 1024) / 1024);
        return new Task<Object>() {
            @Override
            protected Object call() throws InterruptedException {
                while (true) {
                    Thread.sleep(500);
                    updateProgress(0, 1);
                    if (Server.fileRequestedByMobile != null) {
                        if (Controller.jobList.containsKey(Server.fileRequestedByMobile)) {
                            if (Controller.jobList.get(myFileName).getJobName().equals(Server.fileRequestedByMobile)) {
                                Server.fileRequestedByMobile = null;
                                break;
                            }
                        }
                    }
                }
                double megabytes = 0;
                while (megabytes <= TOTAL_SIZE_IN_MB) {
                    Thread.sleep(500);
                    myJob.setByteSent(Controller.jobList.get(UploadTask.this.myFileName).getByteSent());
                    double bytes = myJob.getByteSent();
                    double kilobytes = (bytes / 1024);
                    megabytes = (kilobytes / 1024);
                    updateProgress(megabytes, TOTAL_SIZE_IN_MB);
                    final double currentBytes = bytes;
                    Platform.runLater(() -> statusLbl.setText("Uploaded ".concat(make_bytes_more_presentable(currentBytes)).concat(" of ").concat(make_bytes_more_presentable(jobSizeAsBytes))));
                    if (megabytes == TOTAL_SIZE_IN_MB) {
                        Controller.jobList.remove(UploadTask.this.myFileName, UploadTask.this.myJob);
                        break;
                    }
                }
                return null;
            }
        };
    }

}
