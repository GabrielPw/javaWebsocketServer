package com.gabrielxavier.socketchat.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectedClient {
    private String sessionId;
    private String name;
    private String urlPhoto;

    public ConnectedClient(String sessionId, String clientName) {
        this.sessionId = sessionId;
        this.name = clientName;
    }

    public String getName() {
        return name;
    }
}
