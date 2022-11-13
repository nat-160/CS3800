import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * Modified TicTacToe server for Connect4. Custom protocol is now C4P (Connect 4 Protocol)
 *
 * Client -> Server MOVE <n> QUIT
 *
 * Server -> Client WELCOME <char> VALID_MOVE OTHER_PLAYER_MOVED <n>
 * OTHER_PLAYER_LEFT VICTORY DEFEAT TIE MESSAGE <text>
 */
public class Connect4Server {

    public static void main(String[] args) throws Exception {
        try (var listener = new ServerSocket(58901)) {
            System.out.println("Tic Tac Toe Server is Running...");
            var pool = Executors.newFixedThreadPool(200);
            while (true) {
                Game game = new Game();
                pool.execute(game.new Player(listener.accept(), 'X'));
                pool.execute(game.new Player(listener.accept(), 'O'));
            }
        }
    }
}

class Game {

    // Board cells numbered 0-8, top to bottom, left to right; null if empty
    private Player[][] board = new Player[7][6];
    // board is "sideways", board[i][j] irl columns are i rows are j

    Player currentPlayer;

    private int topCell(int location){
        for(int i=0;i<6;i++){
            if(board[location][i]==null){
                return i;
            }
        }
        return -1;
    }

    public boolean hasWinner(){
        //check for 4 across
        for(int row = 0; row<board.length; row++){
            for (int col = 0;col < board[0].length - 3;col++){
                if (board[row][col] == board[row][col+1] && 
                board[row][col+2] == board[row][col+3] &&
                board[row][col+1] == board[row][col+2] &&
                board[row][col] != null){
                    return true;
                }
            }			
        }
        //check for 4 up and down
        for(int row = 0; row < board.length - 3; row++){
            for(int col = 0; col < board[0].length; col++){
                if (board[row][col] == board[row+1][col] && 
                board[row+2][col] == board[row+3][col] &&
                board[row+1][col] == board[row+2][col] &&
                board[row][col] != null){
                    return true;
                }
            }
        }
        //check upward diagonal
        for(int row = 3; row < board.length; row++){
            for(int col = 0; col < board[0].length - 3; col++){
                if (board[row][col] == board[row-1][col+1]   && 
                board[row-2][col+2] == board[row-3][col+3] &&
                board[row-2][col+2] == board[row-1][col+1] &&
                board[row][col] != null){
                    return true;
                }
            }
        }
        //check downward diagonal
        for(int row = 0; row < board.length - 3; row++){
            for(int col = 0; col < board[0].length - 3; col++){
                if (board[row][col] == board[row+1][col+1]   && 
                board[row+2][col+2] == board[row+3][col+3] &&
                board[row+2][col+2] == board[row+1][col+1] &&
                board[row][col] != null){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean boardFilledUp() {
        for(Player[] column:board)
            for(Player space: column)
                if(space==null)
                    return false;
        return true;
    }

    public synchronized void move(int location, Player player) {
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn");
        } else if (player.opponent == null) {
            throw new IllegalStateException("You don't have an opponent yet");
        } else if (topCell(location)== -1) {
            throw new IllegalStateException("Column already occupied");
        }
        board[location][topCell(location)] = currentPlayer;
        currentPlayer = currentPlayer.opponent;
    }

    /**
     * A Player is identified by a character mark which is either 'X' or 'O'. For
     * communication with the client the player has a socket and associated Scanner
     * and PrintWriter.
     */
    class Player implements Runnable {
        char mark;
        Player opponent;
        Socket socket;
        Scanner input;
        PrintWriter output;

        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
        }

        @Override
        public void run() {
            try {
                setup();
                processCommands();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (opponent != null && opponent.output != null) {
                    opponent.output.println("OTHER_PLAYER_LEFT");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        private void setup() throws IOException {
            input = new Scanner(socket.getInputStream());
            output = new PrintWriter(socket.getOutputStream(), true);
            output.println("WELCOME " + mark);
            if (mark == 'X') {
                currentPlayer = this;
                output.println("MESSAGE Waiting for opponent to connect");
            } else {
                opponent = currentPlayer;
                opponent.opponent = this;
                opponent.output.println("MESSAGE Your move");
            }
        }

        private void processCommands() {
            while (input.hasNextLine()) {
                var command = input.nextLine();
                if (command.startsWith("QUIT")) {
                    return;
                } else if (command.startsWith("MOVE")) {
                    processMoveCommand(Integer.parseInt(command.substring(5)));
                }
            }
        }

        private void processMoveCommand(int location) {
            try {
                move(location, this);
                output.println("VALID_MOVE");
                opponent.output.println("OPPONENT_MOVED " + location);
                if (hasWinner()) {
                    output.println("VICTORY");
                    opponent.output.println("DEFEAT");
                } else if (boardFilledUp()) {
                    output.println("TIE");
                    opponent.output.println("TIE");
                }
            } catch (IllegalStateException e) {
                output.println("MESSAGE " + e.getMessage());
            }
        }
    }
}