package com.chat.server;

import java.util.HashSet;
import java.util.Set;

public class ChatRoom {
    private String name;
    private Set<ClientHandler> clients;
    private boolean hasMessages;

    public ChatRoom(String name) {
        this.name = name;
        this.clients = new HashSet<>();
        this.hasMessages = false;
    }

    public String getName() {
        return name;
    }

    public void addClient(ClientHandler client) {
        clients.add(client);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public Set<ClientHandler> getClients() {
        return new HashSet<>(clients);  // 복사본 반환하여 동시성 문제 방지
    }

    public void broadcast(String message, ClientHandler sender) {
        // 시스템 메시지가 아닐 경우 메시지 플래그 설정
        if (!message.startsWith("SYSTEM: ")) {
            hasMessages = true;
        }

        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public boolean isEmpty() {
        return clients.isEmpty();
    }

    public boolean canBeDeleted() {
        return isEmpty() && !hasMessages;
    }
}