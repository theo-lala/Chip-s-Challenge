package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class for handling all saving and loading of level information
 *
 * @author Jake Domb
 */
class Persistency {
    private final App app;
    private final Domain domain;
    private final Map<String, Integer> tileToIntMap;

    /**
     * Constructor for Persistency.
     */
    public Persistency(App app, Domain domain) {
        this.app = app;
        this.domain = domain;

        this.tileToIntMap = constructTileToIntMap();
    }

    /**
     * Method for loading a level from a file.
     *
     * @param level The ID of the level to load.
     *
     * @return The level as a 2D ArrayList of Domain.Tiles.
     */
    public ArrayList<ArrayList<Domain.Tile>> getLevel(String level) {
        long currentTime = System.currentTimeMillis();
        System.out.println("Loading level: " + level);
        String lvlString = new String();

        try {
            lvlString = Files.readString(Path.of("levels" + File.separator + level + ".json"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        var loadedLevel = getLevelFromJSON(lvlString);
        System.out.println("Finished loading level in " + (System.currentTimeMillis() - currentTime) + "ms");
        return loadedLevel;
    }

    /**
     * Method for building a level from a JSON string.
     * Do not use this method unless you know what you are doing.
     *
     * @param lvlString The JSON string to build the level from.
     *
     * @return The level as a 2D ArrayList of Domain.Tiles.
     */
    public ArrayList<ArrayList<Domain.Tile>> getLevelFromJSON(String lvlString) {
        long currentTime = System.currentTimeMillis();
        JSONObject lvl = new JSONObject(lvlString);

        int wid = lvl.getInt("width");
        int hit = lvl.getInt("height");

        ArrayList<ArrayList<Domain.Tile>> tiles = new ArrayList<ArrayList<Domain.Tile>>(wid);

        for (int i = 0; i < wid; i++) {
            tiles.add(new ArrayList<Domain.Tile>(hit));
            for (int j = 0; j < hit; j++) {
                tiles.get(i).add(null);
            }
        }

        JSONArray tileArray = lvl.getJSONArray("tileArray");

        JSONArray inventoryArray = lvl.getJSONArray("inventoryArray");

        String chapStandingOn = lvl.getString("chapStandingOn");

        int levelNumber = lvl.getInt("levelNumber");
        int totalTime = lvl.getInt("totalTime");
        long elapsedTime = lvl.getLong("elapsedTime");

        app.setLevel(levelNumber);
        app.setTotalTime(totalTime);
        app.setElapsed(elapsedTime);

        for (int i = 0; i < wid; i++) {
            for (int j = 0; j < hit; j++) {
                String tileString = tileArray.getString(i + j * wid);

                Domain.Tile tile = getTileFromString(tileString);

                tiles.get(i).set(j, tile);
            }
        }

        domain.getChap().setStandingOn(getTileFromString(chapStandingOn));

        domain.getChap().inventory.clear();
        inventoryArray.toList().stream().filter(s -> s instanceof String).map(s -> (String) s)
                .map(s -> domain.new KeyTile(Integer.parseInt(s))).forEach(k -> domain.getChap().inventory.add(k));

        return tiles;
    }

    /**
     * Builds a Domain.Tile from a given String.
     *
     * @param tileString The String to build the Domain.Tile from.
     *
     * @return The Domain.Tile built from the given String.
     */
    private Domain.Tile getTileFromString(String tileString) {
        if (tileString == null || tileString.equals(""))
            return null;

        String[] tileStringArray = tileString.split(" ");
        int tileIndex = stringDecoder(tileStringArray[0]);

        switch (tileIndex) {
            case 0:
                return domain.new WallTile();
            case 1:
                return domain.new FreeTile();
            case 2:
                return domain.new KeyTile(Integer.parseInt(tileStringArray[1]));
            case 3:
                return domain.new LockedDoorTile(Integer.parseInt(tileStringArray[1]));
            case 4:
                return domain.new InfoFeildTile(tileStringArray[1].replace("_", " "));
            case 5:
                return domain.new TreasureTile();
            case 6:
                return domain.new ExitLockTile();
            case 7:
                return domain.new ExitTile();
            case 8:
                return domain.new ChapTile(Integer.parseInt(tileStringArray[1]));
            case 9:
                return domain.new EnemyTile(decodeEnemyString(tileStringArray[1]));
            default:
                throw new RuntimeException("Unknown tile type: " + tileStringArray[0]);
        }
    }

    /**
     *
     */
    public boolean saveLevel(String level, ArrayList<ArrayList<Domain.Tile>> tiles) {
        long currentTime = System.currentTimeMillis();
        System.out.println("Saving level: " + level);

        JSONObject lvl = saveLevelJSONObject(level, tiles);

        try {
            FileWriter file = new FileWriter("levels" + File.separator + level + ".json");

            file.write(lvl.toString(4));

            file.close();

            System.out.println("Finished saving level in " + (System.currentTimeMillis() - currentTime) + "ms");
            return true;
        } catch (IOException e) {
            System.out.println("Level save fail");
            e.printStackTrace();

            return false;
        }
    }

    /**
     * Method for saving a level to a file.
     *
     * @param level The ID of the level to save.
     * @param tiles The level as a 2D ArrayList of Domain.Tiles.
     *
     * @return Whether or not the save was successful.
     */
    private JSONObject saveLevelJSONObject(String level, ArrayList<ArrayList<Domain.Tile>> tiles) {
        JSONObject lvl = new JSONObject();

        int wid = tiles.size();
        int hit = tiles.get(0).size();

        lvl.put("width", wid);
        lvl.put("height", hit);

        String[] tileArray = new String[wid * hit];

        ArrayList<Domain.Tile> inventory = domain.getChap().inventory;

        for (int i = 0; i < wid; i++) {
            for (int j = 0; j < hit; j++) {
                String tileString = null;

                Domain.Tile tile = tiles.get(i).get(j);

                tileString = getStringFromTile(tile);

                tileArray[i + j * wid] = tileString;
            }
        }

        String[] inventoryArray;
        inventoryArray = new String[inventory.size()];
        for (int i = 0; i < inventory.size(); i++) {
            inventoryArray[i] = "" + inventory.get(i).iconIndex;
        }

        lvl.put("chapStandingOn", getStringFromTile(domain.getChap().getStandingOn()));

        lvl.put("tileArray", tileArray);

        lvl.put("inventoryArray", inventoryArray);

        lvl.put("levelNumber", app.getLevel());
        lvl.put("totalTime", app.getTotalTime());
        lvl.put("elapsedTime", app.getElapsedTime());

        return lvl;
    }

    /**
     * Builds a String to represent a given tile.
     *
     * @param tile The tile to be turned into a String.
     *
     * @return A String representing the tile.
     */
    private String getStringFromTile(Domain.Tile tile) {
        if (tile instanceof Domain.WallTile)
            return "wallTile";
        else if (tile instanceof Domain.FreeTile)
            return "freeTile";
        else if (tile instanceof Domain.KeyTile) {
            return "keyTile " + tile.iconIndex;
        } else if (tile instanceof Domain.LockedDoorTile) {
            return "lockedDoorTile " + tile.iconIndex;
        } else if (tile instanceof Domain.InfoFeildTile)
            return "infoFeildTile " + ((Domain.InfoFeildTile) tile).info.strip().replace(" ", "_");
        else if (tile instanceof Domain.TreasureTile)
            return "treasureTile";
        else if (tile instanceof Domain.ExitLockTile)
            return "exitLockTile";
        else if (tile instanceof Domain.ExitTile)
            return "exitTile";
        else if (tile instanceof Domain.ChapTile)
            return "chapTile " + ((Domain.ChapTile) tile).treasureTotal;
        else if (tile instanceof Domain.EnemyTile)
            return "enemyTile " + buildEnemyString((Domain.EnemyTile) tile);
        else
            throw new RuntimeException("Unknown tile type: " + tile.getClass().getName());
    }

    /**
     * Loads the exit state.
     *
     * @return The loaded ExitState.
     */
    public ExitState loadExitState() {
        String exitStateString = new String();

        if (new File("exitState.json").isFile()) {
            try {
                exitStateString = Files.readString(Path.of("exitState.json"));
            } catch (IOException e) {
                e.printStackTrace();

                return new ExitState(Optional.of(1), Optional.empty());
            }
        } else {
            return new ExitState(Optional.of(1), Optional.empty());
        }

        JSONObject exitStateJSON = new JSONObject(exitStateString);

        int level = exitStateJSON.getInt("level");
        String saveName = exitStateJSON.getString("saveName");

        return new ExitState(level != -1 ? Optional.of(level) : Optional.empty(),
                !saveName.equals("NOSAVE") ? Optional.of(saveName) : Optional.empty());
    }

    /**
     * Saves the exit state, ready to be loaded on the next boot.
     *
     * @param exitState The exit state to remeber.
     *
     * @return True if the state was saved successfully, false otherwise.
     */
    public boolean saveExitState(ExitState exitState) {
        JSONObject exitStateJSON = new JSONObject();

        int level = exitState.level().orElse(-1);
        String saveName = exitState.saveName().orElse("NOSAVE");

        exitStateJSON.put("level", level);
        exitStateJSON.put("saveName", saveName);

        try {
            FileWriter file = new FileWriter("exitState.json");

            file.write(exitStateJSON.toString(4));

            file.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    /**
     * Saves the exit state, ready to be loaded on the next boot.
     *
     * @param level The level to remember.
     *
     * @return True if the state was saved successfully, false otherwise.
     */
    public boolean saveExitState(int level) {
        return saveExitState(new ExitState(Optional.of(level), Optional.empty()));
    }

    /**
     * Saves the exit state, ready to be loaded on the next boot.
     *
     * @param saveName The name of the file to remember.
     *
     * @return True if the state was saved successfully, false otherwise.
     */
    public boolean saveExitState(String saveName) {
        return saveExitState(new ExitState(Optional.empty(), Optional.of(saveName)));
    }

    /**
     * Builds an 'Enemy String', a String representing the movement path of an
     * enemy.
     *
     * @param tile The enemy tile to build the string from.
     *
     * @return String the enemy string.
     */
    private String buildEnemyString(Domain.EnemyTile tile) {
        return tile.path.stream().map(c -> c.x() + ":" + c.y()).reduce((a, b) -> (a + "," + b)).orElse("");
    }

    /**
     * Decodes an enemy string into a list of coordinates.
     *
     * @param enemyString The enemy string to decode.
     *
     * @return List of coordinates extracted from the enemy string.
     */
    private List<Domain.Coordinates> decodeEnemyString(String enemyString) {
        return Arrays.stream(enemyString.split(",")).map(c -> {
            String[] split = c.split(":");
            return new Domain.Coordinates(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        }).toList();
    }

    private Map<String, Integer> constructTileToIntMap() {
        Map<String, Integer> tileToIntMap = new HashMap<>();

        tileToIntMap.put("wallTile", 0);
        tileToIntMap.put("freeTile", 1);
        tileToIntMap.put("keyTile", 2);
        tileToIntMap.put("lockedDoorTile", 3);
        tileToIntMap.put("infoFeildTile", 4);
        tileToIntMap.put("treasureTile", 5);
        tileToIntMap.put("exitLockTile", 6);
        tileToIntMap.put("exitTile", 7);
        tileToIntMap.put("chapTile", 8);
        tileToIntMap.put("enemyTile", 9);

        return tileToIntMap;
    }

    private int stringDecoder(String str) {
        char[] chars = str.toCharArray();
        switch (chars[0]) {
            case 'w':
                return 0;
            case 'f':
                return 1;
            case 'k':
                return 2;
            case 'l':
                return 3;
            case 'i':
                return 4;
            case 't':
                return 5;
            case 'e':
                if (chars[4] == 'L')
                    return 6;
                else if (chars[4] == 'T')
                    return 7;
                else 
                    return 9;
            case 'c':
                return 8;
        }

        // Should never happen
        return tileToIntMap.get(str);
    }
}

record ExitState(Optional<Integer> level, Optional<String> saveName) {
}
