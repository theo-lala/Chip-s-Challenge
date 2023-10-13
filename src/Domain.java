package src;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.awt.*;

/**
 * Responsible for representing and maintaining the state of the game
 *
 * @author James Goode
 * @version 5 21/09/2023
 **/
public class Domain
{     

    /**
     *  Global variables
     */

    private ArrayList<ArrayList<Tile>> board = new ArrayList<>();
    private static ArrayList<Command> commands = new ArrayList<>();
    private static ArrayList<EnemyTile> enemies = new ArrayList<>();
    private PrivateOperation privateOperation;
    private Persistency persistency = null;
    private static ChapTile theChap = null;
    private static final int BUFFER = 8; // the extra space needed to display outofbounds areas

    /**
     * constructor 
     */

    public Domain(){
        privateOperation = new PrivateOperation();
    }

    /**
     *  PUBLIC METHODS
     */

    /**
     * due to the nature of persistency needing domain, and domain needing persistency 
     * one must be set after initilisation, in this case, domain does not have persistency set
     * in its constructor so persistancy can be constructed with a domain instance.
     * After, persistancy is set inside domain with this method. 
     */

    public void setPersistency( Persistency p ){
        persistency = p;
    }

    /**
     *  loads a level from persistency
     */

    public void loadLevel( String level ){
        if(persistency == null){
            throw new NullPointerException("Persistency is null");
        }
        board = persistency.getLevel( level );
        if(board.isEmpty()){
            throw new NullPointerException("board is empty");
        }
        privateOperation.initializeBoard();
    }

    /**
     *  loads a level from an already existing board for testing
     */

    public void loadNonJsonLevel( ArrayList<ArrayList<Domain.Tile>> level ){
        board = level;
        if(board.isEmpty()){
            throw new NullPointerException("board is empty");
        }
        privateOperation.initializeBoard();
    }

    /**
     *  returns the current board
     */

    public ArrayList<ArrayList<Tile>> getBoard(){
        return board;
    }

    /**
     *  returns the Chap
     */

    public ChapTile getChap(){
        return theChap;
    }

    /**
     *  returns a list of commands that need to be executed
     */

    public ArrayList<Command> getCommands(){
        ArrayList<Command> commandsR = new ArrayList<Command>(commands);
        commands.clear();
        return commandsR;
    }

    /**
     *  updates enemy positons
     */

    public void updateEnemies(){
        for(EnemyTile e : enemies){
            e.move();
        }
    }

    /**
     * 
     * Responsible for representing any Tile
     * Houses properties & methods shared amoung all unique tiles
     * 
     * icons are set upon creation
     *
     * the only subclass of tile that has methods useable by other modules is ChapTile
     **/
    public class Tile
    {
        java.util.List<BufferedImage> icons = new ArrayList<>(); // the BufferedImages of what images the tile can look like
        boolean enterable; // if the tile can be entered by chap
        BufferedImage icon; // the current icon of the tile
        int iconIndex; // only used by multi colored tiles to identify uniqueness  

        /**
         * intentionally private, call it's subclasses please
         * 
         * Constructor for objects of class Tile
         * takes the classtype of it's display icon
         **/   
        private Tile(java.util.List <BufferedImage> images, boolean baseEnterable, int iconIndex ){
            icons = images;  
            enterable = baseEnterable;
            icon = null;
            this.iconIndex = iconIndex;
            setIcon(iconIndex);
        }

        /**
         * clone constructor 
         * 
         * Constructor for cloning a tile
         * takes the other tile and copies the data into itself
         **/  
        private Tile(Tile other) {
            if( other == null ){
                throw new NullPointerException("Clone is null");
            }
            icons = other.icons;
            enterable = other.enterable;
            icon = other.icon;
            iconIndex = other.iconIndex;
        }

        /**
         *  sets if the chap can enter this tile
         */
        public void setEnterable( boolean value ) {
            enterable = value;    
        }

        /**
         *  returns the image of the icon this tile is visually represented by
         */
        public BufferedImage getIcon(){
            if ( icon == null ) {
                throw new NullPointerException("Unset icon");
            }
            return icon;
        }

        /**
         *  sets from a variaty of similar Icons, the icon that you want to display
         */
        public void setIcon( int type ){
            if ( icons.isEmpty() || icons.size()-1<type ) {
                throw new IndexOutOfBoundsException("Icon at index "+type+" does not exist");
            }
            icon = icons.get(type);
            iconIndex = type;
        }

        /**
         *  returns a list of the Icons that could possibly represent this tile
         */
        public java.util.List<BufferedImage> getIcons(){
            return icons;
        }

