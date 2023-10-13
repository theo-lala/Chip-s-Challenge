package src;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

class DomainTest {

    /**
     * test that you cant enter a wall and interacting changes nothing
     */
    @Test
    public void test_Wall() {
        Domain domain = new Domain();
        Domain.Tile wallTile = domain.new WallTile();
        
        assertFalse(wallTile.enterable);
        
        assertFalse(wallTile.interactInfront());
        assertFalse(wallTile.interactOntop());
    }
    
    /**
     * test that you can enter a free tile and interacting changes nothing
     */
    @Test
    public void test_Free() {
        Domain domain = new Domain();
        Domain.Tile freeTile = domain.new FreeTile();
        
        assertTrue(freeTile.enterable);
        
        assertFalse(freeTile.interactInfront());
        assertFalse(freeTile.interactOntop());
    }
    
    /**
     * test that you can enter and pickup a key tile
     */
    @Test
    public void test_Key() {
        Domain domain = new Domain();
        Domain.Tile chapTile = domain.new ChapTile(1);
        Domain.Tile keyTile = domain.new KeyTile(1);
        
        assertTrue(keyTile.enterable);
        
        assertFalse(keyTile.interactInfront()); 
        assertTrue(keyTile.interactOntop());
        
        assertTrue(domain.getChap().getInventory().contains(keyTile));
    }
    
    /**
     * test that you cant enter a door unless you have a key of the same number '1'
     */
    @Test
    public void test_Door() {
        Domain domain = new Domain();
        Domain.Tile chapTile = domain.new ChapTile(1);
        Domain.Tile doorTile = domain.new LockedDoorTile(1);
        
        assertFalse(doorTile.enterable);
        
        assertTrue(doorTile.interactInfront()); 
        assertFalse(doorTile.interactOntop());
        
        assertFalse(doorTile.enterable);
        
        Domain.Tile keyTile = domain.new KeyTile(1);
        keyTile.interactOntop();
        
        doorTile.interactInfront(); 
        assertTrue(doorTile.enterable);
    }
    
    /**
     * test that commands work along with info running the correct commands
     */
    @Test
    public void test_Info() {
        Domain domain = new Domain();
        domain.getCommands(); // clear any commands 
        Domain.Tile infoFeildTile = domain.new InfoFeildTile("test");
        
        assertTrue(infoFeildTile.enterable);
        
        assertFalse(infoFeildTile.interactInfront()); 
        assertFalse(infoFeildTile.interactOntop());
        
        assertTrue(domain.getCommands().size() == 2);
        
        for(Domain.Command c : domain.getCommands()){
            switch (c.type()){
                case "playSound" : assertTrue(c.argument().equals("info")); 
                    break;
                case "printInfo" : assertTrue(c.argument().equals("test")); 
                    break;
            }
        }
        
    }
    
    /**
     * test that picking up treasure updates in chap
     */
    @Test
    public void test_Treasure() {
        Domain domain = new Domain();
        Domain.Tile chapTile = domain.new ChapTile(2);
        Domain.Tile treasureTile = domain.new TreasureTile();
        
        assertTrue(treasureTile.enterable);
        
        assertFalse(treasureTile.interactInfront());
        assertTrue(treasureTile.interactOntop());
        
        assertTrue(domain.getChap().getTreasureTotal() == 1);
    }
    
    /**
     * test that chap can only enter a exit lock if they have zero remaining treasures
     */
    @Test
    public void test_ExitLock() {
        Domain domain = new Domain();
        Domain.Tile chapTile = domain.new ChapTile(1);
        Domain.Tile exitLockTile = domain.new ExitLockTile();
        
        assertFalse(exitLockTile.enterable);
        
        assertTrue(exitLockTile.interactInfront()); 
        assertFalse(exitLockTile.interactOntop());
        
        assertFalse(exitLockTile.enterable);
        
        domain.getChap().collectATreasure();
        
        exitLockTile.interactInfront(); 
        assertTrue(exitLockTile.enterable);
    }
    
    /**
     * test that you can enter a exit tile
     */
    @Test
    public void test_Exit() {
        Domain domain = new Domain();
        Domain.Tile chapTile = domain.new ChapTile(1);
        Domain.Tile exitTile = domain.new ExitTile();
        
        assertTrue(exitTile.enterable);
        
        assertFalse(exitTile.interactInfront());
        assertFalse(exitTile.interactOntop());
    }
    
    /**
     * test that enemies move correctly and kill chap when touching them
     */
    @Test
    public void test_Enemy(){
        Domain domain = new Domain();

        Persistency persistency = new Persistency(new App(), domain);

        String lvlString = """
            {
                "chapStandingOn": "freeTile",
                "inventoryArray": [],
                "totalTime": 1,
                "width": 4,
                "levelNumber": 1,
                "height": 4,
                "elapsedTime": 0,
                "tileArray": [
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "enemyTile 1:1,2:1,",
                    "chapTile 1",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                ]
            }""";
        domain.loadNonJsonLevel(persistency.getLevelFromJSON(lvlString));
        
        assertEquals(Domain.EnemyTile.class, domain.getBoard().get(1+4).get(1+4).getClass());
        
        domain.updateEnemies();
        
        assertEquals(Domain.EnemyTile.class, domain.getBoard().get(2+4).get(1+4).getClass());
        assertEquals(Domain.FreeTile.class, domain.getBoard().get(1+4).get(1+4).getClass());
        
        assertTrue(domain.getChap().getHasLost());
              
    }
    
    /**
     * test that chap can move
     * all other things chap can do are accounted in the other tile tests
     */
    @Test
    public void test_Chap(){
        Domain domain = new Domain();

        Persistency persistency = new Persistency(new App(), domain);

        String lvlString = """
            {
                "chapStandingOn": "freeTile",
                "inventoryArray": [],
                "totalTime": 1,
                "width": 4,
                "levelNumber": 1,
                "height": 4,
                "elapsedTime": 0,
                "tileArray": [
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "chapTile 1",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                    "freeTile",
                ]
            }""";
        domain.loadNonJsonLevel(persistency.getLevelFromJSON(lvlString));
        
        assertEquals(Domain.ChapTile.class, domain.getBoard().get(2+4).get(1+4).getClass());
        
        domain.getChap().move("left", false);
        domain.getChap().move("left", true);
        
        assertEquals(Domain.ChapTile.class, domain.getBoard().get(1+4).get(1+4).getClass());
        assertEquals(Domain.FreeTile.class, domain.getBoard().get(2+4).get(1+4).getClass());      
    }
    
}
