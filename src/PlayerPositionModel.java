import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by cmitchelmore on 16/11/14.
 */
public class PlayerPositionModel {
    private static final int X_WIDTH = 100,
            Y_WIDTH = 100;


    private HashMap<String,Point> lastKnownPositions;
    private HashMap<String,Point> knownPositions;
    private HashMap<Integer, ObjectRelativePosition> mapping;
    private ArrayList<Player> unmappedPlayersWithKnownPositions;
    private ArrayList<Player> unmappedPlayersWithUnknownPositions;
    private HashMap<Integer, ObjectAbsolutePosition> absolute;


    public PlayerPositionModel()
    {
        knownPositions = new HashMap<String, Point>();
        mapping = new HashMap<Integer, ObjectRelativePosition>();
        absolute = new HashMap<Integer, ObjectAbsolutePosition>();
        unmappedPlayersWithKnownPositions = new ArrayList<Player>();
        unmappedPlayersWithUnknownPositions = new ArrayList<Player>();
    }

    public void clearModel()
    {
        mapping.clear();
    }

    public void addPosition(Player p, int markerAbsoluteX, int markerAbsoluteY, double direction, double distance)
    {
        ObjectAbsolutePosition o = new ObjectAbsolutePosition(p.getPlayer().getNumber(), markerAbsoluteX, markerAbsoluteY, direction, distance);
        absolute.put(p.getPlayer().getNumber(), o);
    }



    public void addPlayer(Player p, boolean ownTeam, int number, double distance, double direction)
    {
        int relation = ownTeam ? ObjectRelativePosition.RELATION_OWN_TEAM : ObjectRelativePosition.RELATION_OTHER_TEAM;
        ObjectRelativePosition o = new ObjectRelativePosition(relation, p.getPlayer().getNumber(), number, distance, direction);
        mapping.put(o.hashCode(),o);
    }

    public void addBall(Player p, double distance, double direction)
    {
        ObjectRelativePosition o = new ObjectRelativePosition(ObjectRelativePosition.RELATION_BALL, p.getPlayer().getNumber(), 0, direction, distance);
        mapping.put(o.hashCode(),o);
    }


 }
class ObjectAbsolutePosition {


    public int markerAbsoluteX;
    public int markerAbsoluteY;
    public int number;
    public double direction;
    public double distance;

    public ObjectAbsolutePosition(int number, int markerAbsoluteX, int markerAbsoluteY, double direction, double distance)
    {
        this.markerAbsoluteX = markerAbsoluteX;
        this.markerAbsoluteY = markerAbsoluteY;
        this.number = number;
        this.direction = direction;
        this.distance = distance;
    }
}

class ObjectRelativePosition {
    
    public static final int RELATION_OWN_TEAM = 1,
            RELATION_OTHER_TEAM = 2,
            RELATION_BALL = 3;
    
    
    public int fromValue;
    public int relation;
    public int toValue;
    
    public double direction;
    public double distance;
    
    public ObjectRelativePosition(int relation, int fromValue, int toValue, double direction, double distance)
    {
        this.relation = relation;
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.direction = direction;
        this.distance = distance;
    }
    
    public int hashCode()
    {
        return fromValue * 1000 + toValue * 10 + relation;
    }
    
}