        /**
         *  returns the Index of the icon that currently represents the tile in the list of possible icons.
         */
        public int getIconIndex(){
            if ( iconIndex == -1 ) {
                throw new IllegalArgumentException("Unset iconIndex (you need to set your icon to set your icon index)");
            }
            return iconIndex;
        }

        // overridden subclass methods

        // as of the current version only chap can move, but this may chanage in the future so this will remain a parent method
        public boolean move ( String direction , boolean moving){
            return false;
        }
        // interacts with the idea that chap is looking to enter this tile but has not as of yet entered
        public boolean interactInfront (){
            return false;
        }
        // interacts with the idea that chap has entered this tile
        public boolean interactOntop (){
            return false;
        }
    }

    /**
     * Responsible for representing a Wall Tile which can't be entered
     * 
     * Wall tiles 
     **/
    public class WallTile extends Tile
    {
        public WallTile() {
            super( privateOperation.getImage( WallTile.class ) , false , 0);
        }
    }

    /**
     * Responsible for representing a Free Tile which can be entered
     **/
    public class FreeTile extends Tile
    {
        public FreeTile() {
            super( privateOperation.getImage( FreeTile.class ) , true , 0);
        }
    }

    /**
     * Responsible for representing a Key (Tile type) which can be entered and picked up
     **/
    public class KeyTile extends Tile
    {    
        public KeyTile( int color ) {
            super( privateOperation.getImage( KeyTile.class ) , true , color);
        }

        @Override
        public boolean interactOntop (){
            theChap.getInventory().add(this);
            commands.add(new Command("playSound","key"));
            return true;
        }
    }

    /**
     * Responsible for representing a Locked Door (Tile type) which can be entered if key is present
     **/
    public class LockedDoorTile extends Tile
    { 
        public LockedDoorTile( int color ) {
            super( privateOperation.getImage( LockedDoorTile.class ) , false, color );
        }

        @Override
        public boolean interactInfront (){
            java.util.List<Tile> inv = theChap.getInventory();
            int size  = inv.size();
            for (int i = 0; i < size; i++ ){
                if( inv.get(i).getIconIndex() == super.getIconIndex() ){
                    theChap.getInventory().remove(i);
                    commands.add(new Command("playSound","unlockedDoor"));
                    setEnterable(true);
                    break;
                }
            }
            return true;
        }
    }

    /**
     * Responsible for representing a Infomation Feild (Tile type) which displays info when entered
     **/
    public class InfoFeildTile extends Tile
    {
        String info;
        public InfoFeildTile(String info) {
            super( privateOperation.getImage( InfoFeildTile.class ) , true, 0 );
            this.info = info;
        }

        @Override
        public boolean interactOntop(){
            commands.add(new Command("playSound","info"));
            commands.add(new Command("printInfo",info));
            return false;
        }
    }

    /**
     * Responsible for representing a Treasure (Tile type) which can be entered and picked up
     **/
    public class TreasureTile extends Tile
    {
        public TreasureTile() {
            super( privateOperation.getImage( TreasureTile.class ) , true, 0 );
        }

        @Override
        public boolean interactOntop(){
            theChap.collectATreasure();
            commands.add(new Command("playSound","treasure"));
            return true;
        }
    }

    /**
     * Responsible for representing a Exit Lock (Tile type) which can be entered if no treasures remain
     **/
    public class ExitLockTile extends Tile
    {

        public ExitLockTile() {
            super( privateOperation.getImage( ExitLockTile.class ) , false, 0 );
        }

        @Override
        public boolean interactInfront(){
            if ( theChap.getTreasureTotal() == 0 ){
                super.setEnterable(true);
                commands.add(new Command("playSound","unlockedDoor"));
            }
            return true;
        }
    }

    /**
     * Responsible for representing a Exit (Tile type) which tells chap they won
     **/
    public class ExitTile extends Tile
    {
        public ExitTile() {
            super( privateOperation.getImage( ExitTile.class ) , true, 0 );
        }

        @Override
        public boolean interactOntop(){    
            commands.add(new Command("playSound","win"));
            theChap.hasWon( true );
            setIcon(2);
            return false;
        }
    }

    /**
     * Responsible for representing a Enemy (Tile type) which moves and tells chap they lost upon contact
     **/
    public class EnemyTile extends Tile
    {
        java.util.List<Coordinates> path = new ArrayList<Coordinates>();
        int count = 0;
        int lastCount;

