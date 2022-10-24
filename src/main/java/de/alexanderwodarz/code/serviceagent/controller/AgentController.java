package de.alexanderwodarz.code.serviceagent.controller;

import de.alexanderwodarz.code.serviceagent.model.agent.Agent;
import de.alexanderwodarz.code.serviceagent.model.agent.AgentRegisterResponse;
import de.alexanderwodarz.code.serviceagent.model.agent.AgentSolvedResponse;
import de.alexanderwodarz.code.serviceagent.model.incident.Incident;
import de.alexanderwodarz.code.serviceagent.model.incident.IncidentCreateResponse;
import de.alexanderwodarz.code.web.StatusCode;
import de.alexanderwodarz.code.web.rest.ResponseData;
import de.alexanderwodarz.code.web.rest.annotation.PathVariable;
import de.alexanderwodarz.code.web.rest.annotation.RequestBody;
import de.alexanderwodarz.code.web.rest.annotation.RestController;
import de.alexanderwodarz.code.web.rest.annotation.RestRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;

@RestController(path = "/agent", produces = MediaType.APPLICATION_JSON)
public class AgentController {

    @RestRequest(path = "/status", method = "GET")
    public static ResponseData getAgentStatus() {
        return new ResponseData("{}", StatusCode.OK);
    }

    @RestRequest(path = "/register", method = "POST")
    public static ResponseData postAgentRegister(@RequestBody String b) {
        JSONObject body = new JSONObject(b);
        if (!body.has("id") || !(body.get("id") instanceof String) || body.getString("id").length() == 0 ||
                !body.has("name") || !(body.get("name") instanceof String) || body.getString("name").length() == 0 ||
                !body.has("address") || !(body.get("address") instanceof String) || body.getString("address").length() == 0)
            return new ResponseData("{}", StatusCode.BAD_REQUEST);
        AgentRegisterResponse response = Agent.register(body);
        JSONObject result = new JSONObject();
        if (response.isSuccess())
            result.put("challenge", response.getChallenge());
        else
            result.put("error", response.getError());
        return new ResponseData(result.toString(), response.isSuccess() ? StatusCode.OK : StatusCode.CONFLICT);
    }

    @RestRequest(path = "/auth/token", method = "POST")
    public static ResponseData postAuthToken(@RequestBody String b) {
        JSONObject body = new JSONObject(b);
        if (!body.has("token") || !(body.get("token") instanceof String) || body.getString("token").length() == 0 ||
                !body.has("id") || !(body.get("id") instanceof String) || body.getString("id").length() == 0)
            return new ResponseData(new JSONObject().toString(), StatusCode.BAD_REQUEST);
        Agent agent = Agent.getById(body.getString("id"));
        if (agent != null)
            agent.setAuthToken(body.getString("token"));
        return new ResponseData(new JSONObject().toString(), StatusCode.OK);
    }

    @RestRequest(path = "/solve/{id}", method = "POST")
    public static ResponseData postSolve(@PathVariable("id") String id) {
        AgentSolvedResponse response = Agent.trySolve(id);
        if (response.isSuccess())
            return new ResponseData(new JSONObject().put("auth", response.getAuth()).put("cloudflare", response.getCloudflare()).toString(), StatusCode.OK);
        else
            return new ResponseData(new JSONObject().put("error", response.getError()).toString(), StatusCode.CONFLICT);
    }

    @RestRequest(path = "/master", method = "GET")
    public static ResponseData getMaster() {
        return new ResponseData(new JSONObject().put("id", Agent.masterId).toString(), StatusCode.OK);
    }

    @RestRequest(path = "/trusted/register", method = "POST")
    public static ResponseData postTrustedRegister(@RequestBody String b) {
        JSONObject body = new JSONObject(b);
        if (!body.has("address") || !(body.get("address") instanceof String) || body.getString("address").length() == 0 ||
                !body.has("name") || !(body.get("name") instanceof String) || body.getString("name").length() == 0 ||
                !body.has("challenge") || !(body.get("challenge") instanceof String) || body.getString("challenge").length() == 0 ||
                !body.has("id") || !(body.get("id") instanceof String) || body.getString("id").length() == 0 ||
                !body.has("position") || !(body.get("position") instanceof Integer) ||
                !body.has("status") || !(body.get("status") instanceof String) || body.getString("status").length() == 0)
            return new ResponseData("{}", StatusCode.BAD_REQUEST);
        if (Agent.getById(body.getString("id")) == null) {
            Agent.agents.put(body);
            Agent.save();
        }
        return new ResponseData("{}", StatusCode.OK);
    }

    @RestRequest(path = "/list", method = "GET")
    public static ResponseData getList() {
        return new ResponseData(Agent.agents.toString(), StatusCode.OK);
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

    @RestRequest(path = "/incident/{incidentId}", method = "POST")
    public static ResponseData postIncident(@PathVariable("incidentId") String id, @RequestBody String b) {
        JSONObject body = new JSONObject(b);
        Incident incident = Incident.getById(id);
        if (incident == null)
            return new ResponseData("{}", StatusCode.NOT_FOUND);
        body.keys().forEachRemaining(all -> {
            switch (all) {
                case "status": {
                    incident.setStatus(body.getString("status"));
                }
            }
        });
        return new ResponseData("{}", StatusCode.OK);
    }

}
