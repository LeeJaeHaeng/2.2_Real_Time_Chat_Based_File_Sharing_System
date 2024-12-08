package com.chat.server;

import javax.swing.*;
import java.awt.*;

public class RegisterDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    public RegisterDialog(Frame owner) {
        super(owner, "회원가입", true);
        setupGUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void setupGUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 사용자 이름
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("사용자 이름:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        add(usernameField, gbc);

        // 비밀번호
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("비밀번호:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        add(passwordField, gbc);

        // 비밀번호 확인
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("비밀번호 확인:"), gbc);

        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(15);
        add(confirmPasswordField, gbc);

        // 가입하기 버튼
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton registerButton = new JButton("가입하기");
        registerButton.addActionListener(e -> handleRegister());
        add(registerButton, gbc);
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "모든 필드를 입력해주세요.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.");
            return;
        }

        if (DatabaseManager.userExists(username)) {
            JOptionPane.showMessageDialog(this, "이미 존재하는 사용자 이름입니다.");
            return;
        }

        if (DatabaseManager.registerUser(username, password)) {
            JOptionPane.showMessageDialog(this, "회원가입이 완료되었습니다.");
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "회원가입 실패. 다시 시도해주세요.");
        }
    }
}
