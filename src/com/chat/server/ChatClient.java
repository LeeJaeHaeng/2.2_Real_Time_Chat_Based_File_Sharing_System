package com.chat.server;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class ChatClient extends JFrame {
    private JPanel chatPanel;
    private JScrollPane scrollPane;
    private JTextField messageField;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String username;
    private String roomName;
    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 9002;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("오후 h:mm");
    private Color myMessageColor = new Color(254, 229, 0);
    private Color kakaoBackground = new Color(178, 199, 217);
    private volatile boolean isRunning = true;
    private JLabel participantsLabel;
    private List<String> participants = new ArrayList<>();

    public ChatClient(String username, String roomName) {
        this.username = username;
        this.roomName = roomName;
        participants.add(username);
        setTitle(roomName);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setupGUI();
        connectToServer();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                handleClosing();
            }
        });
    }

    private void setupGUI() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400, 600));

        add(createHeaderPanel(), BorderLayout.NORTH);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(kakaoBackground);
        chatPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
        add(createInputPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        // 왼쪽 - 뒤로가기와 방 제목
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JButton backButton = new JButton("←");
        backButton.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> handleClosing());

        JLabel titleLabel = new JLabel(roomName);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));

        leftPanel.add(backButton);
        leftPanel.add(titleLabel);

        // 오른쪽 패널 - 사용자 수와 버튼들
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        // 참가자 수 라벨
        participantsLabel = new JLabel();
        participantsLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        participantsLabel.setForeground(new Color(100, 100, 100));
        updateParticipantsLabel();

        // 사용자 목록 버튼
        JButton userListButton = new JButton("👥");
        userListButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        userListButton.setBorderPainted(false);
        userListButton.setContentAreaFilled(false);
        userListButton.setFocusPainted(false);
        userListButton.addActionListener(e -> showUserListDialog());
        userListButton.setToolTipText("참가자 목록");

        // 설정 버튼
        JButton settingsButton = new JButton("⚙");
        settingsButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        settingsButton.setBorderPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setFocusPainted(false);
        settingsButton.addActionListener(e -> showDeleteDialog());
        settingsButton.setToolTipText("채팅방 설정");

        rightPanel.add(userListButton);
        rightPanel.add(settingsButton);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        // 구분선 추가
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(220, 220, 220));
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(Color.WHITE);
        wrapperPanel.add(headerPanel, BorderLayout.CENTER);
        wrapperPanel.add(separator, BorderLayout.SOUTH);

        return wrapperPanel;
    }

    private void showUserListDialog() {
        JDialog dialog = new JDialog(this, "참가자 목록", true);
        dialog.setLayout(new BorderLayout());

        // 제목 패널
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel titleLabel = new JLabel("참가자 " + participants.size() + "명");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        titlePanel.add(titleLabel);

        // 참가자 목록 패널
        JPanel userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        userListPanel.setBackground(Color.WHITE);

        // 참가자 목록 추가
        for (int i = 0; i < participants.size(); i++) {
            String participant = participants.get(i);

            JPanel userPanel = new JPanel(new BorderLayout());
            userPanel.setBackground(Color.WHITE);
            userPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

            JLabel userLabel = new JLabel(participant);
            userLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));

            // 현재 사용자인 경우 "(나)" 표시
            if (participant.equals(username)) {
                userLabel.setText(participant + " (나)");
                userLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            }

            userPanel.add(userLabel, BorderLayout.CENTER);
            userListPanel.add(userPanel);

            // 마지막 항목이 아니면 구분선 추가
            if (i < participants.size() - 1) {
                JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
                separator.setForeground(new Color(230, 230, 230));
                userListPanel.add(separator);
            }
        }

        // 스크롤 패널에 추가
        JScrollPane scrollPane = new JScrollPane(userListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        dialog.add(titlePanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // 닫기 버튼 패널
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // 다이얼로그 설정
        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        messageField = new JTextField();
        messageField.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JButton sendButton = new JButton("전송");
        sendButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        sendButton.setBackground(myMessageColor);
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(60, 35));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        return inputPanel;
    }

    private void showDeleteDialog() {
        int option = JOptionPane.showConfirmDialog(this,
                "이 채팅방을 삭제하시겠습니까?\n삭제된 채팅방은 복구할 수 없습니다.",
                "채팅방 삭제",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            writer.println("/delete " + roomName);
            handleClosing();
        }
    }

    private void updateParticipantsLabel() {
        String participantText = participants.size() + "명 참여 중";
        participantsLabel.setText(participantText);
        participantsLabel.setToolTipText(String.join(", ", participants));
    }

    private void addParticipant(String username) {
        if (!participants.contains(username)) {
            participants.add(username);
            updateParticipantsLabel();
        }
    }

    private void removeParticipant(String username) {
        if (participants.remove(username)) {
            updateParticipantsLabel();
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println(username);
            writer.println("/join " + roomName);

            loadChatHistory();

            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "서버 연결 실패: " + e.getMessage(),
                        "연결 오류",
                        JOptionPane.ERROR_MESSAGE);
                dispose();
            });
        }
    }

    private void loadChatHistory() {
        List<ChatMessage> history = DatabaseManager.loadChatHistory(roomName);
        for (ChatMessage msg : history) {
            String time = timeFormat.format(msg.getTimestamp());
            if (msg.isSystemMessage()) {
                addMessage("SYSTEM", msg.getMessage(), time, false);
            } else {
                addMessage(msg.getSender(), msg.getMessage(), time, msg.getSender().equals(username));
                addParticipant(msg.getSender());
            }
        }
    }

    private void handleClosing() {
        closeConnection();
        SwingUtilities.invokeLater(() -> {
            dispose();
        });
    }

    private void closeConnection() {
        try {
            isRunning = false;
            if (writer != null) {
                writer.println("/leave");
                writer.flush();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addMessage(String sender, String message, String time, boolean isMe) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout(5, 5));
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        if (isMe) {
            JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
            rightPanel.setOpaque(false);

            JLabel timeLabel = new JLabel(time);
            timeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            timeLabel.setForeground(Color.GRAY);
            timeLabel.setVerticalAlignment(JLabel.BOTTOM);

            JPanel bubblePanel = new JPanel(new BorderLayout());
            bubblePanel.setBackground(myMessageColor);

            JTextArea textArea = new JTextArea(message);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            textArea.setBackground(myMessageColor);
            textArea.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            bubblePanel.add(textArea);

            rightPanel.add(timeLabel, BorderLayout.WEST);
            rightPanel.add(bubblePanel, BorderLayout.EAST);

            messagePanel.add(rightPanel, BorderLayout.EAST);
        } else if (!sender.equals("SYSTEM")) {
            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.setOpaque(false);

            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftPanel.add(senderLabel);

            JPanel messageTimePanel = new JPanel(new BorderLayout(5, 0));
            messageTimePanel.setOpaque(false);
            messageTimePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            messageTimePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel bubblePanel = new JPanel(new BorderLayout());
            bubblePanel.setBackground(Color.WHITE);

            JTextArea textArea = new JTextArea(message);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            textArea.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            bubblePanel.add(textArea);

            JLabel timeLabel = new JLabel(time);
            timeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            timeLabel.setForeground(Color.GRAY);
            timeLabel.setVerticalAlignment(JLabel.BOTTOM);

            messageTimePanel.add(bubblePanel, BorderLayout.WEST);
            messageTimePanel.add(timeLabel, BorderLayout.EAST);

            leftPanel.add(Box.createVerticalStrut(2));
            leftPanel.add(messageTimePanel);

            messagePanel.add(leftPanel, BorderLayout.WEST);
        } else {
            JPanel systemPanel = new JPanel();
            systemPanel.setLayout(new BoxLayout(systemPanel, BoxLayout.X_AXIS));
            systemPanel.setOpaque(false);
            systemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            systemPanel.add(Box.createHorizontalGlue());
            JLabel systemLabel = new JLabel(message);
            systemLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            systemLabel.setForeground(Color.GRAY);
            systemPanel.add(systemLabel);
            systemPanel.add(Box.createHorizontalGlue());

            messagePanel.add(systemPanel, BorderLayout.CENTER);
        }

        SwingUtilities.invokeLater(() -> {
            chatPanel.add(messagePanel);
            chatPanel.revalidate();
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && isRunning) {
            writer.println(message);
            String time = timeFormat.format(new Date());
            addMessage(username, message, time, true);
            DatabaseManager.saveMessage(roomName, username, message, false);
            messageField.setText("");
            messageField.requestFocus();
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while (isRunning && (message = reader.readLine()) != null) {
                final String receivedMessage = message;
                SwingUtilities.invokeLater(() -> processMessage(receivedMessage));
            }
        } catch (IOException e) {
            if (isRunning) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "서버와의 연결이 끊어졌습니다.",
                            "연결 오류",
                            JOptionPane.ERROR_MESSAGE);
                    dispose();
                });
            }
        }
    }

    private void processMessage(String message) {
        if (message.startsWith("PARTICIPANTS: ")) {
            participants.clear();
            String[] parts = message.substring(13).split(",");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    participants.add(part.trim());
                }
            }
            updateParticipantsLabel();
        } else if (message.startsWith("SYSTEM: ")) {
            String systemMessage = message.substring(8);
            String time = timeFormat.format(new Date());
            addMessage("SYSTEM", systemMessage, time, false);
            DatabaseManager.saveMessage(roomName, "SYSTEM", systemMessage, true);

            // 입장/퇴장 메시지 처리
            if (systemMessage.contains("님이 입장하였습니다.")) {
                String joinedUser = systemMessage.split("님이")[0];
                addParticipant(joinedUser);
            } else if (systemMessage.contains("님이 퇴장하였습니다.")) {
                String leftUser = systemMessage.split("님이")[0];
                removeParticipant(leftUser);
            }
        } else {
            int colonIndex = message.indexOf(": ");
            if (colonIndex > -1) {
                String sender = message.substring(0, colonIndex);
                String content = message.substring(colonIndex + 2);
                if (!sender.equals(username)) {
                    String time = timeFormat.format(new Date());
                    addMessage(sender, content, time, false);
                    addParticipant(sender);
                }
            }
        }
    }
}
