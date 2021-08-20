package com.puumInc.toMobi._custom;

import com.puumInc.toMobi.Main;
import com.puumInc.toMobi._controller.Controller;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.puumInc.toMobi._model.UFile;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import spark.Spark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author edgar
 */
public abstract class Assistant {

    protected final String WEB_CONTEXT_PATH = "/toMobi/web";
    protected final String CONTEXT_PATH = "/toMobi/api/download";


    protected JsonArray get_list_of_pending_uploads() throws SocketException, UnknownHostException {
        final JsonArray jsonArray = new JsonArray();
        for (String string : Controller.UPLOAD_FILE_MAP.keySet()) {
            String ipV4 = get_first_nonLoopback_address(true, false).getHostAddress();
            String downloadUrl = "http://".concat(ipV4).concat(":" + Spark.port()).concat(CONTEXT_PATH.concat("/").concat(string));
            jsonArray.add(new Gson().toJsonTree(downloadUrl, String.class));
        }
        return jsonArray;
    }

    protected final List<UFile> get_list_of_pending_uFiles(String myIp) {
        List<UFile> uFileList = new ArrayList<>();
        for (String string : Controller.UPLOAD_FILE_MAP.keySet()) {
            UFile uFile = new UFile();
            uFile.setName(string);
            uFile.setUrl(String.format("http://%s:%d%s/%s", myIp, Spark.port(), CONTEXT_PATH, string));
            uFileList.add(uFile);
        }
        return uFileList;
    }

    protected Alert show_qr_image_for_upload(File imageFile) throws FileNotFoundException {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(Main.stage);
        alert.setTitle(Main.stage.getTitle());
        alert.setHeaderText("Scan the QR code and open it in your browser");
        alert.setContentText("Click \"Show details\"");
        ImageView imageView = new ImageView();
        imageView.setFitHeight(350);
        imageView.setFitWidth(350);
        imageView.setImage(new Image(new FileInputStream(imageFile)));
        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(imageView);
        stackPane.setMaxSize(imageView.getFitWidth(), imageView.getFitHeight());
        stackPane.setStyle("-fx-background-color: #FFFFFF;");
        alert.getDialogPane().setExpandableContent(stackPane);
        return alert;
    }

    protected File get_a_qr_image_file_that_has_an_embedded_logo(final String jobName) {
        try {
            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix bitMatrix = qrCodeWriter.encode(jobName, BarcodeFormat.QR_CODE, 350, 350);
            //write to png file
            final File file = new File(
                    format_path_name_to_current_os(
                            Main.RESOURCE_PATH.getAbsolutePath()
                                    .concat("\\_address\\")
                                    .concat(RandomStringUtils.randomAlphabetic(10))
                                    .concat(".png")
                    )
            );
            final Path path = FileSystems.getDefault().getPath(file.getAbsolutePath());
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            return file;
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            new Thread(new WatchDog().stack_trace_printing(e)).start();
            new WatchDog().programmer_error(e).show();
        }
        return null;
    }

    protected final InetAddress get_first_nonLoopback_address(boolean preferIpv4, boolean preferIPv6) throws SocketException, UnknownHostException {
        InetAddress result = InetAddress.getLocalHost();
        Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            for (Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses(); inetAddressEnumeration.hasMoreElements(); ) {
                InetAddress inetAddress = inetAddressEnumeration.nextElement();
                if (!inetAddress.isLoopbackAddress()) {
                    if (inetAddress instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        result = inetAddress;
                        break;
                    }
                    if (inetAddress instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        result = inetAddress;
                        break;
                    }
                }
            }
            if (result != null) break;
        }
        return result;
    }

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

    protected String format_path_name_to_current_os(String myPath) {
        String myOperatingSystemSlash = get_slash_for_my_os();
        if (myOperatingSystemSlash == null) {
            return myPath;
        }
        if (!myOperatingSystemSlash.equals("\\")) {
            myPath = myPath.replace("\\", myOperatingSystemSlash);
        }
        return myPath;
    }

    String get_slash_for_my_os() {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win")) {
            //windows
            return "\\";
        } else if (OS.contains("mac")) {
            //mac
            return "/";
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            //unix
            return "/";
        } else if (OS.contains("sunos")) {
            //solaris
            return "/";
        } else {
            return null;
        }
    }
}
