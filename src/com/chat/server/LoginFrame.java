// LoginFrame.java
package com.chat.server;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("채팅 로그인");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupGUI();
        setSize(300, 200);
        setLocationRelativeTo(null);
    }

    private void setupGUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 사용자 이름 입력
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("사용자 이름:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        add(usernameField, gbc);

        // 비밀번호 입력
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("비밀번호:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        add(passwordField, gbc);

        // 로그인 버튼
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JButton loginButton = new JButton("로그인");
        loginButton.addActionListener(e -> handleLogin());
        add(loginButton, gbc);

        // 회원가입 버튼
        gbc.gridy = 3;
        JButton registerButton = new JButton("회원가입");
        registerButton.addActionListener(e -> showRegisterDialog());
        add(registerButton, gbc);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "사용자 이름과 비밀번호를 모두 입력해주세요.");
            return;
        }

        try {
            if (DatabaseManager.validateUser(username, password)) {
                dispose();
                // ChatClient 대신 ChatRoomListFrame을 생성
                SwingUtilities.invokeLater(() -> new ChatRoomListFrame(username).setVisible(true));
            } else {
                JOptionPane.showMessageDialog(this, "잘못된 사용자 이름 또는 비밀번호입니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void showRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this);
        dialog.setVisible(true);
    }
}