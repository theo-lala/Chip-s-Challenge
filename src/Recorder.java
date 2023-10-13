package src;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * The Recorder class is responsible for recording and replaying player movements in a game of Chap's Challenge.
 * It provides methods for capturing player moves, storing them, and replaying them, either automatically,
 * step by step, or at a user-defined speed.
 * 
 * This class also offers options for saving recorded moves to a JSON file and loading them from either a
 * file or a string. The Recorder interacts with App to execute and visualize moves and Persistency for the CTRL-R functionalities.
 * 
 * @author Brodie Banfield
 */
public class Recorder {

    // List to store recorded player moves
    private List<Move> storedMoves;

    // The current move index during replay
    private int moveNum;

    // Counter for keeping track of time during auto-replay
    private long count;

    // Playback speed in milliseconds
    private int playbackSpeed;

    // Start time for replay
    private long startTime;

    // Flag to enable or disable key listeners
    private boolean keyListenersEnabled = true;

    // The current game level
    private int level;

    // Reference to the game application (App)
    private App app;

    // Timer for managing elapsed time during replay
    private Timer timer;

    // Elapsed time during replay
    private long elapsedTime;

    // Current move being replayed
    private Move currentMove;

    // Clock time during replay
    private int clockTime;

    // Counter for slowed-down time during speed adjustment
    private int slowedDownTime;

    // Flag indicating step-by-step replay mode
    private boolean stepByStepMode;

    // Extra time during replay
    private double extraTime;

    // Playback speed (0: slowest, 5: fastest)
    public int speed;

    // Flag to indicate the start of replay
    private boolean start;

    // Time difference during replay
    private int clockDifference;

    // Flag to indicate level switching during replay
    private boolean switching;

    // SwingWorker for updating the game clock
    SwingWorker<Void, Void> clockUpdater;

    // SwingWorker for auto-replay
    SwingWorker<Void, Void> replayWorker;

    // SwingWorker for speed adjustment
    SwingWorker<Void, Void> fasterTime;

    /**
     * Shuts down the operations related to this Recorder. This is the clock and the player.
     */
    public void shutdownOps(){
        if(clockUpdater!=null){
            clockUpdater.cancel(true);
        }
        if(replayWorker!=null){ 
            replayWorker.cancel(true);
        }
    }

    /**
     * Represents a recorded player move with direction, timestamp, and level information.
     */
    public class Move {
        private String direction;
        private long timestamp;
        private int level;

        public Move(String direction, long timestamp, int level) {
            this.direction = direction;
            this.timestamp = timestamp;
            this.level = level;
        }

        /**
         * Get the direction of the move.
         *
         * @return The direction of the move.
         */
        public String getDirection() {
            return direction;
        }

        /**
         * Get the timestamp of the move.
         *
         * @return The timestamp of the move.
         */
        public long getTime() {
            return timestamp;
        }

        /**
         * Get the level associated with the move.
         *
         * @return The level of the move.
         */
        public int getLevel(){
            return level;
        }

    }

    /**
     * Getter method for returning stored moves.
     */
    public List<Move> getStoredMoves() {
        return storedMoves;
    }

    /**
     * Create a new Recorder instance for recording player moves during a game.
     *
     */
    public Recorder(App app) {
        this.storedMoves = new ArrayList<>();
        this.moveNum = 0;
        this.count = 0;
        this.playbackSpeed = 250;
        this.app = app;
    }

    /**
     * Set the playback speed for replaying recorded game moves.
     *
     * @param speedInMillis The playback speed in milliseconds.
     */
    public void setPlaybackSpeed(int speedInMillis) {
        this.playbackSpeed = speedInMillis;
    }

    /**
     * Store player moves made during the game.
     *
     * @param direction   The direction of the move.
     * @param elapsedTime  The time elapsed during the move.
     */
    public void storeMovesMade(String direction, long elapsedTime, int level) {
        Move move = new Move(direction, elapsedTime, level);
        storedMoves.add(move);
    }

    /**
     * Automatically replay the recorded game moves.
     */
    public void autoReplayGame() {
        app.keyListenersEnabled = false;
        moveNum = 0;
        app.resetGame();
        startTime = app.getStartTime();
        elapsedTime = System.currentTimeMillis() - startTime;
        clockTime = 0;

        clockUpdater = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                while (!isCancelled()) {
                    app.updateClock(clockTime);
                    clockTime++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                return null;
            }
        };

        replayWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                while (moveNum < storedMoves.size() - 1 && !isCancelled()) {
                    currentMove = storedMoves.get(moveNum);

                    if (moveNum == 0) {
                        startTime = startTime - currentMove.getTime();
                    }

                    //System.out.println("system time = " + System.currentTimeMillis());

                    elapsedTime = System.currentTimeMillis() - startTime;

                    app.updateBackgroundPanel(false, "unset", 0, false);

                    while (elapsedTime >= currentMove.getTime() && !isCancelled()) {
                        if (app.executeMove(currentMove.getDirection(), false)) {
                            for (int i = 0; i < 30; i += 2) {
                                app.updateBackgroundPanel(true, currentMove.getDirection(), i, false);
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException e) {
                                }
                            }

                        }
                        // Check if the level has changed from 1 to 2
                        if (currentMove.getLevel() == 2 && switching == false) {
                            // Reset the game to level 2 here
                            switching = true;
                            app.setLevel2();
                            app.turnClocksOff();
                            app.keyListenersEnabled = false;

                        }

                        app.executeMove(currentMove.getDirection(), true);
                        app.updateBackgroundPanel(false, currentMove.getDirection(), 0, false);

                        elapsedTime = System.currentTimeMillis() - startTime;
                        moveNum++;

                        if (moveNum < storedMoves.size()) {
                            currentMove = storedMoves.get(moveNum);
                        }
                    }
                }
                // The replay is finished; show the option message
                replayOverMessage();
                clockUpdater.cancel(true);

                return null;
            }

            @Override
            protected void done() {
                app.keyListenersEnabled = true;
                app.turnClocksOn();
            }
        };
        replayWorker.execute();
        clockUpdater.execute();
    }

    /**
     * Step to the next recorded move in the game, for stepByStepReplay
     */
    public void stepToNextMove() {
        stepByStepMode = true; // Set the flag to indicate that the user wants to step to the next move
    }

    /**
     * Replay the game in step-by-step mode, clicking enter key to step to next move
     */
    public void stepByStepReplay() {
        app.keyListenersEnabled = false;
        moveNum = 0;
        app.resetGame();
        startTime = app.getStartTime();
        elapsedTime = System.currentTimeMillis() - startTime;

        replayWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                while (moveNum < storedMoves.size() - 1 && !isCancelled()) {

                    // Check if the stepByStepMode flag is true
                    if (stepByStepMode) {
                        currentMove = storedMoves.get(moveNum);
                        if (moveNum <= storedMoves.size()) {
                            // Get the direction from the currentMove

                            String direction = currentMove.getDirection();
                            //System.out.println("direction is " + direction);
                            // Execute the move manually when the user clicks the "Step to next move" button
                            executeMove(direction); // Pass the direction to executeMove
                            moveNum++;
                            stepByStepMode = false; // Reset the flag to pause execution
                        }
                        // Check if the level has changed from 1 to 2
                        if (currentMove.getLevel() == 2 && switching == false) {
                            // Reset the game to level 2 here
                            switching = true;
                            app.setLevel2();
                            app.turnClocksOff();
                            app.keyListenersEnabled = false;

                        }
                    }

                    try {
                        Thread.sleep(100); // Sleep to avoid high CPU usage
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // The replay is finished; show the option message
                replayOverMessage();
                return null;
            }

            @Override
            protected void done() {
                // Cleanup
                app.keyListenersEnabled = true;
                app.turnClocksOn();
            }
        };
        replayWorker.execute();
    }

    /**
     * Execute a player move, which includes animation and updating the background panel.
     *
     * @param direction The direction of the move.
     */
    public void executeMove(String direction) {
        if (app.executeMove(direction, false)) {
            for (int i = 0; i < 30; i += 2) {
                app.updateBackgroundPanel(true, direction, i, false);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        app.executeMove(direction, true);
        app.updateBackgroundPanel(false, direction, 0, false);
    }

    /**
     * Set the current level of the game.
     *
     * @param level The new level to set.
     */
    public void setCurrentLevel(int level) { 
        this.level = level;
    }

    /**
     * Change the playback speed of an auto replay.
     */
    public void changeSpeed() {
        app.keyListenersEnabled = false;
        moveNum = 0;
        app.resetGame();
        //startTime = System.currentTimeMillis();
        //elapsedTime = System.currentTimeMillis() - startTime;
        clockTime = 0;

        fasterTime = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                while (!isCancelled()) {
                    //System.out.println("selected speed = " + app.getSelectedSpeed());
                    slowedDownTime += app.getSelectedSpeed();
                    app.updateClock(slowedDownTime / 1000);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                    }
                }
                return null;
            }
        };

        replayWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                long startTime = 0;
                while (moveNum < storedMoves.size() - 1 && !isCancelled()) {
                    currentMove = storedMoves.get(moveNum);

                    if (moveNum == 0) {
                        startTime = currentMove.getTime(); 
                    }

                    elapsedTime = slowedDownTime + startTime -1;

                    app.updateBackgroundPanel(false, "unset", 0, false);
                    //System.out.println("Current move time = " + currentMove.getTime());
                    //System.out.println("Current elapsed time = " + elapsedTime);
                    while (elapsedTime >= currentMove.getTime() && !isCancelled()) {
                        //System.out.println("FIRST WHILE LOOP WORKING");
                        if (app.executeMove(currentMove.getDirection(), false)) {
                            //System.out.println("FIRST FOR LOOP WORKING");
                            for (int i = 0; i < 30; i += 2) {
                                app.updateBackgroundPanel(true, currentMove.getDirection(), i, false);
                                try {
                                    Thread.sleep(20 / (1 - app.getSelectedSpeed()/5));
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                        // Check if the level has changed from 1 to 2
                        if (currentMove.getLevel() == 2 && switching == false) {
                            // Reset the game to level 2 here
                            switching = true;
                            app.setLevel2();
                            app.turnClocksOff();
                            app.keyListenersEnabled = false;

                        }

                        app.executeMove(currentMove.getDirection(), true);
                        app.updateBackgroundPanel(false, currentMove.getDirection(), 0, false);

                        elapsedTime = slowedDownTime;
                        moveNum++;

                        if (moveNum < storedMoves.size()) {
                            currentMove = storedMoves.get(moveNum);
                        }
                    }

                }
                // The replay is finished; show the option message
                replayOverMessage();
                clockUpdater.cancel(true);
                return null;
            }

            @Override
            protected void done() {
                app.keyListenersEnabled = true;
                app.turnClocksOn();
            }
        };

        fasterTime.execute();
        replayWorker.execute();
    }

    /**
     * When replay has finished, display pop up to exit game
     */
    private void replayOverMessage() {
        String optionMessage = "The replay is over. Chap's Challenge will now be exited";
        JOptionPane.showMessageDialog(app.getFrame(), optionMessage, "Replay's Up!", JOptionPane.INFORMATION_MESSAGE);

        // Handle the action directly; no user choice is needed
        app.doExit(); 
    }

    /**
     * Getter for the current level.
     * 
     * @return The current level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Setter for the current level.
     * 
     * @param level The new level to set.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Save recorded moves to a JSON file.
     */
    public String saveToFile(JFrame frame) {
        // Determine the current working directory of the game
        String gameDirectory = System.getProperty("user.dir");

        // Create a "recorderfiles" directory if it doesn't exist
        File recorderFilesDir = new File(gameDirectory, "recorderfiles");
        if (!recorderFilesDir.exists()) {
            recorderFilesDir.mkdirs(); // Create the directory and its parent directories if necessary
        }

        String selectedFilename = JOptionPane.showInputDialog(frame, "Enter a filename:");

        // Ensure the selected filename is not null
        if (selectedFilename != null) {
            if (!selectedFilename.endsWith(".json")) {
                selectedFilename += ".json";
            }

            // Construct the full path by combining the directory path and selected filename
            String fullPath = recorderFilesDir.getAbsolutePath() + File.separator + selectedFilename;

            JSONArray movesArray = new JSONArray(); // Create a JSON array to hold recorded moves
            for (Move move : storedMoves) {
                JSONObject moveObject = new JSONObject(); // Create a JSON object for each move
                moveObject.put("direction", move.getDirection()); // Add the move's direction to the JSON object
                moveObject.put("timestamp", move.getTime()); // Add the move's timestamp to the JSON object
                moveObject.put("level", move.getLevel());
                movesArray.put(moveObject); // Add the JSON object to the array
            }

            try (FileWriter fileWriter = new FileWriter(fullPath)) {
                fileWriter.write(movesArray.toString()); // Write the JSON array to the file as a string
            } catch (IOException e) {
                e.printStackTrace(); // Handle any potential IO errors
            }

            // Return the selected filename with the ".json" extension removed
            return selectedFilename.replace(".json", "");
        } else {
            // Handle the case when selectedFilename is null
            return "You must specify a filename for the saved file"; 
        }
    }

    /**
     * Load recorded moves from a JSON file in the same directory as the saved files.
     */
    public String loadFromFile(JFrame frame,String type) {
        String gameDirectory = System.getProperty("user.dir");
        File recorderFilesDir = new File(gameDirectory, "recorderfiles");

        JFileChooser fileChooser = new JFileChooser(recorderFilesDir);
        fileChooser.setDialogTitle("Load Recorded Moves"); // Set the dialog title

        // Filter to allow only JSON files
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON Files (*.json)", "json");
        fileChooser.setFileFilter(jsonFilter);

        int userChoice = fileChooser.showOpenDialog(frame);

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                FileReader fileReader = new FileReader(selectedFile); // Create a file reader for the selected JSON file
                BufferedReader bufferedReader = new BufferedReader(fileReader); // Create a buffered reader for efficient reading
                StringBuilder jsonText = new StringBuilder(); // Create a string builder to store the JSON content
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    jsonText.append(line); // Read each line of the JSON file and append it to the string builder
                }
                JSONArray movesArray = new JSONArray(jsonText.toString()); // Create a JSON array from the loaded JSON content

                // Clear existing moves before loading
                storedMoves.clear();

                // Iterate through the JSON array and populate the storedMoves list
                for (int i = 0; i < movesArray.length(); i++) {
                    JSONObject moveObject = movesArray.getJSONObject(i); // Get each JSON object representing a move
                    String direction = moveObject.getString("direction"); // Retrieve the direction from the JSON object
                    long timestamp = moveObject.getLong("timestamp"); // Retrieve the timestamp from the JSON object
                    int level = moveObject.getInt("level");
                    storedMoves.add(new Move(direction, timestamp, level)); // Create a new Move object and add it to the list
                }

                // Set the game to level 1 before starting the replay
                app.setLevel1();

                switch (type){
                    case "auto" : autoReplayGame();
                        break;
                    case "step" : stepByStepReplay();
                        break;
                    case "speed" : changeSpeed();
                        break;
                }

            } catch (IOException e) {
                e.printStackTrace(); // Handle any potential IO errors
            }
            return selectedFile.toString().replace(".json", "");
        }
        return null;
    }

    /**
     * Load recorded moves from a String in app.
     */
    public String loadFromString(String fileName) {
        String gameDirectory = System.getProperty("user.dir");
        String recorderFilesDir = gameDirectory + File.separator + "recorderfiles";
        String filePath = recorderFilesDir + File.separator + fileName + ".json";

        try {
            File jsonFile = new File(filePath);

            if (jsonFile.exists() && jsonFile.isFile()) {
                FileReader fileReader = new FileReader(jsonFile); // Create a file reader for the specified JSON file
                BufferedReader bufferedReader = new BufferedReader(fileReader); // Create a buffered reader for efficient reading
                StringBuilder jsonText = new StringBuilder(); // Create a string builder to store the JSON content
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    jsonText.append(line); // Read each line of the JSON file and append it to the string builder
                }

                JSONArray movesArray = new JSONArray(jsonText.toString()); // Create a JSON array from the loaded JSON content

                // Clear existing moves before loading
                storedMoves.clear();

                // Iterate through the JSON array and populate the storedMoves list
                for (int i = 0; i < movesArray.length(); i++) {
                    JSONObject moveObject = movesArray.getJSONObject(i); // Get each JSON object representing a move
                    String direction = moveObject.getString("direction"); // Retrieve the direction from the JSON object
                    long timestamp = moveObject.getLong("timestamp"); // Retrieve the timestamp from the JSON object
                    int level = moveObject.getInt("level");
                    storedMoves.add(new Move(direction, timestamp, level)); // Create a new Move object and add it to the list
                }

            }
        } catch (IOException | JSONException e) {
            e.printStackTrace(); // Handle any potential IO or JSON parsing errors
        }
        // return the filepath for persistency to use for CTRL-R
        return filePath.replace(".json", "");
    }

}
