package src;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONObject;
import org.json.JSONArray;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class PersistencyTest {

    /**
     * Testing if getLevel(String) returns an accurate level.
     * Very basic blank level.
     *
     * @see Persistency#getLevel(String)
     *
     */
    @Test
    public void test_1_getLevelFromJSON() {
        Domain d = new Domain();

        Persistency persistency = new Persistency(new App(), d);

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
                        "freeTile",
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
        ArrayList<ArrayList<Domain.Tile>> tiles = persistency.getLevelFromJSON(lvlString);

        for (int i = 0; i < tiles.size(); i++) {
            for (int j = 0; j < tiles.get(i).size(); j++) {
                Domain.Tile tile = tiles.get(i).get(j);
                assertEquals(tile.getClass(), Domain.FreeTile.class);
            }
        }
    }

    @Test
    public void test_2_getTileFromString_freeTile() {
        Domain d = new Domain();

        Persistency persistency = new Persistency(new App(), d);

        try {
            Method getTileFromString = List.of(Persistency.class.getDeclaredMethods()).stream()
                    .filter(m -> m.getName().equals("getTileFromString")).findFirst().get();
            getTileFromString.setAccessible(true);

            Object tile = getTileFromString.invoke(persistency, new Object[] { "freeTile" });

            assertEquals(tile.getClass(), d.new FreeTile().getClass());
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void test_3_getTileFromString_infoFeildTile() {
        Domain d = new Domain();

        Persistency persistency = new Persistency(new App(), d);

        try {
            Method getTileFromString = List.of(Persistency.class.getDeclaredMethods()).stream()
                    .filter(m -> m.getName().equals("getTileFromString")).findFirst().get();
            getTileFromString.setAccessible(true);

            Object tile = getTileFromString.invoke(persistency, new Object[] { "infoFeildTile Testing_info_tile" });

            assertEquals(tile.getClass(), d.new InfoFeildTile("").getClass());

            if (tile instanceof Domain.InfoFeildTile) {
                assertEquals(((Domain.InfoFeildTile) tile).info, "Testing info tile");
            }
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void test_4_saveLevelJSONObject() {
        Domain d = new Domain();

        Persistency persistency = new Persistency(new App(), d);

        try {
            Method saveLevelJSONObject = List.of(Persistency.class.getDeclaredMethods()).stream()
                    .filter(m -> m.getName().equals("saveLevelJSONObject")).findFirst().get();
            saveLevelJSONObject.setAccessible(true);

            ArrayList<ArrayList<Domain.Tile>> tiles = new ArrayList<ArrayList<Domain.Tile>>();

            int wid = 4, hit = 4;

            for (int i = 0; i < wid; i++) {
                tiles.add(new ArrayList<Domain.Tile>());
                for (int j = 0; j < wid; j++) {
                    if ((i + j) % 2 == 0) {
                        tiles.get(i).add(d.new FreeTile());
                    } else {
                        tiles.get(i).add(d.new WallTile());
                    }
                }
            }

            Object levelObject = saveLevelJSONObject.invoke(persistency, "testSave", tiles);

            assertEquals(levelObject.getClass(), JSONObject.class);

            JSONObject levelJSON = (JSONObject) levelObject;

            String levelString = levelJSON.toString(4);

            levelJSON = new JSONObject(levelString);

            assertEquals(levelJSON.get("width"), wid);
            assertEquals(levelJSON.get("height"), hit);

            System.out.println(levelJSON.toString(4));

            JSONArray tilesJSON = levelJSON.getJSONArray("tileArray");

            System.out.println(tilesJSON);

            for (int i = 0; i < wid; i++) {

                for (int j = 0; j < hit; j++) {
                    String tile = tilesJSON.getString(i + j * wid);
                    if ((i + j) % 2 == 0) {
                        assertEquals(tile, "freeTile");
                    } else {
                        assertEquals(tile, "wallTile");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals("", e.getMessage());
        }
    }
}
