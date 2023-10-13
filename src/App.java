package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Class that displays the application window for the game
 *
 * @author Oshi Werellagama
 */
public class App {
    private int textX = 383;
    private int textW = 56;
    private int textH = 29;
    private JPanel backgroundPanel;
    private JFrame frame;
    private boolean isPaused = true;
    private boolean wasPaused = true;
    private long pausedTime = 0;
    private boolean gameStarted = false;
    private JDialog pausedMessage;
    private static Graphics graphics;
    private int recorderCount;
    private int selectedSpeed;

    private int pixels = 0;
    private String move = "unset";

    private ImageIcon backgroundImageIcon;
    private Domain domain;
    private Renderer renderer;
    private Persistency persistency;
    private Recorder recorder;

    private boolean animating = false;
    private boolean updateBackground = false;
    public boolean keyListenersEnabled = true;

    //used to diplay keys
    private int keySize = 30;
    private int keyW = 352;
    private int keyH = 248;

    //used for resetting levels
    private int level = 1;
    private int timerCount;
    private int count = 0;

    //various labels
    private JLabel timeLabel;
    private JLabel chipsLeftLabel;
    private JLabel levelLabel;

    private Timer timer;
    private Timer clock;
    private Timer recorderTimer;

    private long elapsedTime;
    private long startTime = System.currentTimeMillis();
    private int levelTime = 60;

    private List<JLabel> labelList;

    /**
     * Main method to start the game application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        App app = new App();
        app.setUpGUI();
        app.repaintGame();
    }

    /**
     * Constructs an instance of the App class.
     * Initialises game components.
     */
    public App(){
        domain = new Domain();
        recorder = new Recorder(this);
        persistency = new Persistency(this, domain);
        domain.setPersistency(persistency);
        renderer = new Renderer(domain);

        var exitState = persistency.loadExitState();
        Optional<Integer> levelOptional = persistency.loadExitState().level();
        if (levelOptional.isPresent()) {
            domain.loadLevel("" + levelOptional.get());
        } else {
            domain.loadLevel(exitState.saveName().get());
            recorder.loadFromString(exitState.saveName().get());
        }
        resetGame();
    }

    /**
     * Returns the JFrame object associated with this class. This method is used by Recorder.
     *
     * @return The JFrame object used for the GUI.
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Updates the recorder to take in the current level.
     * Initialises a new renderer.
     */
    public void resetGame(){
        recorder.setCurrentLevel(level);
        renderer = new Renderer(domain);
    }

    /**
     * This method is called, to revert the game to the start state of level 1.
     * Used when a player has timed out, wants to restart or just load level 1.
     */
    public void setLevel1(){
        isPaused = true;
        level = 1;
        recorder.setCurrentLevel(level);
        setElapsed(0);
        domain.loadLevel("1");
        resetGame();
        repaintGame();
    }

    /**
     * This method is called, to revert the game to the start state of level 1.
     * Used when a player has timed out or just wants to load level 2.
     */
    public void setLevel2(){
        isPaused = true;
        level = 2;
        recorder.setCurrentLevel(level);
        domain.loadLevel("2");
        setElapsed(0);
        resetGame();
        repaintGame();
    }

    /**
     * This method repaints the game panel and sets the various labels to the specified state.
     * It also ensures the correct time is being displayed.
     *
     */
    private void repaintGame(){
        backgroundPanel.repaint();
        gameStarted = false;
        repaintKeyInventory();
        chipsLeftLabel.setText(Integer.toString(domain.getChap().getTreasureTotal()));
        levelLabel.setText(Integer.toString(level));
        updateClock((int)(elapsedTime/1000));
        count = 0;
    }

    /**
     * Enables or disables key listeners.
     * Used by Recorder to turn off key listeners when a replay is happening.
     *
     * @param b {@code true} to enable key listeners, {@code false} to disable them.
     */

    public void setKeyEnabled(boolean b){
        this.keyListenersEnabled = b;
    }

    /**
     * Checks whether key listeners are enabled. This is a method used by Recorder.
     *
     * @return {@code true} if key listeners are enabled, {@code false} otherwise.
     */
    public boolean areKeyListenersEnabled() {
        return this.keyListenersEnabled;
    }

    /**
     * Used to unpause the game.
     */
    public void togglePaused(){
        isPaused = false;
    }