        public EnemyTile( java.util.List<Coordinates> path ) {
            super( privateOperation.getImage( EnemyTile.class ) , true, 0 );
            enemies.add(this);
            this.path = path;
            lastCount = path.size()-1;
        }

        public void move(){
            if(count+1 == path.size()){
                count = 0;
            }else{
                count++;
            }

            if(lastCount+1 == path.size()){
                lastCount = 0;
            }else{
                lastCount++;
            }

            int HALFBUFFER = BUFFER/2;

            Tile next = board.get(path.get(count).x()+HALFBUFFER).get(path.get(count).y()+HALFBUFFER);

            if(next instanceof ChapTile){
                next.interactInfront();
            }

            board.get(path.get(lastCount).x()+HALFBUFFER).set(path.get(lastCount).y()+HALFBUFFER, new FreeTile()); 
            board.get(path.get(count).x()+HALFBUFFER).set(path.get(count).y()+HALFBUFFER, this); 

        }

        @Override
        public boolean interactInfront(){ 
            commands.add(new Command("playSound","loss"));
            theChap.hasLost( true );
            return true;
        }
    }

    /**
     * Responsible for representing a Chap (Tile type) ( expected to be single instance )
     * 
     * chap can't enter himself hence 'false'
     * chap can move and can hold a inventory of tiles as well as the tile they are standing on
     **/
    public class ChapTile extends Tile
    {
        ArrayList<Tile> inventory = new ArrayList<>();
        Coordinates coordinates;
        Tile standingOn;

        boolean hasWon = false;
        boolean hasLost = false;

        int treasureTotal;

        /**
         * constructor for chap
         */
        public ChapTile( int treasureTotal) {
            super( privateOperation.getImage( ChapTile.class ) , false, 2 );
            this.treasureTotal = treasureTotal;
            standingOn = new FreeTile();
            theChap = this;
        }

        /**
         * adds a Tile to chaps inventory
         */
        public void addToInventory( Tile t ){
            inventory.add(t);
        }

        /**
         *  returns chaps inventory
         */
        public java.util.List<Tile> getInventory(){
            return inventory;
        }

        /**
         *  tells chap if they have won or not
         */
        public void hasWon( boolean value ){
            hasWon = value;
        }

        /**
         *  tells chap if they have lost or not
         */
        public void hasLost( boolean value ){
            hasLost = value;
        }

        /**
         *  returns if chap has won or not
         */
        public boolean getHasWon(){
            return hasWon;
        }

        /**
         *  returns if chap has lost or not
         */
        public boolean getHasLost(){
            return hasLost;
        }

        /**
         *  returns what tile chap is standing on
         */
        public Tile getStandingOn(){
            return standingOn;
        }

        /**
         *  sets what chap is standing on 
         */
        public void setStandingOn( Tile s ){
            standingOn = s;
        }

        public void setCoordinates( Coordinates c ){
            coordinates = c;
        }

        public Coordinates getChapCoordinates(){
            return coordinates;
        }

        @Override
        public BufferedImage getIcon(){
            return privateOperation.overlayImage(icon,icons.get(iconIndex+4),standingOn.getIcon());
        }

        public void setIcon( BufferedImage b ){
            icon = b;
        }

        public int getTreasureTotal(){
            return treasureTotal;
        }

        /**
         *  removes a treasure from the total chap needs to collect to show they collected a tresasure
         */ 
        public void collectATreasure(){
            if ( treasureTotal<1 ) {
                throw new IllegalArgumentException("Setting negitave treasure");
            }else{
                treasureTotal--;
            }
        }

        @Override
        public boolean interactInfront(){ 
            commands.add(new Command("playSound","loss"));
            theChap.hasLost( true );
            updateStatus();
            return true;
        }

        /**
         *  checks to see if chap died, used because enemies may have killed chap when chap walked into a enemy
         */
        private void updateStatus(){
            if(hasLost){
                board.get(coordinates.x()).set(coordinates.y(),new FreeTile());
            } 
        }

