package de.alexanderwodarz.code.serviceagent.model.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AgentRegisterResponse {

    private final boolean success;
    private final String error, challenge;

}
