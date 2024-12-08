package com.chat.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private ChatRoom currentRoom;
    private volatile boolean isRunning = true;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            username = reader.readLine();
            sendRoomList();

            String message;
            while (isRunning && (message = reader.readLine()) != null) {
                try {
                    if (message.startsWith("/create ")) {
                        String roomName = message.substring(8).trim();
                        server.createRoom(roomName);
                        sendRoomList();
                    } else if (message.startsWith("/join ")) {
                        String roomName = message.substring(6).trim();
                        joinRoom(roomName);
                    } else if (message.equals("/list")) {
                        sendRoomList();
                    } else if (message.equals("/leave")) {
                        leaveRoom();
                    } else if (message.startsWith("/delete ")) {
                        String roomName = message.substring(8).trim();
                        server.forceDeleteRoom(roomName);
                    } else if (message.equals("/quit")) {
                        isRunning = false;
                        break;
                    } else if (currentRoom != null) {
                        currentRoom.broadcast(username + ": " + message, this);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void joinRoom(String roomName) {
        ChatRoom room = server.getRoom(roomName);
        if (room != null) {
            if (currentRoom != null) {
                leaveRoom();
            }
            currentRoom = room;
            room.addClient(this);
            room.broadcast("SYSTEM: " + username + "님이 입장하였습니다.", null);
            sendMessage("SYSTEM: " + roomName + " 채팅방에 입장하였습니다.");
            broadcastParticipants();
        } else {
            sendMessage("SYSTEM: 존재하지 않는 채팅방입니다: " + roomName);
        }
    }

    private void leaveRoom() {
        if (currentRoom != null) {
            try {
                currentRoom.broadcast("SYSTEM: " + username + "님이 퇴장하였습니다.", this);
                currentRoom.removeClient(this);
                broadcastParticipants();
                if (currentRoom.canBeDeleted()) {
                    server.removeRoom(currentRoom.getName());
                    sendRoomList();
                }
                currentRoom = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastParticipants() {
        if (currentRoom != null) {
            Set<String> participantNames = currentRoom.getClients().stream()
                    .map(ClientHandler::getUsername)
                    .collect(Collectors.toSet());
            String participantList = "PARTICIPANTS: " + String.join(",", participantNames);
            for (ClientHandler client : currentRoom.getClients()) {
                client.sendMessage(participantList);
            }
        }
    }

    private void cleanup() {
        isRunning = false;
        leaveRoom();
        server.removeClient(this);
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRoomList() {
        StringBuilder sb = new StringBuilder("ROOMS:");
        for (String roomName : server.getRoomList()) {
            sb.append(roomName).append(",");
        }
        sendMessage(sb.toString());
    }

    public void sendMessage(String message) {
        try {
            if (writer != null && isRunning) {
                writer.println(message);
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && isRunning;
    }
}