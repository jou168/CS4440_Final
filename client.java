import java.io.*;
import java.net.*;

public class client {
    public static void main(String[] args) {
        try (
            Socket socket = new Socket("localhost", 6013);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        ) {
            System.out.println("Connected to Tic Tac Toe server. Waiting for game to start...");

            // Thread to handle incoming messages from the server
            Thread receiveThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        if (serverMessage.startsWith("BOARD:")) {
                            displayBoard(serverMessage.substring(6));
                        } else if (serverMessage.equals("YOUR_TURN")) {
                            System.out.println("\nIt's your turn! Enter your move (row and column, 0-2, separated by space):");
                        } else if (serverMessage.equals("WAIT_FOR_TURN")) {
                            System.out.println("\nWaiting for the other player's move...");
                        } else if (serverMessage.startsWith("WIN:")) {
                            char winner = serverMessage.charAt(4);
                            System.out.println("\nPlayer " + winner + " wins!");
                            displayBoard(in.readLine().substring(6));
                        } else if (serverMessage.equals("DRAW")) {
                            System.out.println("\nThe game is a draw!");
                            displayBoard(in.readLine().substring(6));
                        } else if (serverMessage.equals("INVALID_MOVE")) {
                            System.out.println("\nInvalid move! Try again.");
                        } else if (serverMessage.startsWith("Welcome")) {
                            System.out.println("\n" + serverMessage);
                        } else if (serverMessage.equals("GAME_START")) {
                            System.out.println("\nGame started!");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            receiveThread.start();

            // Main thread to handle user input
            String input;
            while ((input = userInput.readLine()) != null) {
                try {
                    String[] parts = input.split(" ");
                    if (parts.length == 2) {
                        int row = Integer.parseInt(parts[0]);
                        int col = Integer.parseInt(parts[1]);
                        if (row >= 0 && row <= 2 && col >= 0 && col <= 2) {
                            out.println("MOVE:" + row + ":" + col);
                        } else {
                            System.out.println("Please enter numbers between 0 and 2");
                        }
                    } else {
                        System.out.println("Please enter two numbers separated by space (row column)");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Please enter valid numbers");
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void displayBoard(String boardState) {
        System.out.println("\nCurrent Board:");
        System.out.println("  0 1 2");
        for (int i = 0; i < 3; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < 3; j++) {
                System.out.print(boardState.charAt(i * 3 + j) + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}