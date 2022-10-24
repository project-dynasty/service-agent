package de.alexanderwodarz.code.serviceagent.model.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AgentSolvedResponse {

    private final boolean success;
    private String error, auth, cloudflare;

}