    /**
     * Used to turn various timers on. Used during replay related events.
     */
    public void turnClocksOn(){
        timer.start();
        clock.start();
        recorderTimer.start();
    }

    /**
     * Used to turn various timers off. Used during replay related events.
     */
    public void turnClocksOff(){
        timer.stop();
        clock.stop();
        recorderTimer.stop();
    }

    /**
     * Updates the time label to display the remaining time.
     *
     * @param time The time since the game has started. 
     *             This parameter is subtracted from the total level time. 
     *             This sum will be used to display the time left in a level.
     */
    public void updateClock(int time){
        timeLabel.setText(Integer.toString(levelTime - time));
    }

    /**
     * This is a method used by Recorder to update the background panel during a replay.
     *
     * @param animating A check regarding whether an animation is in progress.
     * @param move The specified move (up, down, etc.)
     * @param pixels The number of pixels moved.
     * @param updateBackground A flag indicating whether to update the background.
     */
    public void updateBackgroundPanel(boolean animating, String move, int pixels, boolean updateBackground){
        this.animating = animating;
        this.move = move;
        this.pixels = pixels;
        this.updateBackground = updateBackground;
        backgroundPanel.repaint();
    }

    /**
     * Returns the elapsed time in the game in milliseconds.
     *
     * @return The elapsed time in milliseconds.
     */    
    public long getElapsedTime(){
        return elapsedTime;
    }

    /**
     * Sets various time related variables based off a specified elapsed time.
     *
     * @param elapsedTime The new elapsed time in milliseconds.
     */
    void setElapsed(long elapsedTime) {
        this.elapsedTime = elapsedTime;
        this.pausedTime = elapsedTime;
        this.startTime = System.currentTimeMillis() - elapsedTime;
    }

    /**
     * Gets the total time available for the current game level.
     *
     * The total time for a game level is determined by the level number. 
     * Level 1 has a total time of 60 seconds.
     * Level 2 has a total time of 180 seconds.
     *
     * @return The total time available for the current game level in seconds.
     */

    public int getTotalTime(){
        return levelTime;
    }

    /**
     * Sets the total time available for the current game level.
     * @param time The time we want to set levelTime to.
     */
    void setTotalTime(int time){
        levelTime = time;
    }

    /**
     * Gets the current level of the game.
     *
     * @return The current level of the game.
     */

    public int getLevel(){
        return level;
    }

    /**
     * Sets the current level of the game.
     *
     * @param lvl The level to set as the current level.
     */
    void setLevel(int lvl){
        level = lvl;
    }

    /**
     * Gets the exact time when the game started.
     *
     * @return The start time of the game, represented as a long value (e.g., timestamp).
     */

    public long getStartTime(){
        return startTime;
    }

