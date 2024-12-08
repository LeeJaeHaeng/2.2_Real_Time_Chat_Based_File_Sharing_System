package com.chat.server;

import javax.swing.*;

public class main {
    public static void main(String[] args) {
        try {
            // MySQL JDBC 드라이버 로드
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 데이터베이스 초기화
            DatabaseManager.initializeDatabase();

            // 채팅 서버 시작
            startChatServer();

            // GUI 시작
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    new LoginFrame().setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "GUI 초기화 오류: " + e.getMessage(),
                            "오류",
                            JOptionPane.ERROR_MESSAGE);
                }
            });

        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                    "MySQL JDBC 드라이버를 찾을 수 없습니다: " + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void startChatServer() {
        new Thread(() -> {
            try {
                ChatServer server = new ChatServer(9002);
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("채팅 서버 시작 실패: " + e.getMessage());
            }
        }).start();
    }
}