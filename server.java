import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int PORT = 6013;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static char[][] board = new char[3][3];
    private static char currentPlayer = 'X';
    private static boolean gameInProgress = false;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Tic Tac Toe Server started on port " + PORT);
            System.out.println("Waiting for players to connect...");

            // Wait for two players to connect
            while (clients.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients.size() + 1);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
                System.out.println("Player " + clients.size() + " connected from " + clientSocket.getInetAddress());
                
                if (clients.size() == 1) {
                    clientHandler.sendMessage("Welcome Player 1 (X). Waiting for Player 2 to connect...");
                } else if (clients.size() == 2) {
                    clientHandler.sendMessage("Welcome Player 2 (O). Game starting soon...");
                    startGame();
                }
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static void startGame() {
        gameInProgress = true;
        currentPlayer = 'X';
        initializeBoard();
        broadcast("GAME_START");
        broadcastBoard();
        clients.get(0).sendMessage("YOUR_TURN"); // Player 1 (X) goes first
        clients.get(1).sendMessage("WAIT_FOR_TURN");
    }

    private static void initializeBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = '-';
            }
        }
    }

    private static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private static void broadcastBoard() {
        StringBuilder boardState = new StringBuilder("BOARD:");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardState.append(board[i][j]);
            }
        }
        broadcast(boardState.toString());
    }

    public static synchronized boolean makeMove(int playerNumber, int row, int col) {
        if (!gameInProgress || playerNumber != (currentPlayer == 'X' ? 1 : 2)) {
            return false;
        }

        if (row < 0 || row >= 3 || col < 0 || col >= 3 || board[row][col] != '-') {
            return false;
        }

        board[row][col] = currentPlayer;
        broadcastBoard();

        if (checkWin(currentPlayer)) {
            broadcast("WIN:" + currentPlayer);
            gameInProgress = false;
            return true;
        }

        if (checkDraw()) {
            broadcast("DRAW");
            gameInProgress = false;
            return true;
        }

        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
        
        // Notify players whose turn it is
        clients.get(currentPlayer == 'X' ? 0 : 1).sendMessage("YOUR_TURN");
        clients.get(currentPlayer == 'X' ? 1 : 0).sendMessage("WAIT_FOR_TURN");
        
        return true;
    }

    private static boolean checkWin(char player) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) {
                return true;
            }
        }

        // Check columns
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == player && board[1][j] == player && board[2][j] == player) {
                return true;
            }
        }

        // Check diagonals
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            return true;
        }
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            return true;
        }

        return false;
    }

    private static boolean checkDraw() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == '-') {
                    return false;
                }
            }
        }
        return true;
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private int playerNumber;

    public ClientHandler(Socket socket, int playerNumber) {
        this.clientSocket = socket;
        this.playerNumber = playerNumber;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error setting up streams: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from Player " + playerNumber + ": " + inputLine);
                
                if (inputLine.startsWith("MOVE:")) {
                    String[] parts = inputLine.split(":");
                    System.out.println(inputLine);
                    if (parts.length == 3) {
                        try {
                            int row = Integer.parseInt(parts[1]);
                            int col = Integer.parseInt(parts[2]);
                            boolean validMove = server.makeMove(playerNumber, row, col);
                            if (!validMove) {
                                sendMessage("INVALID_MOVE");
                            }
                        } catch (NumberFormatException e) {
                            sendMessage("INVALID_MOVE");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling Player " + playerNumber + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Player " + playerNumber + " disconnected.");
            } catch (IOException e) {
                System.err.println("Error closing socket for Player " + playerNumber + ": " + e.getMessage());
            }
        }
    }
}