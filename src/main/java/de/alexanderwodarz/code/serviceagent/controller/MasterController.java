package de.alexanderwodarz.code.serviceagent.controller;

import de.alexanderwodarz.code.web.StatusCode;
import de.alexanderwodarz.code.web.rest.ResponseData;
import de.alexanderwodarz.code.web.rest.annotation.RequestBody;
import de.alexanderwodarz.code.web.rest.annotation.RestController;
import de.alexanderwodarz.code.web.rest.annotation.RestRequest;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@RestController(path = "/master", produces = MediaType.APPLICATION_JSON)
public class MasterController {

    @RestRequest(path = "/execute", method = "POST")
    public static ResponseData postExecute(@RequestBody String b) {
        JSONObject body = new JSONObject(b);
        if (!body.has("cmd") || !(body.get("cmd") instanceof String) || body.getString("cmd").length() == 0)
            return new ResponseData("{}", StatusCode.BAD_REQUEST);
        try {
            Process pro = Runtime.getRuntime().exec(body.getString("cmd"));
            String result = new BufferedReader(new InputStreamReader(pro.getInputStream()))
                    .lines().parallel().collect(Collectors.joining("\n"));
            String error = new BufferedReader(new InputStreamReader(pro.getErrorStream()))
                    .lines().parallel().collect(Collectors.joining("\n"));
            System.out.println(result);
            System.out.println("ERRORS");
            System.out.println(error);
        } catch (IOException e) {
            return new ResponseData(new JSONObject().put("error", e.getMessage()).toString(), StatusCode.CONFLICT);
        }
        return new ResponseData("{}", StatusCode.OK);
    }

}