        // only boolean so you can detect a can or can't move to play an animation, this will update //his position automatically 
        //boolean moving being false means that it will remove chap from the board so you can do the animation 
        @Override
        public boolean move( String direction , boolean moving ){
            // if chap is not on board, you can't move     
            Coordinates testInstanceExists = privateOperation.findXY(this);

            if(testInstanceExists!=null){
                coordinates = new Coordinates(testInstanceExists.x,testInstanceExists.y);
            }

            if(testInstanceExists == null && !moving ){
                return false;
            }

            Tile next = null;

            int x = coordinates.x;
            int y = coordinates.y;

            switch(direction){
                case "left" :  next = board.get(x-1).get(y);
                    if(moving){
                        commands.add(new Command("move","left"));
                    }
                    setIcon(1);
                    break;
                case "right" : next = board.get(x+1).get(y);
                    if(moving){
                        commands.add(new Command("move","right"));
                    }
                    setIcon(3);
                    break;

                case "up" : next = board.get(x).get(y-1);
                    if(moving){
                        commands.add(new Command("move","up"));
                    }
                    setIcon(0);
                    break;

                case "down" : next = board.get(x).get(y+1);
                    if(moving){
                        commands.add(new Command("move","down"));
                    }
                    setIcon(2);
                    break;
                default :
                    throw new IllegalArgumentException("Did not specify a handled directional input");
            }

            // chap can now interact with the next tile 
            if( !moving ){
                if ( next.interactInfront() && privateOperation.canEnter( next )){           
                    privateOperation.replace(next, new FreeTile());    
                }else if (!privateOperation.canEnter( next )){
                    return false;
                }
            }else if (!privateOperation.canEnter( next )){
                return false;
            }

            // chap can now enter the next tile if he is moving (ie is not in an animation)
            if ( moving ){
                privateOperation.enter(next);
            }else{
                board.get(coordinates.x()).set( coordinates.y() , standingOn ); 

            }

            if( hasWon ) {
                // end the game

            }

            // signal that chap entered the next tile or begain a animation to enter the next tile

            return true;
        }
    }

    /**
     * Responsible for handling private game logic
     *
     * @author James Goode
     **/

    private class PrivateOperation
    {

        /**
         * Theo's code, used due to the lack of connection from Board to Renderer 
         *  Combines chap and the tile they stand on to make a flush image to display
         */
        public BufferedImage overlayImage(BufferedImage imageWhite, BufferedImage imageBlack, BufferedImage background) {   
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

                    if (isWhite(rgbWhite) && isBlack(rgbBlack)) {
                        transparentImage.setRGB(x, y, 0x00FFFFFF); 
                    } else {
                        transparentImage.setRGB(x, y, rgbWhite); 
                    }
                }
            }

            BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D resultGraphics = resultImage.createGraphics();
            resultGraphics.drawImage(background, 0, 0, null);
            resultGraphics.drawImage(transparentImage, 0, 0, null);
            resultGraphics.dispose();

