package com.puumInc.toMobi.api;

import com.google.gson.Gson;
import com.puumInc.toMobi._controller.Controller;
import com.puumInc.toMobi._custom.WatchDog;
import com.puumInc.toMobi._model.UFile;
import com.puumInc.toMobi._model.UploadFile;
import com.puumInc.toMobi.api._response_model.StandardResponse;
import com.puumInc.toMobi.api._response_model.StatusResponse;
import j2html.tags.ContainerTag;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static j2html.TagCreator.head;
import static j2html.TagCreator.*;
import static spark.Spark.*;

/**
 * @author Mandela aka puumInc
 * @version 1.0.0
 */

public class Server extends WatchDog {

    /**
     * Accessible url is <a href=http://localhost:3781/toMobi/api/download><strong>Core URL</strong></a> <br>
     * <br>
     * <strong>When a connection is force closed by a remote host, the following explains why:</strong><br>
     * From HTTP 1.1, keep-alive is enabled by default. You would need to close the connection if you don't want it to be reused explicitly when dealing with HTTP 1.1.<br>
     * <br>
     * For 1.0, a header is what you set for this "Connection: Keep-alive" This only intimates the server that you want to reuse the connection. When under stress or for other reasons, server might choose to act differently as explained below.<br>
     * <br>
     * For most purposes most answers here are correct, where in you add keep alive header, and it works well. The following explanation is for the scenarios where you did that, but it still does not work.<br>
     * <br>
     * The server side issues<br>
     * Normally an answer would focus at a setup when server would behave normally, but this is not completely true. Servers (like Rudra) are built to behave differently in different situations. Keep-alive comes with a number of requests the server would serve you for before dropping your connection, this is there to allow service to others as well so in case of high load, some servers might resort to reducing the no of keep-alive requests served for each new connection.<br>
     * <br>
     * There is also a timeout set from the last request received, which would eventually lead to disconnection if no further requests are made in that window of time. Few modern servers might alter this based on the capacity they have at the moment or drop it to 0 in panic conditions making keep-alive meaningless. So if the server you are trying to connect with is going through any of such (race,panic) conditions it might choose to discard your request.<br>
     * <br>
     * The client side issues<br>
     * For the documentation purpose. From hc.apache.org :<br>
     * <br>
     * HttpClient always does its best to reuse connections. Connection persistence is enabled by default and requires no configuration. Under some situations this can lead to leaked connections and therefore lost resources. The easiest way to disable connection persistence is to provide or extend a connection manager that force-closes connections upon release in the releaseConnection method.<br>
     * <br>
     * HttpClient offers these (read:trivial) things Out Of The Box. But still there are other things that are offered by Apache that you can add to improve its performance.<br>
     * <br>
     * ConnectionManager(s) for example can be customized for HttpClient.<br>
     * <br>
     * So the thing that can block keep-alive/connection persistence is the connection manager that you might be using (this is not true in your case, but it might be true in several other cases). This might be a totally unknown/abstract fact for you if you are getting the Client object for making the calls from some API. An example of how this can be customized has been listed below (from Apache connection management documentation)<br>
     * <br>
     * PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();<br>
     * // Increase max total connection to 200<br>
     * cm.setMaxTotal(200);<br>
     * // Increase default max connection per route to 20<br>
     * cm.setDefaultMaxPerRoute(20);<br>
     * // Increase max connections for localhost:80 to 50<br>
     * HttpHost localhost = new HttpHost("localhost", 80);<br>
     * cm.setMaxPerRoute(new HttpRoute(localhost), 50);<br>
     * <br>
     * CloseableHttpClient httpClient = HttpClients.custom()<br>
     * .setConnectionManager(cm)<br>
     * .build();<br>
     * (please refer Apache documentation on connection management for more details)<br>
     * <br>
     * If you face this problem, try out without a CM or create your own HttpClient object. It's not necessary to use a CM for multiple connections as well. The inbuilt CM is fair enough. If you see a performance loss you can write your own connection manager.<br>
     * <br>
     * In your case however, your server is not very supportive it might not honour Keep-alive at all, even if you have those headers and what not. You would need to check for timeout header in response, on sending keep-alive in a new request to the server to establish that server is complaint.<br>
     */

    public static final List<String> NAMES_OF_FILES_REQUESTED_TO_BE_UPLOADED = new ArrayList<>();

    public Server() {
        port(3781);
        staticFileLocation("public");
        web();
        upload();
    }

