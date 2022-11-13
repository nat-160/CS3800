import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
public class Connect4{
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        char[][] board = new char[7][6];
        for(int i=0;i<board.length;i++)
            for(int j=0;j<board[i].length;j++)
                board[i][j] = ' ';
        try (var socket = new Socket(args[0], 58901)) {
            var scanner = new Scanner(System.in);
            var in = new Scanner(socket.getInputStream());
            var out = new PrintWriter(socket.getOutputStream(), true);
            var response = in.nextLine();
            var mark = response.charAt(8);
            var opponentMark = mark == 'X' ? 'O' : 'X';
            System.out.println("Connect 4: Player "+mark);
            int move = -1;
            while (in.hasNextLine()) {
                response = in.nextLine();
                if (response.startsWith("VALID_MOVE")) {
                    moveBoard(board, mark, move);
                    printBoard(board);
                    System.out.println("Waiting for opponent");
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    var loc = Integer.parseInt(response.substring(15));
                    moveBoard(board, opponentMark, loc);
                    printBoard(board);
                    System.out.print("Enter move: ");
                    move = scanner.nextInt();
                    out.println("MOVE " + move);
                } else if (response.startsWith("MESSAGE")) {
                    System.out.println(response.substring(8));
                    if(response.substring(8).equals("Your move")){
                        printBoard(board);
                        System.out.print("Enter move: ");
                        move = scanner.nextInt();
                        out.println("MOVE " + move);
                    }
                } else if (response.startsWith("VICTORY")) {
                    System.out.println("You win!");
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    System.out.println("You lost.");
                    break;
                } else if (response.startsWith("TIE")) {
                    System.out.println("You tied...");
                    break;
                } else if (response.startsWith("OTHER_PLAYER_LEFT")) {
                    System.out.println("*Other player disconnected*");
                    break;
                }
            }
        }
    }

    private static void printBoard(char[][] board){
        String output = "";
        for(int i=0;i<board[0].length;i++){
            String s = "";
            for(int j=0;j<board.length;j++){
                s += board[j][i] + " ";
            }
            output = s + "\n" + output;
        }
        System.out.print(output);
        System.out.println("0 1 2 3 4 5 6");
    }

    private static boolean moveBoard(char[][] board, char player, int move){
        int i = 0;
        while(board[move][i] != ' ')
            i++;
        if(i<6)
            board[move][i] = player;
        else
            return false;
        return true;
    }
}
