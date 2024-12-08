package com.chat.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private final ServerSocket serverSocket;
    private final Map<String, ChatRoom> chatRooms;
    private final List<ClientHandler> clients;
    private final ExecutorService executorService;

    public ChatServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        chatRooms = new ConcurrentHashMap<>();
        clients = new CopyOnWriteArrayList<>();
        executorService = Executors.newCachedThreadPool();
        System.out.println("서버가 포트 " + port + "에서 시작되었습니다.");
    }

    public void start() {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                executorService.execute(clientHandler);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public synchronized void createRoom(String roomName) {
        // 이미 존재하는 방이면 생성하지 않음
        if (!chatRooms.containsKey(roomName)) {
            chatRooms.put(roomName, new ChatRoom(roomName));
            System.out.println("새로운 채팅방 생성: " + roomName);
            broadcastRoomList();
        }
    }

    public synchronized void removeRoom(String roomName) {
        ChatRoom room = chatRooms.get(roomName);
        if (room != null && room.isEmpty()) {
            chatRooms.remove(roomName);
            System.out.println("채팅방 삭제: " + roomName);
            broadcastRoomList();
        }
    }

    public synchronized void forceDeleteRoom(String roomName) {
        ChatRoom room = chatRooms.get(roomName);
        if (room != null) {
            Set<ClientHandler> roomClients = new HashSet<>(room.getClients());
            for (ClientHandler client : roomClients) {
                client.sendMessage("SYSTEM: 채팅방이 삭제되었습니다.");
                room.removeClient(client);
            }
            chatRooms.remove(roomName);
            DatabaseManager.deleteRoomMessages(roomName);
            System.out.println("채팅방 강제 삭제: " + roomName);
            broadcastRoomList();
        }
    }

    private void broadcastRoomList() {
        String roomList = "ROOMS:" + String.join(",", getRoomList());
        for (ClientHandler client : clients) {
            if (client != null && client.isConnected()) {
                client.sendMessage(roomList);
            }
        }
    }

    public Set<String> getRoomList() {
        return new HashSet<>(chatRooms.keySet());
    }

    public ChatRoom getRoom(String roomName) {
        return chatRooms.get(roomName);
    }

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public void shutdown() {
        try {
            for (ClientHandler client : clients) {
                try {
                    client.sendMessage("SYSTEM: 서버가 종료됩니다.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (ChatRoom room : chatRooms.values()) {
                for (ClientHandler client : room.getClients()) {
                    try {
                        room.removeClient(client);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }

            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        } catch (IOException e) {
            e.printStackTrace();
            executorService.shutdownNow();
        }
    }
}