package com.chat.server;

import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/chat_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASSWORD = "0000";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("MySQL JDBC Driver not found.");
            System.exit(1);
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE DATABASE IF NOT EXISTS chat_db");
            stmt.execute("USE chat_db");

            // 사용자 테이블 생성
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(50) PRIMARY KEY," +
                    "password VARCHAR(100) NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 채팅방 테이블 생성
            stmt.execute("CREATE TABLE IF NOT EXISTS chat_rooms (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100) NOT NULL UNIQUE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 채팅 메시지 테이블 재생성
            stmt.execute("DROP TABLE IF EXISTS chat_messages");
            stmt.execute("CREATE TABLE chat_messages (" +
                    "message_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "room_name VARCHAR(100) NOT NULL," +
                    "sender VARCHAR(50) NOT NULL," +
                    "message TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "is_system_message BOOLEAN DEFAULT FALSE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 사용자 로그인 검증
    public static boolean validateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("User validation failed: " + e.getMessage());
            return false;
        }
    }

    // 새 사용자 등록
    public static boolean registerUser(String username, String password) {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("User registration failed: " + e.getMessage());
            return false;
        }
    }

    // 사용자 존재 여부 확인
    public static boolean userExists(String username) {
        String query = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("User check failed: " + e.getMessage());
            return false;
        }
    }

    // 채팅 메시지 저장
    public static void saveMessage(String roomName, String sender, String message, boolean isSystemMessage) {
        String sql = "INSERT INTO chat_messages (room_name, sender, message, is_system_message) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomName);
            pstmt.setString(2, sender);
            pstmt.setString(3, message);
            pstmt.setBoolean(4, isSystemMessage);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 채팅방의 이전 메시지 불러오기
    public static List<ChatMessage> loadChatHistory(String roomName) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT sender, message, created_at, is_system_message FROM chat_messages " +
                "WHERE room_name = ? ORDER BY created_at ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ChatMessage msg = new ChatMessage(
                        rs.getString("sender"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("is_system_message")
                );
                messages.add(msg);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    // 채팅방 생성
    public static boolean createChatRoom(String roomName) {
        String sql = "INSERT INTO chat_rooms (name) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomName);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 채팅방 삭제
    public static boolean deleteChatRoom(String roomName) {
        String sql = "DELETE FROM chat_rooms WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomName);
            pstmt.executeUpdate();
            deleteRoomMessages(roomName);  // 관련 메시지도 삭제
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 채팅방 메시지 삭제
    public static void deleteRoomMessages(String roomName) {
        String sql = "DELETE FROM chat_messages WHERE room_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomName);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 채팅방 목록 가져오기
    public static List<String> getChatRooms() {
        List<String> rooms = new ArrayList<>();
        String sql = "SELECT name FROM chat_rooms ORDER BY id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rooms.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }
}