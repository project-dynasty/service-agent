package de.alexanderwodarz.code.serviceagent.controller;

import com.sun.management.OperatingSystemMXBean;
import de.alexanderwodarz.code.web.rest.ResponseData;
import de.alexanderwodarz.code.web.rest.annotation.RestController;
import de.alexanderwodarz.code.web.rest.annotation.RestRequest;
import org.json.JSONObject;

import javax.management.MBeanServer;
import javax.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController(path = "/info", produces = MediaType.APPLICATION_JSON)
public class InformationController {

    @RestRequest(path = "/now", method = "GET")
    public static ResponseData get() {
        JSONObject body = new JSONObject();
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        JSONObject disk = new JSONObject();
        disk.put("free", osBean.getFreePhysicalMemorySize());
        disk.put("total", osBean.getTotalPhysicalMemorySize());
        disk.put("used", osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize());
        body.put("disk", disk);
        body.put("arch", osBean.getArch());
        body.put("name", osBean.getName());
        try {
            body.put("hostname", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException ignored) {
            body.put("hostname", "");
        }
        return new ResponseData(body.toString(), 200);
    }

}
