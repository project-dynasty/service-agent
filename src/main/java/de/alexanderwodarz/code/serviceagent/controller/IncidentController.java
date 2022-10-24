package de.alexanderwodarz.code.serviceagent.controller;

import de.alexanderwodarz.code.serviceagent.model.incident.Incident;
import de.alexanderwodarz.code.serviceagent.model.incident.IncidentCreateResponse;
import de.alexanderwodarz.code.web.StatusCode;
import de.alexanderwodarz.code.web.rest.RequestData;
import de.alexanderwodarz.code.web.rest.ResponseData;
import de.alexanderwodarz.code.web.rest.annotation.RequestBody;
import de.alexanderwodarz.code.web.rest.annotation.RestController;
import de.alexanderwodarz.code.web.rest.annotation.RestRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;

@RestController(path = "/incident", produces = MediaType.APPLICATION_JSON)
public class IncidentController {

    @RestRequest(method = "GET")
    public static ResponseData getIncidents(RequestData data) {
        JSONArray array = new JSONArray();
        for (Incident incident : Incident.getIncidents())
            array.put(incident.getIncident());
        return new ResponseData(array.toString(), StatusCode.OK);
    }

    @RestRequest(path = "/incident", method = "PUT")
    public static ResponseData putIncident(@RequestBody String b) {
        JSONObject body = new JSONObject(b);
        if (!body.has("status") || !(body.get("status") instanceof String) || body.getString("status").length() == 0 ||
                !body.has("type") || !(body.get("type") instanceof String) || body.getString("type").length() == 0 ||
                !body.has("description") || !(body.get("description") instanceof String) || body.getString("description").length() == 0 ||
                !body.has("effected") || !(body.get("effected") instanceof JSONArray))
            return new ResponseData("{}", StatusCode.BAD_REQUEST);
        IncidentCreateResponse response = Incident.create(body);
        if (!response.isSuccess())
            return new ResponseData(new JSONObject().put("error", response.getError()).toString(), response.getStatus());
        return new ResponseData(new JSONObject().put("id", response.getId()).toString(), StatusCode.OK);
    }

}
