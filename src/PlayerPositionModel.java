import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cmitchelmore on 16/11/14.
 */
public class PlayerPositionModel {



    private HashMap<String,Point> lastKnownPositions;
    private HashMap<String,Point> knownPositions;
    private HashMap<Integer, ObjectRelativePosition> mapping;
    private ArrayList<Player> unmappedPlayersWithKnownPositions;
    private ArrayList<Player> unmappedPlayersWithUnknownPositions;
    private HashMap<Integer, ArrayList<ObjectAbsolutePosition>> absolute;
    private HashMap<Integer, EstimatedPosition> estimatedPositions;


    public PlayerPositionModel()
    {
        knownPositions = new HashMap<String, Point>();
        mapping = new HashMap<Integer, ObjectRelativePosition>();
        absolute = new HashMap<Integer, ArrayList<ObjectAbsolutePosition>>();
        for (int i = 1; i <= 11; i++){
            absolute.put(i,new ArrayList<ObjectAbsolutePosition>());
        }
        unmappedPlayersWithKnownPositions = new ArrayList<Player>();
        unmappedPlayersWithUnknownPositions = new ArrayList<Player>();
        estimatedPositions = new HashMap<Integer, EstimatedPosition>();
    }

    public void clearModel()
    {
        mapping.clear();
        for (int i = 1; i <= 11; i++){
            absolute.put(i,new ArrayList<ObjectAbsolutePosition>());
        }
    }

    public void addPosition(Player p, int markerAbsoluteX, int markerAbsoluteY, double direction, double distance)
    {
        int playerNumber = p.getPlayer().getNumber();
        if (playerNumber < 0) { return; }
        ObjectAbsolutePosition o = new ObjectAbsolutePosition(playerNumber, markerAbsoluteX, markerAbsoluteY, direction, distance);
        ArrayList<ObjectAbsolutePosition> positions = absolute.get(playerNumber);
        positions.add(o);
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

    public void estimatePositions()
    {
        for (Map.Entry<Integer, ArrayList<ObjectAbsolutePosition>> entry : absolute.entrySet()) {
            EstimatedPosition estimatedPosition = new EstimatedPosition(entry.getKey());
            ArrayList<ObjectAbsolutePosition> absolutes = entry.getValue();
            for (int i = 0; i < absolutes.size()-1; i++) {
                for (int j = i+1; j < absolutes.size(); j++) {
                    Point p = triangulate(absolutes.get(i), absolutes.get(j));
                    if (p != null){
                        estimatedPosition.x += p.x;
                        estimatedPosition.y += p.y;
                        estimatedPosition.numberPoints++;
                    }
                }
            }
            if (estimatedPosition.numberPoints > 0) {
                estimatedPosition.x /= estimatedPosition.numberPoints;
                estimatedPosition.y /= estimatedPosition.numberPoints;
                estimatedPosition.y -= Player.PITCH_BOUNDARY_Y_WIDTH/2;
                estimatedPosition.x -= Player.PITCH_BOUNDARY_X_WIDTH/2;
                estimatedPositions.put(estimatedPosition.identifier, estimatedPosition);
            }
        }
        int i = 0;

    }

    private Point triangulate(ObjectAbsolutePosition a, ObjectAbsolutePosition b)
    {
        //First find the distance between the two known points.
        double distance = Math.sqrt(Math.pow((a.markerAbsoluteX - b.markerAbsoluteX), 2) + Math.pow((a.markerAbsoluteY - b.markerAbsoluteY), 2));

        //Use smallest distance for least error.?

        // Use law of cosines c^2 = a^2 + b^2  - 2ab cos(C)

        double leftA = (Math.pow(distance, 2) + Math.pow(b.distance, 2) - Math.pow(a.distance, 2));
        double leftB = (Math.pow(distance, 2) + Math.pow(b.distance, 2) - Math.pow(a.distance, 2));
        double angleA = Math.acos(leftA / (2 * b.distance * distance));
        double angleB = Math.acos(leftB / (2 * a.distance * distance));


        if (a.markerAbsoluteX == b.markerAbsoluteX || a.markerAbsoluteY == b.markerAbsoluteY) {

            Point offset;
            // If points are on the same x line we have all we need
            if (a.markerAbsoluteX == b.markerAbsoluteX) {
                if (a.markerAbsoluteY > b.markerAbsoluteY) {
                    //swap
                    offset = findOffset(angleB, angleA, b.distance, a.distance, distance);
                } else {
                    offset = findOffset(angleA, angleB, a.distance, b.distance, distance);
                }

                if (a.markerAbsoluteX == Player.PITCH_BOUNDARY_X_WIDTH) { //Top line
                    return new Point(a.markerAbsoluteX - offset.y, a.markerAbsoluteY + offset.x);
                }
                if (a.markerAbsoluteX == 0) { //Bottom line
                    return new Point(a.markerAbsoluteX + offset.y, a.markerAbsoluteY + offset.x);
                }
            }
            if (a.markerAbsoluteY == b.markerAbsoluteY) {
                if (a.markerAbsoluteX > b.markerAbsoluteX) {
                    //swap
                    offset = findOffset(angleB, angleA, b.distance, a.distance, distance);
                } else {
                    offset = findOffset(angleA, angleB, a.distance, b.distance, distance);
                }
                if (a.markerAbsoluteY == Player.PITCH_BOUNDARY_Y_WIDTH) { //Top line
                    return new Point(a.markerAbsoluteX + offset.x, a.markerAbsoluteY - offset.y);
                }
                if (a.markerAbsoluteY == 0) { //Bottom line
                    return new Point(a.markerAbsoluteX + offset.x, a.markerAbsoluteY + offset.y);
                }
            }
        }
        //TODO case where on same x line. case where two different lines



        return null;

    }


    private Point findOffset(double angleA, double angleB, double distanceA, double distanceB, double distanceC)
    {
        double yOffset, xOffset;
        if (angleA > Math.PI/2){ //Case 1
            yOffset = Math.sin(Math.PI - angleA) * distanceB;
            xOffset = -Math.cos(Math.PI - angleA) * distanceB;
        } else if (angleB > Math.PI/2){
            yOffset = Math.sin(Math.PI - angleB) * distanceA;
            xOffset = distanceC + Math.cos(Math.PI - angleB) * distanceA;
        } else {
            yOffset = Math.sin(angleA) * distanceB;
            xOffset = Math.cos(Math.PI - angleA) * distanceB;
        }
        return new Point(xOffset, yOffset);
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
        //Lets convert to simple +xy axis when creating points
        this.markerAbsoluteX = markerAbsoluteX + Player.PITCH_BOUNDARY_X_WIDTH/2;
        this.markerAbsoluteY = markerAbsoluteY + Player.PITCH_BOUNDARY_Y_WIDTH/2;
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

class Point {
    public double x;
    public double y;
    public Point(double x, double y){
        this.x = x;
        this.y = y;
    }
}

class EstimatedPosition {

    public int identifier; // (-11 to 11) - other team + our team 0=ball
    public int numberPoints;
    public double totalOffset;
    public double x;
    public double y;

    EstimatedPosition(int identifier)
    {
        totalOffset = 0;
        numberPoints = 0;
        x = 0;
        y = 0;
        this.identifier = identifier;
    }
}