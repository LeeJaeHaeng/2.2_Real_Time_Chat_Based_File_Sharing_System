package com.chat.server;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ChatRoomListFrame extends JFrame {
    private JPanel roomListPanel;
    private String username;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 9002;
    private volatile boolean isRunning = true;
    private List<ChatRoomInfo> chatRooms = new ArrayList<>();

    public ChatRoomListFrame(String username) {
        this.username = username;
        setTitle("채팅");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupGUI();
        connectToServer();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                closeConnection();
            }
        });
    }

    private void setupGUI() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400, 600));

        // 상단 패널
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 채팅방 목록 패널
        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(roomListPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(242, 242, 242));
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        // 왼쪽 - 채팅 타이틀
        JLabel titleLabel = new JLabel("채팅");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        // 오른쪽 - 채팅방 생성, 메뉴 버튼
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);

        // 채팅방 생성 버튼
        JButton newChatButton = new JButton("➕");
        newChatButton.setToolTipText("채팅방 만들기");
        styleButton(newChatButton);
        newChatButton.addActionListener(e -> showCreateRoomDialog());

        // 메뉴 버튼
        JButton menuButton = new JButton("☰");
        menuButton.setToolTipText("메뉴");
        styleButton(menuButton);
        menuButton.addActionListener(e -> {
            JPopupMenu menu = createPopupMenu();
            menu.show(menuButton, 0, menuButton.getHeight());
        });

        buttonPanel.add(newChatButton);
        buttonPanel.add(menuButton);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(2, 5, 2, 5));
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // 메뉴 아이템 스타일 설정
        Font menuFont = new Font("맑은 고딕", Font.PLAIN, 13);

        // 새로고침 메뉴
        JMenuItem refreshItem = new JMenuItem("새로고침");
        refreshItem.setFont(menuFont);
        refreshItem.addActionListener(e -> refreshRoomList());

        // 로그아웃 메뉴
        JMenuItem logoutItem = new JMenuItem("로그아웃");
        logoutItem.setFont(menuFont);
        logoutItem.addActionListener(e -> handleLogout());

        menu.add(refreshItem);
        menu.addSeparator();
        menu.add(logoutItem);

        return menu;
    }

    private void showCreateRoomDialog() {
        JDialog dialog = new JDialog(this, "새로운 채팅방", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("채팅방 이름:");
        label.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(label, gbc);

        JTextField roomNameField = new JTextField(20);
        roomNameField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(roomNameField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton createButton = new JButton("생성");
        createButton.addActionListener(e -> {
            String roomName = roomNameField.getText().trim();
            if (!roomName.isEmpty()) {
                writer.println("/create " + roomName);
                dialog.dispose();
            }
        });

        JButton cancelButton = new JButton("취소");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(createButton);
        dialog.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println(username);
            writer.println("/list");

            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "서버 연결 실패: " + e.getMessage(),
                    "연결 오류",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void closeConnection() {
        isRunning = false;
        try {
            if (writer != null) {
                writer.println("/quit");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleLogout() {
        int option = JOptionPane.showConfirmDialog(this,
                "로그아웃 하시겠습니까?",
                "로그아웃",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            closeConnection();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void refreshRoomList() {
        writer.println("/list");
        roomListPanel.removeAll();
        roomListPanel.revalidate();
        roomListPanel.repaint();
    }

    private void addChatRoom(ChatRoomInfo room) {
        JPanel roomPanel = new JPanel(new BorderLayout());
        roomPanel.setBackground(Color.WHITE);
        roomPanel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 15, 10, 15),
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230))
        ));

        // 채팅방 제목
        JLabel titleLabel = new JLabel(room.getName());
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        // 채팅방 최근 메시지
        JLabel messageLabel = new JLabel(room.getLastMessage());
        messageLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        messageLabel.setForeground(Color.GRAY);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(messageLabel);

        roomPanel.add(textPanel, BorderLayout.CENTER);

        // 시간 표시
        JLabel timeLabel = new JLabel(room.getLastMessageTime());
        timeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        timeLabel.setForeground(Color.GRAY);
        roomPanel.add(timeLabel, BorderLayout.EAST);

        // 클릭 이벤트
        roomPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                openChatRoom(room);
            }

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                roomPanel.setBackground(new Color(245, 245, 245));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                roomPanel.setBackground(Color.WHITE);
            }
        });

        roomListPanel.add(roomPanel);
        roomListPanel.revalidate();
        roomListPanel.repaint();
    }

    private void receiveMessages() {
        try {
            String message;
            while (isRunning && (message = reader.readLine()) != null) {
                if (message.startsWith("ROOMS:")) {
                    updateRoomList(message.substring(6));
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "서버와의 연결이 끊어졌습니다.",
                            "연결 오류",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                });
            }
        }
    }

    private void updateRoomList(String roomsData) {
        SwingUtilities.invokeLater(() -> {
            roomListPanel.removeAll();
            String[] rooms = roomsData.split(",");
            for (String room : rooms) {
                if (!room.trim().isEmpty()) {
                    ChatRoomInfo roomInfo = new ChatRoomInfo(room.trim());
                    addChatRoom(roomInfo);
                }
            }
        });
    }

    private void openChatRoom(ChatRoomInfo room) {
        ChatClient chatClient = new ChatClient(username, room.getName());
        chatClient.setVisible(true);
    }

    private static class ChatRoomInfo {
        private String name;
        private String lastMessage;
        private String lastMessageTime;

        public ChatRoomInfo(String name) {
            this.name = name;
            this.lastMessage = "새로운 채팅방입니다";
            this.lastMessageTime = new SimpleDateFormat("HH:mm").format(new Date());
        }

        public String getName() { return name; }
        public String getLastMessage() { return lastMessage; }
        public String getLastMessageTime() { return lastMessageTime; }
    }
}