    /**
     * Sets up the JFrame, background image.
     *
     * Calls methods to set up buttons and labels as well as a method to check the game status.
     */
    public void setUpGUI(){
        frame = new JFrame("Chips's Challenge: LESSON 1");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(530, 410);

        // load the background image
        try {
            String currentDirectory = System.getProperty("user.dir");
            String filePath = currentDirectory.replace(File.separator + "src", "") + File.separator + "images" + File.separator + "background.png";
            ImageIO.read(new File(filePath));
            backgroundImageIcon = new ImageIcon(ImageIO.read(new File(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        backgroundPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                graphics = g;
                backgroundImageIcon.paintIcon(this, g, 0, 0);
                renderer.repaint(graphics, animating, move, pixels, updateBackground);
            }
        };

        backgroundPanel.setLayout(null);

        setUpLabels();
        checkGameStatus();
        setUpMenu();

        // add the backgroundPanel to the JFrame
        frame.add(backgroundPanel);

        setKeyFunctions();
        frame.setVisible(true);
    }

    /**
     * Initializes and starts a timer to update parts of the game at a specified rate.
     * Aspects of the game that are updated include enemies, background and the keyInventory.
     * Every time an action is triggered at the given rate, we also check if there any commands from domain and execute accordingly.
     *
     * @param updateE Flag indicating whether to update the background when the timer triggers.
     * @param rate The rate (in milliseconds) at which the timer triggers the action.
     */
    public void makeTimer(boolean updateE, int rate){
        timerCount = 0;
        timer = new Timer(rate, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(!isPaused){
                        timerCount+= 20;
                        if(timerCount % 100 == 0){
                            updateBackground = true;
                        }else{
                            updateBackground = false;
                        }
                        if(level == 2){
                            if (timerCount % 400 == 0){
                                domain.updateEnemies();
                            }
                        }
                        // if move selected and not currently animating
                        if(!move.equals("unset") && !animating){
                            // if the move is even possible
                            if(executeMove(move, false)){
                                // if move is possible start animating
                                animating = true;
                            }
                        }

                        if(animating){
                            // if pixels moved in the animation is less than the full 32 pixels needed for a full animation
                            if( pixels != 30){
                                // add 1 pixel so next clock loop it moves a extra pixel
                                pixels+=2;
                            }else{ // when reached the end of the animation and chap has moved 32 pixels
                                // activate the logic that assigns chap's position to the next tile

                                executeMove(move, true);
                                for(Domain.Command c : domain.getCommands()){
                                    switch (c.type()){
                                        case "playSound": renderer.playSound(c.argument());
                                            break;
                                        case "printInfo":
                                            JOptionPane.showMessageDialog(frame, c.argument(), "Help", JOptionPane.INFORMATION_MESSAGE);
                                            break;
                                        case "move" :
                                            recorder.storeMovesMade(c.argument(), elapsedTime, level);
                                            break;
                                    }
                                }
                                // reset values
                                pixels = 0;
                                move = "unset";
                                animating = false;
                            }
                        }

                        repaintKeyInventory();

                        backgroundPanel.repaint();
                        int chipsLeft = domain.getChap().getTreasureTotal();

                        chipsLeftLabel.setText(Integer.toString(chipsLeft));
                    }

                }
            });
        timer.start();
    }

    /**
     * This changes the keys displayed on the GUI to be accurate to the keys in the inventory in the game.
     *
     */
    public void repaintKeyInventory(){
        int inventorySize = domain.getChap().getInventory().size();

        for (int i = 0; i < labelList.size(); i++) {
            if (i < inventorySize) {
                BufferedImage image = domain.getChap().getInventory().get(i).getIcon();
                ImageIcon inventoryIcon = new ImageIcon(image);
                labelList.get(i).setIcon(inventoryIcon);
            } else {
                //clears the icon for labels with indices greater than or equal to inventorySize
                labelList.get(i).setIcon(null);
            }
        }
    }

    /**
     * Sets up the labels for level, time, chips left and inventory objects (keys).
     * These labels will eventually be modified to represent the current situation in the game.
     *
     */
    public void setUpLabels(){
        //labels that display level, time and chips left
        levelLabel = new JLabel(Integer.toString(level));
        levelLabel.setBounds(textX, 60, textW, textH);
        levelLabel.setFont(new Font("Monospaced", Font.PLAIN, 29));
        levelLabel.setForeground(Color.GREEN);
        levelLabel.setBackground(Color.BLACK);
        levelLabel.setOpaque(true);
        backgroundPanel.add(levelLabel);

        timeLabel = new JLabel(Integer.toString(levelTime));
        timeLabel.setBounds(textX, 122, textW, textH);
        timeLabel.setFont(new Font("Monospaced", Font.PLAIN, 29));
        timeLabel.setForeground(Color.GREEN);
        timeLabel.setBackground(Color.BLACK);
        timeLabel.setOpaque(true);
        backgroundPanel.add(timeLabel);

        int chipsLeft = domain.getChap().getTreasureTotal();
        chipsLeftLabel = new JLabel(Integer.toString(chipsLeft));
        chipsLeftLabel.setBounds(textX, 212, textW, textH);
        chipsLeftLabel.setFont(new Font("Monospaced", Font.PLAIN, 29));
        chipsLeftLabel.setForeground(Color.GREEN);
        chipsLeftLabel.setBackground(Color.BLACK);
        chipsLeftLabel.setOpaque(true);
        backgroundPanel.add(chipsLeftLabel);

        //below is a list that will help display the keys collected within the game
        labelList = new ArrayList<>();

        for(int i = 0; i < 4; i++){
            JLabel inventoryLabel = new JLabel();
            inventoryLabel.setBounds(keyW + ((keySize + 2) * i), keyH, keySize, keySize);
            backgroundPanel.add(inventoryLabel);
            labelList.add(inventoryLabel);
        }

        for(int i = 0; i < 4; i++){
            JLabel inventoryLabel = new JLabel();
            inventoryLabel.setBounds(keyW + ((keySize + 2) * i), keyH + ((keySize + 2) * i), keySize, keySize);
            backgroundPanel.add(inventoryLabel);
            labelList.add(inventoryLabel);
        }

    }

