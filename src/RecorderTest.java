package src;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;


/**
 * JUnit tests for the Recorder class.
 * 
 * @author brodiebanfield
 *
 */
public class RecorderTest {

    private Recorder recorder;
    private App app;

    @Before
    public void setUp() {
        app = new App();
        recorder = new Recorder(app);
    }

    /**
     * Test case for adding a move to the Recorder and verifying it.
     */
    @Test
    public void testAddMove() {
        // Create sample data
        String direction = "right";
        long timestamp = System.currentTimeMillis();
        int level = 1;

        // Add the move to the recorder
        recorder.storeMovesMade(direction, timestamp, level);

        List<Recorder.Move> storedMoves = recorder.getStoredMoves();

        // Check if the list contains the added move
        boolean moveFound = false;
        for (Recorder.Move move : storedMoves) {
            if (move.getDirection().equals(direction) &&
                move.getTime() == timestamp &&
                move.getLevel() == level) {
                moveFound = true;
                break;
            }
        }

        assertTrue("The added move should be in the stored moves list", moveFound);

        // Test that the storedMoves list is not empty after adding a move
        assertFalse("The stored moves list should not be empty", storedMoves.isEmpty());

        // Test adding multiple moves and checking their order
        recorder.storeMovesMade("up", timestamp + 1000, level);
        List<Recorder.Move> updatedMoves = recorder.getStoredMoves();
        assertEquals("The second move added should be 'up'", "up", updatedMoves.get(1).getDirection());
        assertEquals("The first move added should be 'right'", "right", updatedMoves.get(0).getDirection());
    }
    
    /**
     * Test for setCurrentLevel method using Level 1.
     */
    @Test
    public void testSetCurrentLevelToLevel1() {
        int level = 1; // The level to set

        recorder.setCurrentLevel(level); // Set the current level

        // Check if the level is set correctly
        assertEquals(level, recorder.getLevel());
    }

    /**
     * Test for setCurrentLevel method using Level 2.
     */
    @Test
    public void testSetCurrentLevelToLevel2() {
        int level = 2; // The level to set

        recorder.setCurrentLevel(level); // Set the current level

        // Check if the level is set correctly
        assertEquals(level, recorder.getLevel());
    }
    
}
