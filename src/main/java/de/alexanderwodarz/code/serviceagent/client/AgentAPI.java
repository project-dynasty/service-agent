package de.alexanderwodarz.code.serviceagent.client;

import com.sun.jersey.api.client.ClientHandlerException;
import de.alexanderwodarz.code.rest.ClientThread;
import de.alexanderwodarz.code.serviceagent.model.agent.Agent;
import de.alexanderwodarz.code.serviceagent.model.incident.IncidentStatus;
import de.alexanderwodarz.code.serviceagent.model.incident.IncidentType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class AgentAPI {

    public static JSONObject getStatus(String address) {
        ClientThread thread = new ClientThread((address.startsWith("http") ? address : "http://" + address) + "/agent/status", ClientThread.RequestMethod.GET);
        try {
            thread.run();
            while (thread.isAlive()) {
            }
        } catch (Exception e) {
            if (e instanceof ClientHandlerException)
                if (e.getMessage().contains("Connection refused"))
                    return new JSONObject().put("error", "connection refused");
            return new JSONObject().put("error", "unknown error");
        }
        return new JSONObject(thread.getResponse());
    }

    public static void updateIncident(String id, JSONObject body) {
        if (Agent.getOwn().isMaster()) {
            //Incident.create(incident);
            return;
        }
        Request request = request(Agent.getMaster().getAddress(), "/agent/incident/" + id, ClientThread.RequestMethod.POST, body, Agent.getOwn().getHeaders());
        System.out.println(request.getStatus());
    }

    public static int registerIncident(IncidentStatus status, IncidentType type, String description, JSONArray effected) {
        JSONObject incident = new JSONObject();
        incident.put("status", status);
        incident.put("type", type);
        incident.put("description", description);
        incident.put("effected", effected);
        if (Agent.getOwn().isMaster()) {
            //Incident.create(incident);
            return 0;
        }
        return request(Agent.getMaster().getAddress(), "/agent/incident", ClientThread.RequestMethod.PUT, incident, Agent.getOwn().getHeaders()).getStatus();
    }

    public static void sendAuthHeader(Agent agent, String token) {
        sendAuthHeader(agent.getId(), agent.getAddress(), token);
    }

    public static void sendAuthHeader(String id, String address, String token) {
        JSONObject body = new JSONObject();
        body.put("token", token);
        body.put("id", id);
        Request request = request(address, "/agent/auth/token", ClientThread.RequestMethod.POST, body);
        System.out.println(request.getStatus());
    }

    public static String register(String address, JSONObject registerClient) {
        ClientThread thread = new ClientThread(address + "/agent/register", ClientThread.RequestMethod.POST);
        thread.setBody(registerClient);
        try {
            thread.run();
        } catch (Exception e) {
            return "cant connect";
        }
        while (thread.isAlive()) {
        }
        if (thread.getStatus() == 200)
            return new JSONObject(thread.getResponse()).getString("challenge");
        else
            return "";
    }

    public static boolean isOnline(String address) {
        return request(address, "/agent/list", ClientThread.RequestMethod.GET).getStatus() != 499;
    }

    public static JSONObject solve(String address, String id) {
        System.out.println(address);
        ClientThread thread = new ClientThread(address + "/agent/solve/" + id, ClientThread.RequestMethod.POST);
        thread.run();
        while (thread.isAlive()) {
        }
        return new JSONObject(thread.getResponse());
    }

    public static void registerTrusted(Agent agent, JSONObject newClient) {
        try {
            request(agent.getAddress(), "/agent/trusted/register", ClientThread.RequestMethod.POST, newClient, agent.getHeaders());
        } catch (Exception ignored) {
        }
    }

    public static JSONArray listAgents(Agent agent) {
        return listAgents(agent.getAddress(), agent.getHeaders());
    }

    public static JSONArray listAgents(String address, HashMap<String, String> headers) {
        try {
            return request(address, "/agent/list", ClientThread.RequestMethod.GET, headers).getArray();
        } catch (Exception e) {
            return Agent.agents;
        }
    }

    public static String getCurrentMaster(String address) {
        Request request = request(address, "/agent/master", ClientThread.RequestMethod.GET);
        if (request.getStatus() == 499)
            return null;
        return request.getObject().getString("id");
    }

    public static Request request(String address, String url, ClientThread.RequestMethod method) {
        return request(address, url, method, (JSONObject) null);
    }

    public static Request request(String address, String url, ClientThread.RequestMethod method, JSONObject body) {
        return request(address, url, method, body, null);
    }

    public static Request request(String address, String url, ClientThread.RequestMethod method, HashMap<String, String> headers) {
        return request(address, url, method, null, headers);
    }

    public static Request request(String address, String url, ClientThread.RequestMethod method, JSONObject body, HashMap<String, String> headers) {
        return new Request((address.startsWith("http") ? address : "http://" + address) + url, method, body, headers);
    }


}
