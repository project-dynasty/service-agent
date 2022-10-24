package de.alexanderwodarz.code.serviceagent.model.incident;

import de.alexanderwodarz.code.FileCore;
import de.alexanderwodarz.code.JavaCore;
import de.alexanderwodarz.code.serviceagent.AgentAuthentication;
import de.alexanderwodarz.code.web.StatusCode;
import de.alexanderwodarz.code.web.rest.authentication.AuthenticationManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Incident {

    private static JSONArray incidents;

    @Getter
    private final JSONObject incident;

    public static void save() {
        new FileCore().writeFile("incidents.json", incidents.toString());
    }

    public static void init() {
        if (!new File("incidents.json").exists()) new FileCore().writeFile("incidents.json", "[]");
        incidents = new JSONArray(new FileCore().readFile("incidents.json"));
    }

    public static List<Incident> getIncidents() {
        ArrayList<Incident> result = new ArrayList<>();
        for (int i = 0; i < incidents.length(); i++)
            result.add(new Incident(incidents.getJSONObject(i)));
        return result;
    }

    public static IncidentCreateResponse create(JSONObject body) {
        List<Incident> inc = getBefore(System.currentTimeMillis()).stream().filter(i -> {
            if (i.getStatus() != IncidentStatus.OPEN)
                return false;
            if (i.getType() != IncidentType.valueOf(body.getString("type")))
                return false;
            return i.getDescription().equals(body.getString("description"));
        }).collect(Collectors.toList());
        if (inc.size() != 0)
            return new IncidentCreateResponse(false, "Already reported", "", StatusCode.ALREADY_REPORTED.getCode());
        AgentAuthentication authentication = (AgentAuthentication) AuthenticationManager.getAuthentication();
        String id = generateId();
        JSONObject incident = new JSONObject();
        incident.put("start", System.currentTimeMillis());
        incident.put("reporter", authentication.getAgent().getId());
        incident.put("status", body.getString("status"));
        if (body.has("end") && body.get("end") instanceof Long)
            incident.put("end", body.getLong("end"));
        else
            incident.put("end", 0L);
        incident.put("type", body.getString("type"));
        incident.put("description", body.getString("description"));
        incident.put("effected", body.getJSONArray("effected"));
        incident.put("id", id);
        incidents.put(incident);
        save();
        return new IncidentCreateResponse(true, "", id, StatusCode.OK.getCode());
    }

    public static Incident getById(String id) {
        return getIncidents().stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }

    private static String generateId() {
        String id = "";
        while (true) {
            String rdm = JavaCore.getRandomString(4);
            if (Incident.getById(rdm) != null)
                continue;
            id = rdm;
            break;
        }
        return id;
    }

    public static List<Incident> getBefore(List<Incident> incidents, long time) {
        return incidents.stream().filter(i -> i.getStart() < time).collect(Collectors.toList());
    }

    public static List<Incident> getBefore(long time) {
        return getBefore(getIncidents(), time);
    }

    public static List<Incident> getAfter(long time) {
        return getAfter(getIncidents(), time);
    }

    public static List<Incident> getAfter(List<Incident> incidents, long time) {
        return incidents.stream().filter(i -> i.getStart() > time).collect(Collectors.toList());
    }

    public long getStart() {
        return incident.getLong("start");
    }

    public long getEnd() {
        return incident.getLong("end");
    }

    public IncidentStatus getStatus() {
        try {
            return IncidentStatus.valueOf(incident.getString("status"));
        } catch (Exception e) {
            return null;
        }
    }

    public void setStatus(String status) {
        try {
            IncidentStatus s = IncidentStatus.valueOf(status);
            incident.put("status", s);
            if (s == IncidentStatus.CLOSED)
                incident.put("end", System.currentTimeMillis());
            Incident.save();
        } catch (Exception e) {

        }
    }

    public IncidentType getType() {
        try {
            return IncidentType.valueOf(incident.getString("type"));
        } catch (Exception e) {
            return null;
        }
    }

    public String getReporter() {
        return incident.getString("reporter");
    }

    public String getDescription() {
        return incident.getString("description");
    }

    public String getId() {
        return incident.getString("id");
    }

    public JSONArray getEffected() {
        return incident.getJSONArray("effected");
    }

}
