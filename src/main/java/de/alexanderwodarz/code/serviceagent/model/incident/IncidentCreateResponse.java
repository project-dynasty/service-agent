package de.alexanderwodarz.code.serviceagent.model.incident;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IncidentCreateResponse {

    private final boolean success;
    private final String error, id;
    private final int status;

}
