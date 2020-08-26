package com._toMobi._custom;

import com._toMobi.Main;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Mandela aka puumInc
 * @version 1.1.2
 */

public class WatchDog {

    private final String PATH_TO_ERROR_FOLDER = Main.RESOURCE_PATH.getAbsolutePath() + "\\_watchDog\\_error\\";

    protected final boolean the_file_has_zero_bytes(@NotNull File file) {
        return get_size_of_the_provided_file_or_folder(file) == 0;
    }

    protected final double get_size_of_the_provided_file_or_folder(@NotNull File file) {
        double bytes = 0;
        if (file.isFile()) {
            bytes = file.length();
        } else {
            if (file.isDirectory()) {
                final File[] fileList = file.listFiles();
                if (fileList != null) {
                    for (File file1 : fileList) {
                        bytes += get_size_of_the_provided_file_or_folder(file1);
                    }
                }
            }
        }
        return bytes;
    }

    protected final @NotNull String make_bytes_more_presentable(double bytes) {
        final double kilobytes = (bytes / 1024);
        final double megabytes = (kilobytes / 1024);
        final double gigabytes = (megabytes / 1024);
        final double terabytes = (gigabytes / 1024);
        final double petabytes = (terabytes / 1024);
        final double exabytes = (petabytes / 1024);
        final double zettabytes = (exabytes / 1024);
        final double yottabytes = (zettabytes / 1024);
        String result;
        if (((int) yottabytes) > 0) {
            result = String.format("%,.2f", yottabytes).concat(" YB");
            return result;
        }
        if (((int) zettabytes) > 0) {
            result = String.format("%,.2f", zettabytes).concat(" ZB");
            return result;
        }
        if (((int) exabytes) > 0) {
            result = String.format("%,.2f", exabytes).concat(" EB");
            return result;
        }
        if (((int) petabytes) > 0) {
            result = String.format("%,.2f", petabytes).concat(" PB");
            return result;
        }
        if (((int) terabytes) > 0) {
            result = String.format("%,.2f", terabytes).concat(" TB");
            return result;
        }
        if (((int) gigabytes) > 0) {
            result = String.format("%,.2f", gigabytes).concat(" GB");
            return result;
        }
        if (((int) megabytes) > 0) {
            result = String.format("%,.2f", megabytes).concat(" MB");
            return result;
        }
        if (((int) kilobytes) > 0) {
            result = String.format("%,.2f", kilobytes).concat(" KB");
            return result;
        }
        result = String.format("%,.0f", bytes).concat(" Bytes");
        return result;
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    protected final Runnable stack_trace_printing(Exception exception) {
        return () -> write_stack_trace(exception);
    }

    protected final void write_stack_trace(Exception exception) {
        BufferedWriter bw = null;
        try {
            File log = new File(PATH_TO_ERROR_FOLDER.concat(gate_date_for_file_name().concat(" stackTrace_log.txt")));
            if (!log.exists()) {
                FileWriter fw = new FileWriter(log);
                fw.write("\nThis is a newly created file [ " + time_stamp() + " ].");
            }
            if (log.canWrite() & log.canRead()) {
                FileWriter fw = new FileWriter(log, true);
                bw = new BufferedWriter(fw);
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                exception.printStackTrace(printWriter);
                String exceptionText = stringWriter.toString();
                bw.write("\n ##################################################################################################"
                        + " \n " + time_stamp()
                        + "\n " + exceptionText
                        + "\n\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            programmer_error(ex).show();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                programmer_error(ex).show();
            }
        }
    }

    @NotNull
    protected final Alert programmer_error(@NotNull Object object) {
        Exception exception = (Exception) object;
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(Main.stage);
        alert.setTitle("WATCH DOG");
        alert.setHeaderText("ERROR TYPE : " + exception.getClass());
        alert.setContentText("This dialog is a detailed explanation of the error that has occurred");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        String exceptionText = stringWriter.toString();
        Label label = new Label("The exception stacktrace was: ");
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        VBox vBox = new VBox();
        vBox.getChildren().add(label);
        vBox.getChildren().add(textArea);
        alert.getDialogPane().setExpandableContent(vBox);
        return alert;
    }

    private @NotNull String gate_date_for_file_name() {
        return new SimpleDateFormat("dd-MMM-yyyy").format(Calendar.getInstance().getTime()).replaceAll("-", " ");
    }

    @NotNull
    protected String time_stamp() {
        return get_date() + " at " + new SimpleDateFormat("HH:mm:ss:SSS").format(Calendar.getInstance().getTime());
    }

    protected String get_date() {
        return new SimpleDateFormat("dd-MMM-yyyy").format(Calendar.getInstance().getTime());
    }

    protected final void information_message(String message) {
        try {
            SystemTray systemTray = SystemTray.getSystemTray();
            java.awt.image.BufferedImage bufferedImage = ImageIO.read(getClass().getResource("/com/_toMobi/_images/_other/toMobi.png"));
            TrayIcon trayIcon = new TrayIcon(bufferedImage);
            trayIcon.setImageAutoSize(true);
            systemTray.add(trayIcon);
            trayIcon.displayMessage("Information", message, TrayIcon.MessageType.NONE);
        } catch (IOException | AWTException exception) {
            exception.printStackTrace();
            programmer_error(exception).show();
        }
    }

    protected final void success_notification(String about) {
        try {
            SystemTray systemTray = SystemTray.getSystemTray();
            java.awt.image.BufferedImage bufferedImage = ImageIO.read(getClass().getResource("/com/_toMobi/_images/_icons/icons8_Ok_48px.png"));
            TrayIcon trayIcon = new TrayIcon(bufferedImage);
            trayIcon.setImageAutoSize(true);
            systemTray.add(trayIcon);
            trayIcon.displayMessage("Success", about, TrayIcon.MessageType.NONE);
        } catch (IOException | AWTException exception) {
            exception.printStackTrace();
            programmer_error(exception).show();
        }
    }

    protected final Notifications error_message(String title, String text) {
        Image image = new Image("/com/_toMobi/_images/_icons/icons8_Close_Window_48px.png");
        return Notifications.create()
                .title(title)
                .text(text)
                .graphic(new ImageView(image))
                .hideAfter(Duration.seconds(8))
                .position(Pos.TOP_RIGHT);
    }

    protected final Notifications warning_message(String title, String text) {
        Image image = new Image("/com/_toMobi/_images/_icons/icons8_Error_48px.png");
        return Notifications.create()
                .title(title)
                .text(text)
                .graphic(new ImageView(image))
                .hideAfter(Duration.seconds(8))
                .position(Pos.TOP_RIGHT);
    }

}