    private void web() {
        get(WEB_CONTEXT_PATH, ((request, response) -> {
            int myPort = port();
            String myIp = get_first_nonLoopback_address(true, false).getHostAddress();
            AtomicInteger index = new AtomicInteger(1);
            List<UFile> listOfPendingUFiles = get_list_of_pending_uFiles(get_first_nonLoopback_address(true, false).getHostAddress());
            response.type("text/html");
            return html()
                    .with(
                            head()
                                    .with(
                                            meta()
                                                    .withCharset("utf-8")
                                                    .attr("http-equiv='X-UA-Compatible'")
                                                    .attr("content='IE=edge'")
                                                    .withName("viewport")
                                                    .withContent("width=device-width, initial-scale=1"),
                                            link()
                                                    .withRel("shortcut icon")
                                                    .withType("image/x-icon")
                                                    .withHref(String.format("http://%s:%d/img/toMobi.png", myIp, myPort)),
                                            link()
                                                    .withRel("stylesheet")
                                                    .withType("text/css")
                                                    .attr("media='screen'")
                                                    .withHref(String.format("http://%s:%d/web/main.css", myIp, myPort)),
                                            script()
                                                    .withSrc(String.format("http://%s:%d/web/main.js", myIp, myPort))
                                    ),
                            body()
                                    .with(
                                            div()
                                                    .with(
                                                            img()
                                                                    .withId("myLogo")
                                                                    .withSrc(String.format("http://%s:%d/img/toMobi.png", myIp, myPort)),
                                                            h3("The following files are available, you can get either of them")
                                                    ),
                                            div()
                                                    .with(
                                                            listOfPendingUFiles.stream().map(uFile -> {
                                                                String divId = String.format("divId%d", index.get());
                                                                return div()
                                                                        .withId(divId)
                                                                        .withClass("fileDiv")
                                                                        .with(
                                                                                label(String.format("%d. %s", index.getAndIncrement(), uFile.getName()))
                                                                                        .withClass("downloadTitles"),
                                                                                button()
                                                                                        .withText("Get File")
                                                                                        .attr(String.format("onclick=\"download('%s', '%s', '%s')\"", uFile.getUrl(), uFile.getName(), divId))
                                                                        );
                                                            }).toArray(ContainerTag[]::new)
                                                    )
                                    )
                    ).render();
        }));
    }

    private void upload() {
        get(CONTEXT_PATH.concat("/:fileName"), ((request, response) -> {
            String fileName = request.params(":fileName");
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
            if (Controller.UPLOAD_FILE_MAP.size() > 0) {
                if (!Controller.UPLOAD_FILE_MAP.containsKey(fileName)) {
                    fileName = fileName.replace("_", " ");
                }
            }
            response.type("application/json");
            response.status(HttpURLConnection.HTTP_OK);
            try {
                if (Controller.UPLOAD_FILE_MAP.size() > 0) {
                    if (Controller.UPLOAD_FILE_MAP.containsKey(fileName)) {
                        response.type("file/*");
                        final UploadFile modifiedUploadFile = Controller.UPLOAD_FILE_MAP.get(fileName);
                        File file = new File(modifiedUploadFile.getFilePath());
                        if (file.exists() && file.canRead()) {
                            String duplicateFileName = Server.NAMES_OF_FILES_REQUESTED_TO_BE_UPLOADED.stream().filter(fileName::equals).findAny().orElse(null);
                            if (duplicateFileName == null) {
                                Server.NAMES_OF_FILES_REQUESTED_TO_BE_UPLOADED.add(fileName);
                            }
                            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(response.raw().getOutputStream()));
                            response.raw().setContentLengthLong((long) modifiedUploadFile.getSourceSize());
                            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(modifiedUploadFile.getFilePath())));
                            byte[] buffer = new byte[8192];
                            int count;
                            while ((count = dataInputStream.read(buffer)) > 0) {
                                dataOutputStream.write(buffer, 0, count);
                                modifiedUploadFile.setByteSent(modifiedUploadFile.getByteSent() + buffer.length);
                                Controller.UPLOAD_FILE_MAP.replace(modifiedUploadFile.getName(), modifiedUploadFile);
                            }
                            dataInputStream.close();
                            dataOutputStream.close();
                            return HttpURLConnection.HTTP_OK;
                        } else {
                            return halt(HttpURLConnection.HTTP_GONE, "Causes could be that the file cant be found or read");
                        }
                    } else {
                        return new Gson().toJson(new StandardResponse(StatusResponse.WARNING, "A > Reasons:" +
                                "\n  1.No such file exits." +
                                "\n  2.The file has been already uploaded." +
                                "\n  3.The file has a bad name, remove special characters from the file and retry."));
                    }
                } else {
                    return new Gson().toJson(new StandardResponse(StatusResponse.WARNING, "B > Reasons:" +
                            "\n  1.No such file exits." +
                            "\n  2.The file has been already uploaded." +
                            "\n  3.The file has a bad name, remove special characters from the file and retry."));
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                new Thread(stack_trace_printing(exception)).start();
                response.type("application/json");
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, exception.getLocalizedMessage()));
            }
        }));
    }

}
