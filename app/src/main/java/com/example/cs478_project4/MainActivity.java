package com.example.cs478_project4;

import java.util.ArrayList;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final String Player1_name = "X";
    public static final String Player2_name = "O";
    public static final int draw = 0;

    private ArrayList boardPositions, availablePositions;
    private int gameResult;
    private Thread player1, player2;
    private Handler player1Handler, player2Handler;
    private TextView statusText;

    public static int ROW = 3;
    public static int COLUMN = 3;
    public static int[] BOARD_UI_POSITIONS = {R.id.textview1, R.id.textview2, R.id.textview3,
            R.id.textview4, R.id.textview5, R.id.textview6,
            R.id.textview7, R.id.textview8, R.id.textview9};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.result_text);

        if(savedInstanceState != null) {
            boardPositions = savedInstanceState.getIntegerArrayList("boardPositions");
            availablePositions = savedInstanceState.getIntegerArrayList("availablePositions");
            gameResult = savedInstanceState.getInt("gameResult");
        } else {
            // Initialize Game
            initializeGame();
        }
        Winner();
        updateDisplay();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putIntegerArrayList("boardPositions", boardPositions);
        outState.putIntegerArrayList("availablePositions", availablePositions);
        outState.putInt("gameResult", gameResult);
        super.onSaveInstanceState(outState);
    }

    //Initialize the game
    private void initializeGame() {
        boardPositions = new ArrayList();  // boardPositions: ArrayList contains value in that position. 0 : empty, 1:player1(X), 2:player2(O)
        availablePositions = new ArrayList();//availablePositions: emptypositions.
        gameResult = -1;

        for(int i = 0; i< ROW*COLUMN; i++) {
            boardPositions.add(0);
            availablePositions.add(i);
        }

        Winner();
    }

    // When Start button is clicked this method is exceuted
    public void startGame(View view) {
        mHandler.removeCallbacksAndMessages(null);
        if(player1 != null && player1.isAlive()) {
            player1Handler.removeCallbacksAndMessages(null);
            player1Handler.getLooper().quit();
        }
        if(player2 != null && player2.isAlive()) {
            player2Handler.removeCallbacksAndMessages(null);
            player2Handler.getLooper().quit();
        }

        initializeGame();

        player1 = new Player1();
        player2 = new Player2();
        player1.start();
        player2.start();

        updateDisplay();
    }


    // Updates single grid view
    private void updateCellView(int index, int value) {
        TextView cell = findViewById(BOARD_UI_POSITIONS[index]);
        updateCellView(cell, value);
    }

    // Updates entire board's view
    private void updateDisplay() {
        for(int i=0; i<ROW*COLUMN; i++) {
            int value = (int) boardPositions.get(i);
            updateCellView(i, value);
        }
    }

    //Method to find the game result
    private int getGameResult() {
        gameResult = getGameResult(boardPositions, availablePositions);
        return gameResult;
    }

    //Strategy 1 : random assignment
    private int randomMethod() {
        return randomMethod(availablePositions);
    }

    //Strategy 2 : Greedy method
    private int greedyMethod() {
        return greedyMethod(boardPositions, availablePositions);
    }

    // Publish the result of the Game in the UI
    private void Winner() {
        String message = "";
        if(gameResult == 1) {
            message = "Player 1 is the winner";
        }
        else if(gameResult == 2){
            message = "Player 2 is the winner";
        }
        else if(gameResult == draw) {
            message = "Draw! Game is tied";
        }
        statusText.setText(message);
    }


    // Handler which communicates between two threads
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            int what = msg.what;
            int index = msg.arg1;

            if(what == 1) {
                boardPositions.set(index, 1);
                availablePositions.remove(Integer.valueOf(index));
                updateCellView(index, 1);
                statusText.setText("Player 1 made a move waiting for Player2's move");

                if (getGameResult() == -1) {
                    try {
                        player1.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    player2Handler.sendMessage(player2Handler.obtainMessage());
                }
            } else if(what == 2) {
                boardPositions.set(index, 2);
                availablePositions.remove(Integer.valueOf(index));
                updateCellView(index, 2);
                statusText.setText("Player 2 made a move waiting for Player1's move");

                if (getGameResult() == -1) {
                    try {
                        player2.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }  player1Handler.sendMessage(player1Handler.obtainMessage());
                }
            }

            // If the game is compeleted, publish the winner in UI
            if(gameResult != -1) Winner();
        }
    };

    //Player1 - Thread1 implementation
    public class Player1 extends Thread {

        // random position is notified to Main thread
        private void makeMove() {
            Message msg = mHandler.obtainMessage(1);
            msg.arg1 = randomMethod();
            mHandler.sendMessage(msg);
        }

        @Override
        public void run() {
            if(gameResult != -1) return;
            Looper.prepare();
            makeMove(); // Initial move
            // Handle messages from the UI Thread
            player1Handler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    if(gameResult == -1)
                        makeMove();
                }
            };
            Looper.loop();
        }
    }

    //Player2 - Thread2 implementation
    public class Player2 extends Thread {
        // greedy position is notified to Main thread
        private void makeMove() {
            Message msg = mHandler.obtainMessage(2);
            msg.arg1 = greedyMethod();
            mHandler.sendMessage(msg);
        }

        @Override
        public void run() {
            Looper.prepare();
            // Handle messages receiving from the UI Thread
            player2Handler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    if(gameResult == -1)  makeMove();
                }
            };
            Looper.loop();
        }
    }


    Random random = new Random();

    // Game Result - Logic check for tictactoe
    public int getGameResult(ArrayList positions, ArrayList availablePositions) {
        int result = -1;
        boolean gameOver;

        // Check Horizontal Rows
        for(int i=0; i<ROW; i++) {
            gameOver = true;
            int val = (int) positions.get(i*3);
            for(int j=0; j<COLUMN && gameOver; j++) {
                if(val != (int) positions.get( (i*3)+j) ) {
                    gameOver = false;
                }
            }
            if(gameOver) return getWinnerForValue(val);
        }

        // Check Vertical Columns
        for(int i=0; i<COLUMN; i++) {
            gameOver = true;
            int val = (int) positions.get(i);
            for(int j=0; j<COLUMN && gameOver; j++) {
                if(val != (int)positions.get( i+(j*3) )) {
                    gameOver = false;
                }
            }
            if(gameOver) return getWinnerForValue(val);
        }

        // Check Diagonals
        if(ROW == COLUMN) {
            // Check TopLeft to BottomRight Diagonal
            gameOver = true;
            int topLeft = (int) positions.get(0);
            for(int i=0; i<ROW && gameOver; i++) {
                if(topLeft != (int) positions.get(i*ROW + i)) {
                    gameOver = false;
                }
            }
            if(gameOver) return getWinnerForValue(topLeft);

            // Check TopRight to BottomLeft Diagonal
            gameOver = true;
            int topRight = (int) positions.get(ROW-1);
            for(int i=0; i<ROW && gameOver; i++) {
                if(topRight != (int) positions.get((ROW-1)*(i+1)) ) {
                    gameOver = false;
                }
            }
            if(gameOver) return getWinnerForValue(topRight);
        }

        // Check for Draw
        if(availablePositions.size() == 0)
            return draw;

        return result;
    }

    // Getting gameresult value here
    private int getWinnerForValue(int gameResult) {
        switch (gameResult) {
            case 1: return 1;
            case 2: return 2;
            default: return -1;
        }
    }

    // Generates random position
    public int randomMethod(ArrayList availablePositions) {
        synchronized (random) {
            int rand = random.nextInt(availablePositions.size());
            return (int) availablePositions.get(rand);
        }
    }

    // Updates the grid with value
    public void updateCellView(TextView cell, int value) {
        if(value == 1) {
            cell.setText(Player1_name);
        }
        else if(value == 2) {
            cell.setText(Player2_name);
        }
        else if(value == 0) {
            cell.setText("");
        }
    }

    //Greedy Strategy : If there is a chance for immediate victory, we place the player there.
    public int greedyMethod(ArrayList positions, ArrayList availablePositions) {
        ArrayList<Integer> newAvailablePositions;
        int player1Result, player2Result;

        for(int i=0; i<availablePositions.size(); i++) {
            int moveIndex = (int)availablePositions.get(i);
            newAvailablePositions = (ArrayList<Integer>) availablePositions.clone();
            newAvailablePositions.remove(Integer.valueOf(moveIndex));

            // logic for player1 wins
            positions.set(moveIndex, 1);
            player1Result = getGameResult(positions, newAvailablePositions);
            // logic for player2 wins
            positions.set(moveIndex, 2);
            player2Result = getGameResult(positions, newAvailablePositions);

            // Reset the value
            positions.set(moveIndex, 0);

            // If either of player1Result or player2Result is not UNFINISHED, We play there.
            // Playing in this cell will either ensure victor or prevent Opponent to secure victory by occupy this threatening cell.
            if(player1Result != -1 || player2Result != -1) {
                return moveIndex;
            }
        }

        // If there is no immediate threat or victory opportunity, we play a random position.
        return randomMethod(availablePositions);
    }




}

