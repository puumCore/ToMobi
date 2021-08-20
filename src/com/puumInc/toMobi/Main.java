package com.puumInc.toMobi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Main extends Application {

    public static File RESOURCE_PATH = new File(System.getenv("JAVAFX_DEV_APP_HOME").concat("\\_to_mobi"));
    private double xOffset, yOffset;
    public static Stage stage;


    @Override
    public void start(@NotNull Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/_fxml/sample.fxml"));
        Scene scene = new Scene(root);
        scene.setOnMousePressed(event2 -> {
            xOffset = event2.getSceneX();
            yOffset = event2.getSceneY();
        });
        scene.setOnMouseDragged(event1 -> {
            primaryStage.setX(event1.getScreenX() - xOffset);
            primaryStage.setY(event1.getScreenY() - yOffset);
        });
        scene.setFill(Color.web("#FFFFFF", 0));
        primaryStage.setTitle("To Mobi");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.getIcons().add(new Image(getClass().getResource("/_images/_other/toMobi.png").toExternalForm()));
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();
        Main.stage = primaryStage;
    }


    public static void main(String[] args) {
        launch(args);
    }
}
