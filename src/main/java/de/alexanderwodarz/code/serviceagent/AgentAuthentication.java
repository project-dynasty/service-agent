package de.alexanderwodarz.code.serviceagent;

import de.alexanderwodarz.code.serviceagent.model.agent.Agent;
import de.alexanderwodarz.code.web.rest.authentication.Authentication;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AgentAuthentication extends Authentication {

    private Agent agent;

}
