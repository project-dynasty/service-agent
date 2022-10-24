package de.alexanderwodarz.code.serviceagent.model.agent;

import de.alexanderwodarz.code.FileCore;
import de.alexanderwodarz.code.JavaCore;
import de.alexanderwodarz.code.cloudflare.zone.dns.DnsRecord;
import de.alexanderwodarz.code.log.Level;
import de.alexanderwodarz.code.log.Log;
import de.alexanderwodarz.code.serviceagent.Main;
import de.alexanderwodarz.code.serviceagent.client.AgentAPI;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
public class Agent {

    public static JSONArray agents;
    public static String masterKey;

    public static String masterId;
    public static int position = 0;
    private final JSONObject agent;

    public static AgentRegisterResponse register(JSONObject agent) {
        Agent filter = getById(agent.getString("id"));
        if (filter != null)
            return new AgentRegisterResponse(false, "duplicate id", "");
        String challenge = JavaCore.getRandomString();
        agent.put("challenge", challenge);
        agent.put("status", "new");
        agent.put("position", 0);
        agents.put(agent);
        save();
        return new AgentRegisterResponse(true, "", challenge);
    }

    public static Agent getById(String id) {
        for (int i = 0; i < agents.length(); i++)
            if (agents.getJSONObject(i).getString("id").equalsIgnoreCase(id))
                return new Agent(agents.getJSONObject(i));
        return null;
    }

    public static void save() {
        new FileCore().writeFile("agents.json", agents.toString());
    }

    public static void initAgents() {
        if (!new File("agents.json").exists()) {
            JSONArray arr = new JSONArray();
            JSONObject tmp = new JSONObject();
            tmp.put("challenge", "");
            tmp.put("id", Main.getSettings().getString("id"));
            tmp.put("position", 0);
            tmp.put("status", "trusted");
            try {
                tmp.put("name", InetAddress.getLocalHost().getHostName());
            } catch (Exception e) {
                tmp.put("name", "");
            }
            tmp.put("address", Main.getSettings().getString("address") + ":" + Main.getSettings().getInt("port"));
            arr.put(tmp);
            new FileCore().writeFile("agents.json", arr.toString());
        }
        Agent.agents = new JSONArray(new FileCore().readFile("agents.json"));
    }

    public static AgentSolvedResponse trySolve(String id) {
        Agent agent = getById(id);
        if (agent == null)
            return new AgentSolvedResponse(false, "not found", "", "");
        if (!agent.getStatus().equals("new"))
            return new AgentSolvedResponse(false, "already trusted", "", "");
        DnsRecord record = Main.getCf().getZoneByName(Main.getSettings().getString("domain")).listRecords().stream().filter(r -> r.getName().equals((id.toLowerCase()) + "." + Main.getSettings().getString("domain"))).findFirst().orElse(null);
        if (record == null)
            return new AgentSolvedResponse(false, "not created", "", "");
        if (!record.getContent().equals(agent.getChallenge()))
            return new AgentSolvedResponse(false, "wrong content", "", "");
        agent.agent.put("status", "trusted");
        save();
        for (int i = 0; i < agents.length(); i++) {
            Agent target = getById(agents.getJSONObject(i).getString("id"));
            if (target == null)
                continue;
            if (target.getId().equals(id))
                continue;
            if (!target.isOwn())
                AgentAPI.registerTrusted(target, agent.agent);
        }
        return new AgentSolvedResponse(true, "", Main.getSettings().getString("auth_token"), Main.getSettings().getString("cloudflare"));
    }

    public static Agent getRandomAgent() {
        Agent agent = null;
        if (agents.length() < 2)
            return null;
        if (agents.length() == 2) {
            for (int i = 0; i < agents.length(); i++) {
                Agent agent1 = getById(agents.getJSONObject(i).getString("id"));
                if (agent1.isOwn())
                    continue;
                agent = agent1;
            }
            if (!agent.isOnline())
                agent = null;
        } else {
            int trys = 0;
            while (true) {
                Agent tmp = getById(agents.getJSONObject(new Random().nextInt(agents.length())).getString("id"));
                trys++;
                if (trys == 10)
                    break;
                if (tmp == null || !tmp.isOnline())
                    continue;
                if (!tmp.isOwn()) {
                    agent = tmp;
                    break;
                }
            }
        }
        return agent;
    }