    /**
     * This method checks if a user has timed out of, lost or won a level.
     * It then displays the appropriate option dialogs for each case.
     * Depending on the option chosen by the user an action is performed (either loading a level or exiting the game);
     *
     */
    public void checkGameStatus(){
        makeTimer(true,20);

        count = 0;
        clock = new Timer(100, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    count = (int)(elapsedTime / 1000);
                    updateClock(count);

                    //resets the game to level 1
                    if(level == 1){
                        if (count == 60) {
                            isPaused = true;
                            String optionMessage = "Oh no ! You ran out of time. Do you want to restart level 1?\n\nPress Yes to restart level 1\nPress No to exit the game";
                            int choice = JOptionPane.showConfirmDialog(frame, optionMessage, "Time's Up!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            if (choice == JOptionPane.YES_OPTION) {
                                setLevel1();
                            }

                            if (choice == JOptionPane.NO_OPTION) {
                                doExit();
                            }
                        }

                        if(domain.getChap().getHasWon()){    
                            String optionMessage = "Nice ! You completed level 1. Do you want to level up?\n\nPress Yes to level up\nPress No to exit the game";
                            int choice = JOptionPane.showConfirmDialog(frame, optionMessage, "Level Up!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            if (choice == JOptionPane.YES_OPTION) {
                                setLevel2();
                            }

                            if (choice == JOptionPane.NO_OPTION) {
                                doExit();
                            }
                        }
                    }

                    if(level == 2){
                        if (count == 180) {
                            isPaused = true;
                            String optionMessage = "Oh no ! You ran out of time. Do you want to restart level 2?\n\nPress Yes to restart level 2\nPress No to exit the game";
                            int choice = JOptionPane.showConfirmDialog(frame, optionMessage, "Time's Up!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            if (choice == JOptionPane.YES_OPTION) {
                                setLevel2();
                            }

                            if (choice == JOptionPane.NO_OPTION) {
                                doExit();
                            }
                        }

                        if(domain.getChap().getHasWon()){    
                            String optionMessage = "Nice ! You completed level 2. Do you want to restart the game again from Level 1?\n\nPress Yes to restart from level 1\nPress No to exit the game, your progress will not be saved";
                            int choice = JOptionPane.showConfirmDialog(frame, optionMessage, "Level Up!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            if (choice == JOptionPane.YES_OPTION) {
                                setLevel1();
                            }

                            //exits the game, game will start from level 1 when reopened
                            if (choice == JOptionPane.NO_OPTION) {
                                persistency.saveExitState(1);
                                System.exit(0);
                            }
                        }
                    }

                    //if Chap loses at level 2
                    if(domain.getChap().getHasLost()){
                        isPaused = true;
                        String optionMessage = "Oh no! You lost! Do you want to restart level 2?\n\nPress Yes to restart level 2\nPress No to exit the game";

                        int choice = JOptionPane.showConfirmDialog(frame, optionMessage, "Restart Level?", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (choice == JOptionPane.YES_OPTION) {
                            setLevel2();
                        }

                        if (choice == JOptionPane.NO_OPTION) {
                            doExit();
                        }
                    }

                }
            });

        recorderTimer = new Timer(1, new ActionListener() {
                int recorderCount = 0;
                public void actionPerformed(ActionEvent e) {
                    if(!isPaused){
                        elapsedTime = System.currentTimeMillis() - startTime;
                    }
                    if(wasPaused != isPaused){
                        if(isPaused){
                            pausedTime = getElapsedTime();
                        } else {
                            setElapsed(pausedTime);
                        }
                        wasPaused = isPaused;
                    }
                }
            });

