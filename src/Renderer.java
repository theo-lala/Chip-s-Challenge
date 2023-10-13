package src;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;



/**
 * Class mainly to draw and update stuff
 * 
 * @author lalatheo
 * @version 9/22/2023
 */
public class Renderer{
    private final int TOPLEFTCORNER = 32;
    private final int TILESIZE = 32;
    private final int HOWMANYTILES = 9; //9x9

    private Domain domain;
    private Domain.ChapTile theChap;
    private ArrayList<ArrayList<Domain.Tile>> board;
    private BufferedImage spritesheet;
    private HashMap<String, BufferedImage> chapImages;

    
    
    private Clip backgroundClip;
    private Clip backgroundClip2;
    private Clip loseClip;
    private Clip keyClip;
    private Clip doorClip;
    private Clip beepClip;
    private Clip winClip;
    private Clip infoClip;

    /**
     * Constructor, set up of the chapImages, board, and audio
     */
    public Renderer(Domain domain) {
        this.domain = domain;
        this.board = domain.getBoard();
        this.chapImages = new HashMap<>();
        this.theChap = domain.getChap();
        
        initializeImages();
        initializeAudio();
    }

    /**
     * method for the renderer logic, stuff like maze, character, objects creation will be here
     * 
     * @param g 
     */
    public void repaint(Graphics g, boolean animating, String move, int pixel, boolean updateBackground) {
        if(updateBackground){
            backgroundAnimationUpdate();
        }
        
        //its 9 for how many tile fit in the screen
        BufferedImage mask = new BufferedImage(board.get(0).size()*32, board.size()*32, BufferedImage.TYPE_INT_ARGB);
        Graphics maskGraphics = mask.getGraphics();
        
        for (int i = 0; i < board.get(0).size(); i++) {
            for (int j = 0; j < board.size(); j++) {
                maskGraphics.drawImage(board.get(i).get(j).getIcon()
                , i * 32
                , j * 32, null);
            }
        }
        
         int calcPixelX = pixel;
        int calcPixelY = pixel;
        
         switch(move){
            case "left" :
                 calcPixelX = (calcPixelX*-1);
                 calcPixelY = 0;
                 
             case "right" :
                 calcPixelY = 0;
                 
                break;
            case "up" : 
                calcPixelY = (calcPixelY*-1);
                calcPixelX = 0;
                 break;
            case "down" :
                calcPixelX = 0;
                break;
         }
        
         int drawingX = ((theChap.getChapCoordinates().x()-4)*32)+calcPixelX;
         int drawingY = ((theChap.getChapCoordinates().y()-4)*32)+calcPixelY;

         drawingX = Math.max(0, Math.min(drawingX, mask.getWidth() - 32 * 9));
         drawingY = Math.max(0, Math.min(drawingY, mask.getHeight() - 32 * 9));
        
         BufferedImage crop = mask.getSubimage(drawingX, drawingY, TILESIZE * HOWMANYTILES, TILESIZE * HOWMANYTILES);
        
         g.drawImage(crop,TOPLEFTCORNER,TOPLEFTCORNER,null);
        
        if(animating){
            animate(g, move, 0);
        }
    }

    /**
     * method to handle the background animation
     */
    public void backgroundAnimationUpdate() {
        // Loop through the board and find the ExitTile
        for (int i = 0; i < board.size() ; i++) {
            for (int j = 0; j < board.get(0).size() ; j++) {
                Domain.Tile tile = board.get(i).get(j);
                
                // Check if the tile is an ExitTile
                if (tile instanceof Domain.ExitTile) {
                    Domain.ExitTile exitTile = (Domain.ExitTile) tile;
                    
                    // Get the current icon index and update it
                    int iconIndex = exitTile.getIconIndex();
                    switch (iconIndex) {
                        case 0:
                            exitTile.setIcon(1);
                            break;
                        case 1:
                            exitTile.setIcon(2);
                            break;
                        case 2:
                            exitTile.setIcon(0);
                            break;
                    }
                    return;
                }
            }
        }
    }

    /**
     * method to control the animation, to animate
     * 
     * @param g 
     * @param move this is the move direction 'up', 'left', 'right', 'down'
     * @param pixel this is would be the direction for the 
     */
    private void animate(Graphics g, String move, int pixel){
        
        int x = 4 * 32 + TOPLEFTCORNER;
        int y = 4 * 32 + TOPLEFTCORNER;

        switch(move){
            case "left" :
                g.drawImage(getChapImage(move),x - pixel, y , null);
                break;
            case "right" : 
                g.drawImage(getChapImage(move),x + pixel, y, null);
                break;
            case "up" : 
                g.drawImage(getChapImage(move),x , y - pixel , null);
                break;
            case "down" : 
                g.drawImage(getChapImage(move),x , y + pixel , null);
                break;
            default :
                throw new IllegalArgumentException("Did not give move");
        }

    }

    
 
