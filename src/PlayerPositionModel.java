import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by cmitchelmore on 16/11/14.
 */
public class PlayerPositionModel {
    private static final int X_WIDTH = 100,
            Y_WIDTH = 100;
    private static final String OWN_TEAM = "own",
            OTHER_TEAM = "thr";

    private HashMap<String,Point> knownPositions;
    private HashMap<ArrayList<Integer>,ArrayList<Integer>> mapping;
    private ArrayList<Player> unmappedPlayersWithKnownPositions;
    private ArrayList<Player> unmappedPlayersWithUnknownPositions;


    public PlayerPositionModel()
    {
        knownPositions = new HashMap<String, Point>();
        mapping = new HashMap<ArrayList<Integer>, ArrayList<Integer>>();
        unmappedPlayersWithKnownPositions = new ArrayList<Player>();
        unmappedPlayersWithUnknownPositions = new ArrayList<Player>();
    }

    public void clearModel()
    {

    }

    public void addSelf(Player p)
    {
        if (knownPositions.containsKey(OWN_TEAM+p.getPlayer().getNumber())) {
            //No need to add them
        } else {

        }
    }

    public void addPlayer(Player p, boolean ownTeam)
    {

    }

}
