package de.alexanderwodarz.code.serviceagent.client;

import com.sun.jersey.api.client.ClientHandlerException;
import de.alexanderwodarz.code.rest.ClientThread;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class Request {

    private JSONObject object;
    private JSONArray array;
    private int status;

    public Request(String url, ClientThread.RequestMethod method, JSONObject body, HashMap<String, String> headers) {
        ClientThread thread = new ClientThread(url, method);
        try {
            if (body != null)
                thread.setBody(body);
            if (headers != null)
                thread.setHeaders(headers);
            thread.run();
            while (thread.isAlive()) {
            }
        } catch (Exception e) {
            status = 499;
            if (e instanceof ClientHandlerException) {
                if (e.getMessage().contains("Connection refused")) {
                    object = new JSONObject().put("error", "connection refused");
                    return;
                }
            }
            object = new JSONObject().put("error", "unknown error");
            return;
        }
        try {
            object = new JSONObject(thread.getResponse());
        } catch (Exception e) {
            try {
                array = new JSONArray(thread.getResponse());
            } catch (Exception ignored) {
            }
        }
        status = thread.getStatus();
    }

    public int getStatus() {
        return status;
    }

    public JSONArray getArray() {
        return array;
    }

    public JSONObject getObject() {
        return object;
    }
}