    /**
     * Make the background of an image transparent
     * 
     * @param image The image to process
     * @return  The image with a transparent background
     */
    private BufferedImage makeTransparent(BufferedImage imageWhite, BufferedImage imageBlack) {
        int width = imageWhite.getWidth();
        int height = imageWhite.getHeight();
        
        if(width != 32 || height != 32){
            throw new Error("Image's width or height is not 32");
        }

        BufferedImage transparentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgbWhite = imageWhite.getRGB(x, y);
                int rgbBlack = imageBlack.getRGB(x, y);
                // Check if the pixel color is white (you may need to adjust the tolerance)
                if (isWhite(rgbWhite) && isBlack(rgbBlack)) {
                    transparentImage.setRGB(x, y, 0x00FFFFFF); // Transparent white (ARGB)
                } else {
                    transparentImage.setRGB(x, y, rgbWhite); // Keep the original pixel color
                }
            }
        }
        return transparentImage;
    }
    
    
    private boolean isWhite(int rgb) {
        Color color = new Color(rgb);
        return (color.getGreen() == 255 && color.getRed() == 255 && color.getBlue() == 255);
    }

    private boolean isBlack(int rgb) {
        Color color = new Color(rgb);
        return (color.getGreen() == 0 && color.getRed() == 0 && color.getBlue() == 0);
    }

    public void playSound(String sound){
        switch (sound){
            case "info":
                playInfoSFX();
                break;
            case "background":
                playBackgroundMusic();
                break;
            case "background2":
                playBackgroundMusic2();
                break;
            case "key":
                playKeySFX();
                break;
            case "unlockedDoor":
                playDoorSFX();
                break;
            case "win":
                playWinSFX();
                break;
            case "loss":
                playLoseSFX();
                break;
            case "treasure":
                playTreasureSFX();
                break;            
        }
    }

    public void stopSound(String sound){

    }

    /**
     * methods to play and stop music and SFX
     */
    public void playBackgroundMusic(){
    	if(backgroundClip!=null) {
    		backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
    	}else {
            System.err.println("backgroundClip is null. Cannot start background music.");
    	}    }

    public void playBackgroundMusic2(){
    	if(backgroundClip2!=null) {
    		backgroundClip2.loop(Clip.LOOP_CONTINUOUSLY);
    	}else {
            System.err.println("backgroundClip2 is null. Cannot start background music.");
    	}
    }

    public void playKeySFX(){
        keyClip.setFramePosition(0);
        keyClip.start();
    }

    public void playDoorSFX(){
        doorClip.setFramePosition(0);
        doorClip.start();
    }

    public void playTreasureSFX(){
        beepClip.setFramePosition(0);
        beepClip.start();
    }

    public void playLoseSFX(){
        loseClip.setFramePosition(0);
        loseClip.start();
    }

    public void playWinSFX(){
        winClip.setFramePosition(0);
        winClip.start();
    }

    public void playInfoSFX(){
        infoClip.setFramePosition(0);
        infoClip.start();
    }

    //methods for 
    public void stopBackgroundMusic(){
    	if(backgroundClip!=null) {
    		backgroundClip.stop();
    	}else {
            System.err.println("backgroundClip is null. Cannot stop background music.");
    	}
    }

    public void stopBackgroundMusic2(){
    	if(backgroundClip2!=null) {
    		backgroundClip2.stop();
    	}else {
            System.err.println("backgroundClip2 is null. Cannot stop background music.");
    	}
    }

    public void stopKeySFX(){
        keyClip.stop();
    }

    public void stopDoorSFX(){
        doorClip.stop();
    }
    
    public void stopTreasureSFX(){
        beepClip.stop();
    }

    public void stopLoseSFX(){
        loseClip.stop();
    }

    public void startWinSFX(){
        winClip.setFramePosition(0);
        winClip.start();
    }

    public void stopWinSFX(){
        winClip.stop();
    }

    public void stopInfoSFX(){
        infoClip.stop();
    }

    /**
     * method to get chap images
     * 
     * @param direction
     * @return little chap based on the direction 
     */
    public BufferedImage getChapImage(String direction) {
        return chapImages.get(direction);
    }

    /**
     * method to initialize all the audios and sound effects
     */
    private void initializeAudio() {
        try {
            // Get the current working directory
            String currentDirectory = System.getProperty("user.dir");
            String audioPath = currentDirectory.replace(File.separator + "src", "") + File.separator + "audio" + File.separator;

            // Initialize background music
            backgroundClip = loadAudioClip(audioPath + "game-music-loop.wav");
            backgroundClip2 = loadAudioClip(audioPath + "game-music-loop2.wav");

            // Initialize SFX
            keyClip = loadAudioClip(audioPath + "key-sfx.wav");
            loseClip = loadAudioClip(audioPath + "lose-sfx.wav");
            doorClip = loadAudioClip(audioPath + "door-sfx.wav");
            beepClip = loadAudioClip(audioPath + "beep-sfx.wav");
            winClip = loadAudioClip(audioPath + "winning-sfx.wav");
            infoClip = loadAudioClip(audioPath + "info-sfx.wav");
        } catch (Exception e) {
            System.out.println("Audio not initialized: " + e.getMessage());
        }
    }

    private Clip loadAudioClip(String filePath) throws Exception {
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(filePath));
        Clip clip = AudioSystem.getClip();
        clip.open(audioStream);
        return clip;
    }

    /**
     * method to initialize images
     */
    private void initializeImages(){
        try {
            String currentDirectory = System.getProperty("user.dir");
            String filePath = currentDirectory.replace(File.separator+"src", "") + File.separator+"images"+File.separator+"spritesheet.png";
            this.spritesheet = ImageIO.read(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int XWhiteBG = 288;
        int XBlackBG = 384;
        // put the little chap into the hashmap, with move as it's key
        chapImages.put("up", makeTransparent(spritesheet.getSubimage(XWhiteBG, 384, 32, 32), spritesheet.getSubimage(XBlackBG, 384, 32, 32)));
        chapImages.put("left", makeTransparent(spritesheet.getSubimage(XWhiteBG, 416, 32, 32), spritesheet.getSubimage(XBlackBG, 416, 32, 32)));
        chapImages.put("down", makeTransparent(spritesheet.getSubimage(XWhiteBG, 448, 32, 32), spritesheet.getSubimage(XBlackBG, 448, 32, 32)));
        chapImages.put("right", makeTransparent(spritesheet.getSubimage(XWhiteBG, 480, 32, 32), spritesheet.getSubimage(XBlackBG, 480, 32, 32)));
    }

}