package de.alexanderwodarz.code.serviceagent;

import de.alexanderwodarz.code.FileCore;
import de.alexanderwodarz.code.JavaCore;
import de.alexanderwodarz.code.cloudflare.CloudFlare;
import de.alexanderwodarz.code.cloudflare.zone.dns.DnsRecord;
import de.alexanderwodarz.code.log.Level;
import de.alexanderwodarz.code.log.Log;
import de.alexanderwodarz.code.serviceagent.client.AgentAPI;
import de.alexanderwodarz.code.serviceagent.model.agent.Agent;
import de.alexanderwodarz.code.serviceagent.model.incident.Incident;
import de.alexanderwodarz.code.serviceagent.model.incident.IncidentStatus;
import de.alexanderwodarz.code.serviceagent.model.incident.IncidentType;
import de.alexanderwodarz.code.web.WebCore;
import de.alexanderwodarz.code.web.rest.annotation.RestApplication;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

@RestApplication
public class Main {

    public static JSONObject tokens = new JSONObject();
    private static JSONObject settings;
    private static CloudFlare cf;

    public static JSONObject getSettings() {
        return settings;
    }

    @SneakyThrows
    public static void main(String[] args) {
        JavaCore.initLog();
        initSettings();
        if (!getSettings().getBoolean("configured")) {
            if (args.length == 0) {
                Log.log("This agent has to be configured.", Level.WARNING);
                Log.log(new JSONObject().put("address", "ADDRESS"), Level.WARNING);
                System.exit(1);
            }
            JSONObject config = new JSONObject(args[0]);
            JSONObject request = new JSONObject();
            request.put("id", getSettings().getString("id"));
            request.put("name", InetAddress.getLocalHost().getHostName());
            request.put("address", getSettings().getString("address") + ":" + getSettings().getInt("port"));
            String challenge = AgentAPI.register(config.getString("address"), request);
            if (challenge.length() == 0) {
                Log.log("An error occurred. The challenge creation has failed.", Level.ERROR);
                System.exit(1);
            }
            if (challenge.equals("cant connect")) {
                Log.log("Could not connect to the agent. Please check the address (" + config.getString("address") + ")", Level.ERROR);
                System.exit(1);
            }
            Log.log("Challenge was created.", Level.INFO);
            Log.log("Please create a TXT record " + getSettings().getString("id") + "." + getSettings().getString("domain") + " with the content " + challenge, Level.INFO);
            Log.log("Press any key when the Cloudflare entry is configured.", Level.INFO);
            new Scanner(System.in).nextLine();
            Log.log("Connection with the agent...", Level.INFO);
            JSONObject solve = AgentAPI.solve(config.getString("address"), getSettings().getString("id"));
            if (!solve.has("auth")) {
                Log.log("The challenge has failed.", Level.ERROR);
                Log.log(solve.getString("error"), Level.ERROR);
                System.exit(1);
            }
            Log.log("Successfully verified", Level.INFO);
            getSettings().put("auth_token", solve.getString("auth"));
            getSettings().put("cloudflare", solve.getString("cloudflare"));
            getSettings().put("configured", true);
            saveSettings();
            Log.log("Loading agent list...", Level.INFO);
            String token = JavaCore.getRandomString(128);
            AgentAPI.sendAuthHeader(config.getString("address"), settings.getString("id"), token);
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", token);
            Agent.agents = AgentAPI.listAgents(config.getString("address"), headers);
            Agent.save();
            Log.log("Agent " + getSettings().getString("id") + " was successfully configured.", Level.INFO);
            System.exit(1);
        } else {
            new FileCore().writeFile("token.json", tokens.toString());
            Agent.initAgents();
            Incident.init();
            cf = new CloudFlare(settings.getString("cloudflare"));
            checkCloudflareENtrys();
            Agent.calcFirstMaster();
            if (!Agent.getMaster().isOwn()) {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", Agent.masterKey);
                Agent.agents = AgentAPI.listAgents(Agent.getMaster().getAddress(), headers);
                Agent.save();
            }
            Log.log("Starting RestAPI on Port " + settings.getInt("port"), Level.INFO);
            WebCore.start(Main.class, settings.getInt("port"));
            doTask();
        }
    }

    public static void checkCloudflareENtrys() {
        for (DnsRecord domain : getCf().getZoneByName(getSettings().getString("domain")).listRecords()) {
            if (!domain.getType().equals("TXT") || !domain.getName().startsWith("remove-agent"))
                continue;
            Agent agent = Agent.getById(domain.getContent());
            if (agent == null)
                continue;
            agent.remove();
            System.out.println(domain.getType() + " => " + domain.getName() + " => " + domain.getContent());
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                checkCloudflareENtrys();
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 60 * 5 * 1000);
    }

    public static void doTask() {
        for (Agent agent : Agent.getAgents()) {
            if (agent.isOwn())
                continue;
            if (agent.getStatus().equals("new"))
                continue;
            JSONObject status = AgentAPI.getStatus(agent.getAddress());
            if (status.has("error")) {
                if (status.getString("error").equals("connection refused") && agent.isMaster())
                    Agent.calcNextMaster();
                int code = AgentAPI.registerIncident(IncidentStatus.OPEN, IncidentType.CRITICAL, agent.getId() + " is not reachable", new JSONArray().put(agent.getId()));
                if (code == 200)
                    Log.log(agent.getId() + " got error " + status.getString("error"), Level.ERROR);
            }
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                doTask();
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 10 * 1000);
    }

    public static CloudFlare getCf() {
        return cf;
    }

    public static void saveSettings() {
        new FileCore().writeFile("settings.json", settings.toString());
    }

    public static void initSettings() {
        if (!new File("settings.json").exists()) {
            JSONObject init = new JSONObject();
            init.put("auth_token", JavaCore.getRandomString(128));
            init.put("port", JavaCore.getRandomInt(20000, 30000));
            init.put("configured", false);
            init.put("position", 0);
            init.put("interval", 600);
            init.put("id", JavaCore.getRandomString("abcdefghijklmnopqrstuvwxyz1234567890", 4));
            init.put("cloudflare", "");
            init.put("domain", "project-dynasty.de");
            init.put("address", "127.0.0.1");
            new FileCore().writeFile("settings.json", init.toString());
            Log.log("Settings created", Level.SYSTEM);
            System.exit(1);
        }
        settings = new JSONObject(new FileCore().readFile("settings.json"));
    }


}
