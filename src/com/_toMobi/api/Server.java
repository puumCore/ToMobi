package com._toMobi.api;

import com._toMobi._controller.Controller;
import com._toMobi._custom.WatchDog;
import com._toMobi._object.Job;
import com._toMobi.api._response_model.StandardResponse;
import com._toMobi.api._response_model.StatusResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.UnknownHostException;

import static spark.Spark.get;
import static spark.Spark.port;

/**
 * @author Mandela aka puumInc
 * @version 1.0.0
 */

public class Server extends WatchDog {

    /**
     * Accessible url is <a href=http://localhost:3781/toMobi/api><strong>Core URL</strong></a>
     */

    public static String fileRequestedByMobile;

    public Server() {
        port(3781);
        final String CONTEXT_PATH = "/toMobi/api";
        get(CONTEXT_PATH.concat("/download"), ((request, response) -> {
            response.type("application/json");
            try {
                final JsonArray jsonArray = get_list_of_pending_uploads();
                JsonObject jsonObject = new JsonObject();
                if (jsonArray.size() == 0) {
                    jsonObject.add("result", new Gson().toJsonTree("No file has been selected!", String.class));
                } else {
                    jsonObject.add("result", jsonArray);
                    jsonObject.add("info", new Gson().toJsonTree("Use the urls to download the files", String.class));

                }
                response.status(HttpURLConnection.HTTP_OK);
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(jsonObject, JsonObject.class)));
            } catch (Exception exception) {
                exception.printStackTrace();
                new Thread(stack_trace_printing(exception)).start();
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, exception.getLocalizedMessage()));
            }
        }));
        get(CONTEXT_PATH.concat("/download/:fileName"), ((request, response) -> {
            response.type("*/*");
            final String fileName = request.params(":fileName");
            response.status(HttpURLConnection.HTTP_OK);
            try {
                if (Controller.jobList.containsKey(fileName)) {
                    final Job modifiedJob = Controller.jobList.get(fileName);
                    fileRequestedByMobile = fileName;
                    DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(response.raw().getOutputStream()));
                    response.raw().setContentLengthLong((long) modifiedJob.getSourceSize());
                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(modifiedJob.getFilePath()))));
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = dataInputStream.read(buffer)) > 0) {
                        dataOutputStream.write(buffer, 0, count);
                        modifiedJob.setByteSent(modifiedJob.getByteSent() + buffer.length);
                        Controller.jobList.replace(modifiedJob.getJobName(), modifiedJob);
                    }
                    dataInputStream.close();
                    dataOutputStream.close();
                    return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS));
                }
                return new Gson().toJson(new StandardResponse(StatusResponse.WARNING, "The name provided is invalid or no longer exists"));
            } catch (Exception exception) {
                exception.printStackTrace();
                new Thread(stack_trace_printing(exception)).start();
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, exception.getLocalizedMessage()));
            }
        }));
    }

    private @NotNull JsonArray get_list_of_pending_uploads() throws SocketException, UnknownHostException {
        final JsonArray jsonArray = new JsonArray();
        for (String string : Controller.jobList.keySet()) {
            String ipV4 = get_first_nonLoopback_address(true, false).getHostAddress();
            String downloadUrl = "http://".concat(ipV4).concat(":" + port()).concat("/toMobi/api/download/".concat(string));
            jsonArray.add(new Gson().toJsonTree(downloadUrl, String.class));
        }
        return jsonArray;
    }

}
