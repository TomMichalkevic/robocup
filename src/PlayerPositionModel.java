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

    private Point triangulate(ObjectAbsolutePosition posA, ObjectAbsolutePosition posB)
    {
        //First find the distance between the two known points.
        double distanceC = Math.sqrt(Math.pow((posA.markerAbsoluteX - posB.markerAbsoluteX), 2) + Math.pow((posA.markerAbsoluteY - posB.markerAbsoluteY), 2));

        //Use smallest distance for least error.?

        // Use law of cosines c^2 = a^2 + b^2  - 2ab cos(C)
        double distanceA = posB.distance;
        double distanceB = posA.distance;

        double distanceASquared = Math.pow(distanceA, 2);
        double distanceBSquared = Math.pow(distanceB, 2);
        double distanceCSquared = Math.pow(distanceC, 2);



        double angleA = Math.acos(
                ( distanceBSquared + distanceCSquared - distanceASquared )
                                     /
                        (2 * distanceB * distanceC)
        );

        double angleB = Math.acos(
                ( distanceCSquared + distanceASquared - distanceBSquared )
                        /
                        (2 * distanceC * distanceA)
        );

//        double angleC = Math.PI - (angleA + angleB);
        Point p = null;

        if (posA.markerAbsoluteX == posB.markerAbsoluteX || posA.markerAbsoluteY == posB.markerAbsoluteY) {

            Point offset;
            // If points are on the same x line we have all we need
            if (posA.markerAbsoluteX == posB.markerAbsoluteX) {
                if (posA.markerAbsoluteY > posB.markerAbsoluteY) {
                    //swap
                    offset = findOffset(angleB, angleA, distanceB, distanceA, distanceC);
                } else {
                    offset = findOffset(angleA, angleB, distanceA, distanceB, distanceC);
                }

                if (posA.markerAbsoluteX == Player.PITCH_BOUNDARY_X_WIDTH) { //Top line
                    p = new Point(posA.markerAbsoluteX - offset.y, posA.markerAbsoluteY + offset.x);
                }
                if (posA.markerAbsoluteX == 0) { //Bottom line
                    p = new Point(posA.markerAbsoluteX + offset.y, posA.markerAbsoluteY + offset.x);
                }
            }
            if (posA.markerAbsoluteY == posB.markerAbsoluteY) {
                if (posA.markerAbsoluteX > posB.markerAbsoluteX) {
                    //swap
                    offset = findOffset(angleB, angleA, distanceB, distanceA, distanceC);
                } else {
                    offset = findOffset(angleA, angleB, distanceA, distanceB, distanceC);
                }
                if (posA.markerAbsoluteY == Player.PITCH_BOUNDARY_Y_WIDTH) { //Top line
                    p = new Point(posA.markerAbsoluteX + offset.x, posA.markerAbsoluteY - offset.y);
                }
                if (posA.markerAbsoluteY == 0) { //Bottom line
                    p = new Point(posA.markerAbsoluteX + offset.x, posA.markerAbsoluteY + offset.y);
                }
            }
        }
        //TODO case where on same x line. case where two different lines


        if (p.x == Double.NaN) {
            int i = 6;
        }
        return p;

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