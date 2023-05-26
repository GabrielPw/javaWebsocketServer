package com.gabrielxavier.socketchat.handler;

import com.gabrielxavier.socketchat.model.ConnectedClient;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    private final Map<WebSocketSession, ConnectedClient> sessions = new HashMap<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        String sessionId = session.getId();
        ConnectedClient connectedClient = new ConnectedClient(sessionId, "");

        sessions.put(session,connectedClient);
        sendIdToConnectedClient(session);
        sendIdList(sessions);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        String payload = message.getPayload();

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(payload, JsonObject.class);

        if (jsonObject.has("type")) {
            String type = jsonObject.get("type").getAsString();
            if (type.equals("System")){
                // Setando foto do cliente e nome.
                if(jsonObject.get("content").getAsString().equals("setClientPhoto")){
                    String urlPhoto = jsonObject.get("urlPhoto").getAsString();
                    String clientName = jsonObject.get("clientName").getAsString();

                    this.sessions.get(session).setUrlPhoto(urlPhoto);
                    this.sessions.get(session).setName(clientName);

                    System.out.println("Id: " + session.getId() + " - img: " + urlPhoto);
                }else if (jsonObject.get("content").getAsString().equals("getIdList")){ // Cliente requisitou a lista de id's. (será usado para saber a lista de pessoas online no momento)
                    sendIdList(sessions);
                }

            } else if (type.equals("Chat")) {

                // obter id de cliente que está enviando a mensagem. obter id do destinatário. obter mensagem enviada.
                String clientId = jsonObject.get("sessionId").getAsString();
                String messageToId = jsonObject.get("messageTo").getAsString();
                String receivedMessage = jsonObject.get("content").getAsString();

                System.out.println("Mesagem Recebida.");
                System.out.println("Mesagem: " + receivedMessage + "- < " + clientId + ">");
                System.out.println("Destinatáio: " + messageToId);

                WebSocketSession messageToSession = null;
                // Procurar a sessão correspondente ao messageToId
                for (WebSocketSession session_ : sessions.keySet()) {
                    if (sessions.get(session_).getSessionId().equals(messageToId)) {
                        messageToSession = session_;
                        break;
                    }
                }
                JsonObject messageJson = new JsonObject();
                System.out.println("Message to session: " + messageToSession);
                if (messageToSession != null) {

                    messageJson.addProperty("content", receivedMessage);
                    messageJson.addProperty("type", "Chat");
                    messageJson.addProperty("sessionId", clientId);

                    System.out.println("Preparando para enviar mensagem. type: " + messageJson.get("type").getAsString());
                    messageToSession.sendMessage(new TextMessage(messageJson.toString()));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        sendIdList(sessions);
    }

    // método para enviar o id do cliente conectado.
    private void sendIdToConnectedClient(WebSocketSession session) throws IOException {

        String sessionId = session.getId();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("content", "getId");
        jsonObject.addProperty("type", "System");
        jsonObject.addProperty("sessionId", sessionId);

        String connectedClientJson = jsonObject.toString();
        session.sendMessage(new TextMessage(connectedClientJson));
    }

    // método para enviar lista com o id de todos os clientes.
    private void sendIdList(Map<WebSocketSession, ConnectedClient> sessionMap) throws IOException {

        System.out.println("Length: " + sessions.size());
        List<WebSocketSession> sessionList = new ArrayList<>(sessionMap.keySet());
        System.out.println("sessionList Length: " + sessionList.size());

        List<ConnectedClient> connectedClientList = new ArrayList<>(sessionMap.values());

        System.out.println("clientList: " + connectedClientList);
        System.out.println("fotos da lista: ");
        connectedClientList.forEach(connectedClient -> {
            System.out.println(connectedClient.getUrlPhoto());
        });

        Gson gson = new Gson();
        Map<String, Object> idListJson = new HashMap<>();
        idListJson.put("content", "idList");
        idListJson.put("type", "System");
        idListJson.put("connectedClientList", gson.toJson(connectedClientList));

        for (WebSocketSession session : sessionList) {
            session.sendMessage(new TextMessage(gson.toJson(idListJson)));
        }
    }

}