            return resultImage;

        }

        private boolean isWhite(int rgb) {
            Color color = new Color(rgb);
            return (color.getGreen() == 255 && color.getRed() == 255 && color.getBlue() == 255);
        }

        private boolean isBlack(int rgb) {
            Color color = new Color(rgb);
            return (color.getGreen() == 0 && color.getRed() == 0 && color.getBlue() == 0);
        }

        /**
         *  add the spaces to the outside of the board so when chap goes into a corner there are
         *  spaces to display.
         */
        private void initializeBoard() {
            // Create a larger gBoard with white tiles as a buffer
            int oldLength = board.size(); 
            int oldHeight = board.get(0).size(); 

            int length = board.size() + BUFFER; // 4 tiles on each side
            int height = board.get(0).size() + BUFFER;

            ArrayList<ArrayList<Domain.Tile>> gBoard = new ArrayList<ArrayList<Domain.Tile>>();

            for (int i = 0; i < length; i++) {
                ArrayList<Domain.Tile> row = new ArrayList<>();
                for (int j = 0; j < height; j++) {
                    row.add(new FreeTile());
                }
                gBoard.add(row);
            }

            for (int i = 0; i < oldLength; i++) {
                for (int j = 0; j <  oldHeight; j++) {

                    gBoard.get(i+BUFFER/2).set(j+BUFFER/2,board.get(i).get(j));

                }
            }

            board = gBoard;

            theChap.setCoordinates(privateOperation.findXY(theChap));
        }

        private java.util.List<BufferedImage> getImage( Class<?> wildTileType ){  
            BufferedImage spritesheet = null;

            try{
                String currentDirectory = System.getProperty("user.dir");
                String fileName = "spritesheet.png";
                String filePath = currentDirectory.replace(File.separator+"src", "") + File.separator +"images"+ File.separator + fileName;
                spritesheet = ImageIO.read(new File(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if( spritesheet == null ){ throw new IllegalArgumentException("Could not find 'spritesheet' in 'images' folder in the target directory");}
            else if (wildTileType == WallTile.class) {
                return  java.util.List.of(spritesheet.getSubimage(0, 32, 32, 32)); 
            } else if (wildTileType == FreeTile.class) {
                return java.util.List.of(spritesheet.getSubimage(0, 0, 32, 32));  
            } else if (wildTileType == KeyTile.class) {
                return java.util.List.of(spritesheet.getSubimage(192, 128, 32, 32),spritesheet.getSubimage(192, 160, 32, 32),
                    spritesheet.getSubimage(192, 192, 32, 32),spritesheet.getSubimage(192, 224, 32, 32)); 
            } else if (wildTileType == LockedDoorTile.class) {
                return java.util.List.of(spritesheet.getSubimage(32, 192, 32, 32),spritesheet.getSubimage(32, 224, 32, 32),
                    spritesheet.getSubimage(32, 256, 32, 32),spritesheet.getSubimage(32, 288, 32, 32)); 
            } else if (wildTileType == InfoFeildTile.class) {
                return java.util.List.of(spritesheet.getSubimage(64, 480, 32, 32)); 
            } else if (wildTileType == TreasureTile.class) {
                return java.util.List.of(spritesheet.getSubimage(0, 64, 32, 32)); 
            } else if (wildTileType == ExitLockTile.class) {
                return java.util.List.of(spritesheet.getSubimage(64, 64, 32, 32)); 
            } else if (wildTileType == ExitTile.class) {
                return java.util.List.of(spritesheet.getSubimage(32, 160, 32, 32),spritesheet.getSubimage(96, 320, 32, 32), spritesheet.getSubimage(96, 352, 32, 32),spritesheet.getSubimage(96, 288, 32, 32)); 

            } else if (wildTileType == ChapTile.class) {          
                return java.util.List.of(
                    spritesheet.getSubimage(288, 384, 32, 32),spritesheet.getSubimage(288, 416, 32, 32),
                    spritesheet.getSubimage(288, 448, 32, 32),spritesheet.getSubimage(288, 480, 32, 32),
                    spritesheet.getSubimage(384, 384, 32, 32),spritesheet.getSubimage(384, 416, 32, 32),
                    spritesheet.getSubimage(384, 448, 32, 32),spritesheet.getSubimage(384, 480, 32, 32));
            }        
            else if(wildTileType == EnemyTile.class){
                return java.util.List.of(
                    spritesheet.getSubimage(128, 0, 32, 32),spritesheet.getSubimage(128, 32, 32, 32),
                    spritesheet.getSubimage(128, 64, 32, 32),spritesheet.getSubimage(128, 96, 32, 32));

            } else {
                throw new IllegalArgumentException("Unsupported tile type");
            }
        }

        /**
         * Tile Interaction and entering logic
         **/

        private void replace( Tile replacing, Tile replacement){
            for( int i = 0; i < board.size(); i ++ ){
                for( int j = 0; j < board.get(0).size(); j++ ){
                    if( board.get(i).get(j).equals(replacing)){
                        board.get(i).set(j,replacement);
                    }
                }
            }   
        }

        /**
         * Constructor for objects of class TileManager
         **/
        private boolean canEnter( Tile T )
        {
            return T.enterable;
        }

        /**
         * Tile Movement communication with logic
         * 
         * Has no interaction with animation of said movement, only the internal updating of positions. 
         **/

        private Coordinates findXY(Tile T){
            if(!board.isEmpty() && !board.get(0).isEmpty() ){ 
                for( int i = 0; i < board.size(); i ++ ){
                    for( int j = 0; j < board.get(0).size(); j++ ){
                        if( board.get(i).get(j).equals(T)){
                            //System.out.println("FindXY found "+i+" "+j+"with a board size of "+board.size());

                            return new Coordinates(i,j);
                        }
                    }
                }       
            }
            return null;
        } 

        private void enter(Tile entering ){

            // add new tile

            int x = theChap.getChapCoordinates().x;
            int y = theChap.getChapCoordinates().y;

            Coordinates coord = findXY(entering);

            int xE = coord.x;
            int yE = coord.y;

            boolean canBePickedUp = entering.interactOntop();

            if(canBePickedUp){
                Tile replacement = new FreeTile(); 
                replace(entering,replacement);
                entering = replacement;
            }

            board.get(x).set( y , theChap.getStandingOn() );
            theChap.setStandingOn(entering);

            theChap.setCoordinates(new Coordinates(xE,yE)) ;

            // add player
            if ( !theChap.getHasWon() && !theChap.getHasLost()){
                board.get(xE).set( yE , theChap );  
                //System.out.println("chap is at :"+xE+" "+yE);
            }
        }
    }

    /**
     *  for storing two values for x,y positions
     **/

    public record Coordinates( int x, int y){}

    /**
     *  for passing arguments with lables to app
     **/

    public record Command( String type, String argument){}

}