    public static List<Agent> getAgents() {
        List<Agent> agents = new ArrayList<>();
        for (int i = 0; i < Agent.agents.length(); i++)
            agents.add(new Agent(Agent.agents.getJSONObject(i)));
        return agents;
    }

    public static void calcFirstMaster() {
        for (Agent agent : getAgents()) {
            String m = agent.getMasterId();
            if (m == null)
                continue;
            masterId = m;
            break;
        }
        if (masterId == null)
            masterId = Agent.getOwn().getId();
        position = Agent.getById(masterId).getPosition();
        System.out.println(masterId);
        newMaster();
    }

    public static String calcNextMaster() {
        String result = null;
        int tries = 100;
        nextPosition();
        Agent next = null;
        do {
            tries--;
            Agent tmp = getAgents().stream().filter(a -> a.getPosition() == position).findFirst().orElse(null);
            try {
                if (tmp.isOnline())
                    next = tmp;
                else
                    nextPosition();
            } catch (Exception e) {
                nextPosition();
            }
        } while (tries != 0 && next == null);
        if (next != null)
            result = next.getId();
        if (result == null)
            result = getOwn().getId();
        masterId = result;
        newMaster();
        return result;
    }

    public static void newMaster() {
        if (getMaster().isOwn())
            return;
        masterKey = JavaCore.getRandomString(128);
        AgentAPI.sendAuthHeader(Main.getSettings().getString("id"), getMaster().getAddress(), masterKey);
        Agent.getOwn().setAuthToken(masterKey);
    }

    public static Agent getMaster() {
        return getById(masterId);
    }

    public static void nextPosition() {
        int max = 0;
        for (Agent agent : getAgents()) {
            if (agent.getPosition() > max)
                max = agent.getPosition();
        }
        if (position == max)
            position = 1;
        else
            position++;
    }

    public static Agent getOwn() {
        return Agent.getById(Main.getSettings().getString("id"));
    }

    public static int findIndex(String id) {
        int index = -1;
        for (int i = 0; i < agents.length(); i++) {
            if (!agents.getJSONObject(i).getString("id").equals(id))
                continue;
            index = i;
            break;
        }
        return index;
    }

    public static int findIndex(Agent agent) {
        return findIndex(agent.getId());
    }

    public String getAuthToken() {
        return Main.tokens.has(getId()) ? Main.tokens.getString(getId()) : null;
    }

    public void setAuthToken(String token) {
        Main.tokens.put(getId(), token);
        new FileCore().writeFile("token.json", Main.tokens.toString());
    }

    public boolean isOnline() {
        return AgentAPI.isOnline(getAddress());
    }

    public String getId() {
        return agent.getString("id");
    }

    public String getStatus() {
        return agent.getString("status");
    }

    public String getChallenge() {
        return agent.getString("challenge");
    }

    public int getPosition() {
        return agent.getInt("position");
    }

    public String getMasterId() {
        return AgentAPI.getCurrentMaster(getAddress());
    }

    public String getAddress() {
        return agent.getString("address");
    }

    public boolean isOwn() {
        return getId().equals(Main.getSettings().getString("id"));
    }

    public boolean isMaster() {
        return Agent.masterId.equals(getId());
    }

    public void remove() {
        Agent.agents.remove(findIndex(this));
        save();
        if (isOwn()) {
            Main.getSettings().put("configured", false);
            Main.getSettings().put("cloudflare", "");
            Main.saveSettings();
            Agent.agents = new JSONArray();
            Agent.save();
            Log.log("Ich wirst ausgeschließt werden", Level.ERROR);
            System.exit(1);
        }
    }

    public HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        if (getAuthToken() != null)
            headers.put("Authorization", getAuthToken());
        return headers;
    }

}
