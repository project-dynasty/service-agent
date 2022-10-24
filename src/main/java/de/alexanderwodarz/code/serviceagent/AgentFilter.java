package de.alexanderwodarz.code.serviceagent;

import de.alexanderwodarz.code.log.Level;
import de.alexanderwodarz.code.log.Log;
import de.alexanderwodarz.code.serviceagent.model.agent.Agent;
import de.alexanderwodarz.code.web.rest.RequestData;
import de.alexanderwodarz.code.web.rest.authentication.AuthenticationFilter;
import de.alexanderwodarz.code.web.rest.authentication.AuthenticationFilterResponse;
import de.alexanderwodarz.code.web.rest.authentication.AuthenticationManager;

public class AgentFilter extends AuthenticationFilter {

    public static AuthenticationFilterResponse doFilter(RequestData request) {
        if (!request.getPath().equals("/agent/status")) {
            String spaces = "";
            for (int i = 10 - request.getMethod().length(); i > 0; i--)
                spaces += " ";
            Log.log(request.getMethod() + spaces + "|" + request.getPath(), Level.INFO);
        }
        String auth = request.getHeader("Authorization");
        if ((request.getPath().equals("/agent/register") && request.getMethod().equals("POST")) ||
                (request.getPath().equals("/agent/auth/token") && request.getMethod().equals("POST")) ||
                (request.getPath().equals("/agent/master") && request.getMethod().equals("GET")) ||
                (request.getPath().equals("/agent/status") && request.getMethod().equals("GET")) ||
                (request.getPath().startsWith("/agent/solve") && request.getMethod().equals("POST")))
            return AuthenticationFilterResponse.OK();

        else if (request.getPath().startsWith("/agent")) {
            if (auth == null)
                return AuthenticationFilterResponse.UNAUTHORIZED();
            Agent agent = Agent.getAgents().stream().filter(a -> a.getAuthToken() != null && a.getAuthToken().equals(auth)).findFirst().orElse(null);
            if (agent == null)
                return AuthenticationFilterResponse.UNAUTHORIZED();
            AuthenticationManager.setAuthentication(new AgentAuthentication(agent));
            return AuthenticationFilterResponse.OK();
        } else if (request.getPath().startsWith("/master")) {
            if (auth == null)
                return AuthenticationFilterResponse.UNAUTHORIZED();
            System.out.println(Agent.getOwn().getAuthToken());
            if (Agent.getOwn().getAuthToken() == null || !Agent.getOwn().getAuthToken().equals(auth))
                return AuthenticationFilterResponse.UNAUTHORIZED();
            Agent agent = Agent.getMaster();
            if (agent == null)
                return AuthenticationFilterResponse.UNAUTHORIZED();
            AuthenticationManager.setAuthentication(new AgentAuthentication(agent));
            return AuthenticationFilterResponse.OK();
        } else if (auth.equals("8492018472912jifejiofjeiojipkdnsfi0n1e0wfhn01jdf0")) {
            return AuthenticationFilterResponse.OK();
        } else
            return AuthenticationFilterResponse.UNAUTHORIZED();
    }
}