        timer.start();
        clock.start();
        recorderTimer.start();
    }

    /**
     * Sets up menu bar items.
     *
     * Has working buttons to pause, save, exit and resume the game.
     *
     * Has a button to display the game rules.
     * Has buttons to load a new level 1 or new level 2.
     */
    public  void setUpMenu(){
        // Create a menu bar
        JMenuBar menuBar = new JMenuBar();

        // Create a "File" menu
        JMenu options = new JMenu("Options");
        JMenu loadReplayMenuItem = new JMenu("Load Replay");

        JMenuItem pauseMenuItem = new JMenuItem("Pause");  
        JMenuItem saveMenuItem = new JMenuItem("Save");
        JMenuItem exitMenuItem = new JMenuItem("Exit");

        JMenuItem autoMenuItem = new JMenuItem("Auto");
        JMenuItem stepMenuItem = new JMenuItem("Step-by-step");
        JMenuItem speedMenuItem = new JMenuItem("Controlled");

        options.add(pauseMenuItem);
        options.add(saveMenuItem);
        options.add(loadReplayMenuItem);
        options.add(exitMenuItem);

        loadReplayMenuItem.add(autoMenuItem);
        loadReplayMenuItem.add(stepMenuItem);
        loadReplayMenuItem.add(speedMenuItem);

        JMenu level = new JMenu("Level");
        JMenuItem L1MenuItem = new JMenuItem("Level 1");
        JMenuItem L2MenuItem = new JMenuItem("Level 2");
        level.add(L1MenuItem);
        level.add(L2MenuItem);

        JMenu help = new JMenu("Help");
        JMenuItem gameRulesMenuItem = new JMenuItem("Rules");
        help.add(gameRulesMenuItem);

        menuBar.add(options);
        menuBar.add(level);
        menuBar.add(help);

        exitMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doExit();
                }
            });

        autoMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    turnClocksOff();
                    recorder.shutdownOps();
                    recorder.loadFromFile(frame,"auto");
                }
            });

        stepMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    turnClocksOff();
                    recorder.shutdownOps();
                    recorder.loadFromFile(frame,"step");
                }
            });

        speedMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    turnClocksOff();
                    recorder.shutdownOps();
                    recorder.loadFromFile(frame,"speed");
                    takeInput();
                }
            });

        gameRulesMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Display a dialog with game rules

                    String rules = "How to play:\n"
                        + "- You are playing as chap, press the up, down, left, and right arrow keys to navigate through the maze\n"
                        + "- Collect all the chips; there will be a counter of how many chips you have left\n"
                        + "- Collect keys to open locked doors\n"
                        + "- After you have collected all the chips, navigate to the blue exit to level up";
                    JOptionPane.showMessageDialog(frame, rules, "How to Play", JOptionPane.INFORMATION_MESSAGE);
                }
            });

        pauseMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(!isPaused){
                        runPauseDialog();
                    }
                }
            });

        saveMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recorder.shutdownOps();
                    isPaused = true;
                    String filename = recorder.saveToFile(frame);
                    persistency.saveLevel(filename, domain.getBoard());
                    persistency.saveExitState(filename);
                    System.exit(0);
                }
            });

        L1MenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recorder.shutdownOps();
                    setLevel1();
                    isPaused = true;
                }
            });

        L2MenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recorder.shutdownOps();
                    setLevel2();
                    isPaused = true;
                }
            });

        // Set the menu bar for the frame
        frame.setJMenuBar(menuBar);
    }

    /**
     * Prompts the user until they specify a number between 1 and 5.
     * This input will be used by Recorder when a player wants to do a controlled replay.
     *
     */
    public void takeInput(){
        boolean isValidInput = false;
        int inputNum = 0;

        //prompts the user until they give a valid input
        while (!isValidInput) {
            String inputValue = JOptionPane.showInputDialog(frame, "Please enter a number between 1 and 5:", "Adjust Speed", JOptionPane.PLAIN_MESSAGE);

            // check if the user pressed OK and provided a number
            if (inputValue != null && !inputValue.isEmpty()) {
                try {
                    inputNum = Integer.parseInt(inputValue);
                    if (inputNum >= 1 && inputNum <= 5) {
                        isValidInput = true;
                    } else {
                        // if the input is outside the valid range
                        JOptionPane.showMessageDialog(frame, "Please enter a number between 1 and 5.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    // if the input is not valid
                    JOptionPane.showMessageDialog(frame, "Please enter a valid number between 1 and 5.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                break;
            }
        }

        if (isValidInput) {
            selectedSpeed = inputNum;
        }

    }

    /**
     * Gets the speed specified by the user, this is used by Recorder to change the speed in the controlled replay.
     *
     * @return The currently selected speed setting.
     */

    public int getSelectedSpeed(){
        return selectedSpeed;
    }

    /**
     *
     * This checks domain to see if a specified move is valid and then returns a boolean.
     * It clarifies if the move can be executed or not.
     *
     * @param move The move to be executed. It should be a valid move action, such as "up", "down", etc.
     * @param entering Specifies whether you are entering a door.
     * @return true if the move can be execute, false otherwise.
     */

    public boolean executeMove(String move, boolean entering) {
        return domain.getChap().move(move, entering);
    }

    /**
     * Sets up key listeners to handle user input.
     * Depending on what keys are pressed, various tasks can happen.
     *
     */
    public  void setKeyFunctions(){
        frame.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    if (keyCode == KeyEvent.VK_ENTER){
                        recorder.stepToNextMove();
                    }

                    if(keyListenersEnabled){
                        if (keyCode == KeyEvent.VK_UP) {
                            if(!animating){
                                move = "up";
                            }

                            if(gameStarted == false){
                                isPaused = false;
                                gameStarted = true;
                            }
                        } else if (keyCode == KeyEvent.VK_DOWN) {
                            if(!animating){
                                move = "down";
                            }

                            if(gameStarted == false){
                                isPaused = false;
                                gameStarted = true;
                            }
                        } else if (keyCode == KeyEvent.VK_LEFT) {
                            if(!animating){
                                move = "left";
                            }

                            if(gameStarted == false){
                                isPaused = false;
                                gameStarted = true;
                            }
                        } else if (keyCode == KeyEvent.VK_RIGHT) {
                            if(!animating){
                                move = "right";
                            }

                            if(gameStarted == false){
                                isPaused = false;
                                gameStarted = true;
                            }
                        } else if (keyCode == KeyEvent.VK_SPACE){
                            if(!isPaused){
                                runPauseDialog();
                            }
                        } else if (keyCode == KeyEvent.VK_ESCAPE){
                            if(isPaused){
                                isPaused = false;
                            }
                        }
                        else if (e.isControlDown() && keyCode == KeyEvent.VK_X){
                            doExit();
                            //exit the game, the current game state will be lost,
                            //the next time the game is started,
                            //it will resume from the last unfinished level
                        } else if (e.isControlDown() && keyCode == KeyEvent.VK_S){
                            isPaused = true;
                            String filename = recorder.saveToFile(frame);
                            persistency.saveLevel(filename, domain.getBoard());
                            persistency.saveExitState(filename);
                            System.exit(0);
                            //exit the game,
                            //saves the game state,
                            //game will resume next time the application will be started
                        } else if (e.isControlDown() && keyCode == KeyEvent.VK_R){

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
                                if (selectedFile != null) {
                                    String filenameWithExtension = selectedFile.getName(); // Get the file name with extension
                                    String filename = filenameWithExtension.substring(0, filenameWithExtension.lastIndexOf('.'));

                                    domain.loadLevel(filename);
                                    recorder.loadFromString(filename);
                                    resetGame();
                                    repaintGame();
                                }
                            }
                            //resume a saved game
                            //this will pop up a file selector 
                            //to select a saved game to be loaded
                        } else if (e.isControlDown() && keyCode == KeyEvent.VK_1){
                            setLevel1();
                        } else if (e.isControlDown() && keyCode == KeyEvent.VK_2){
                            setLevel2();
                        }
                    }
                }
            });

    }

    /**
     * Saves the level we are on prior to exit to persistency.
     * Exits the game.
     * The next time the game is started we can extract the level number from persistency.
     */
    public void doExit(){
        persistency.saveExitState(level);
        System.exit(0);
    }

    /**
     * Displays a dialog when the game is paused.
     * The user is prompted to press the ESC key to unpause the game.
     */
    public void runPauseDialog(){
        isPaused = true;

        //informs player that game is paused
        pausedMessage = new JDialog(frame, "Pause", true);
        JLabel label = new JLabel("Game is paused. Press escape to resume game.");
        pausedMessage.add(label);
        pausedMessage.setSize(300, 100);
        pausedMessage.setLocationRelativeTo(frame);

        //when escape is pressed dialog closes and game unpauses
        pausedMessage.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        pausedMessage.dispose();
                        isPaused = false;
                    }
                }
            });

        pausedMessage.setVisible(true);
    }
}
