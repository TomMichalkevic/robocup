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
//    private static  Triangle t1 = new Triangle(); //Use static triangles to save compute resources.. oops multithreading

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
                estimatedPosition.y -= Player.BOUNDARY_HEIGHT /2;
                estimatedPosition.x -= Player.BOUNDARY_WIDTH /2;
                estimatedPositions.put(estimatedPosition.identifier, estimatedPosition);
            }
        }
        if (estimatedPositions.size() > 6){
            int i = 0;
        }

    }

    private Point triangulate(ObjectAbsolutePosition posA, ObjectAbsolutePosition posB)
    {
        //First find the distance between the two known points.
        double distanceC = Math.sqrt(Math.pow((posA.markerAbsoluteX - posB.markerAbsoluteX), 2) + Math.pow((posA.markerAbsoluteY - posB.markerAbsoluteY), 2));
        double distanceA = posB.distance;
        double distanceB = posA.distance;
        Triangle t1 = new Triangle();
        t1.sideA = distanceA;
        t1.sideB = distanceB;
        t1.sideC = distanceC;
        Point p = null;
        if (t1.calculate()) { // If we can make a triangle

            double angleA = t1.angleA;
            double angleB = t1.angleB;
            Point offset = null;
            if (posA.markerAbsoluteX == posB.markerAbsoluteX || posA.markerAbsoluteY == posB.markerAbsoluteY) {

                // If points are on the same x line we have all we need
                if (posA.markerAbsoluteX == posB.markerAbsoluteX) {
                    if (posA.markerAbsoluteY > posB.markerAbsoluteY) {
                        //swap
                        offset = findOffsetParallel(angleB, angleA, distanceB, distanceA, distanceC);
                    } else {
                        offset = findOffsetParallel(angleA, angleB, distanceA, distanceB, distanceC);
                    }

                    if (posA.markerAbsoluteX == Player.BOUNDARY_WIDTH) { //Top line
                        p = new Point(posA.markerAbsoluteX - offset.y, posA.markerAbsoluteY + offset.x);
                    }
                    if (posA.markerAbsoluteX == 0) { //Bottom line
                        p = new Point(posA.markerAbsoluteX + offset.y, posA.markerAbsoluteY + offset.x);
                    }
                    //TODO case where on center line
                }
                if (posA.markerAbsoluteY == posB.markerAbsoluteY) {
                    if (posA.markerAbsoluteX > posB.markerAbsoluteX) {
                        //swap
                        offset = findOffsetParallel(angleB, angleA, distanceB, distanceA, distanceC);
                    } else {
                        offset = findOffsetParallel(angleA, angleB, distanceA, distanceB, distanceC);
                    }
                    if (posA.markerAbsoluteY == Player.BOUNDARY_HEIGHT) { //Top line
                        p = new Point(posA.markerAbsoluteX + offset.x, posA.markerAbsoluteY - offset.y);
                    }
                    if (posA.markerAbsoluteY == 0) { //Bottom line
                        p = new Point(posA.markerAbsoluteX + offset.x, posA.markerAbsoluteY + offset.y);
                    }
                }
            } else {
                for (int i = 0; i < 1; i++){ //Easy way to skip code when break
                    //Top left corner
                    if (posA.markerAbsoluteX == 0 && posB.markerAbsoluteY == Player.BOUNDARY_HEIGHT) {
                        offset = findOffsetPerpendicular(angleA, posB.markerAbsoluteX, Player.BOUNDARY_HEIGHT - posA.markerAbsoluteY, distanceB);
                    }
                    if (posB.markerAbsoluteX == 0 && posA.markerAbsoluteY == Player.BOUNDARY_HEIGHT) {
                        //swap
                        offset = findOffsetPerpendicular(angleB, posA.markerAbsoluteX, Player.BOUNDARY_HEIGHT - posB.markerAbsoluteY, distanceA);
                    }
                    if (offset != null) {
                        p = new Point(posA.markerAbsoluteX + offset.y, posA.markerAbsoluteY + offset.x);
                        break;
                    }

                    //Bottom left
                    if (posB.markerAbsoluteX == 0 && posA.markerAbsoluteY == 0) {
                        offset = findOffsetPerpendicular(angleA, posB.markerAbsoluteY, posA.markerAbsoluteX, distanceB);
                    }
                    if (posA.markerAbsoluteX == 0 && posB.markerAbsoluteY == 0) {
                        //swap
                        offset = findOffsetPerpendicular(angleB, posA.markerAbsoluteY, posB.markerAbsoluteX, distanceA);
                    }

                    if (offset != null) {
                        p = new Point(posA.markerAbsoluteX - offset.x, posA.markerAbsoluteY + offset.y);
                        break;
                    }

                    //Bottom right
                    if (posA.markerAbsoluteX == Player.BOUNDARY_WIDTH && posB.markerAbsoluteY == 0) {
                        offset = findOffsetPerpendicular(angleA, Player.BOUNDARY_WIDTH - posB.markerAbsoluteX, posA.markerAbsoluteY, distanceB);
                    }
                    if (posA.markerAbsoluteX == 0 && posB.markerAbsoluteY == Player.BOUNDARY_WIDTH) {
                        //swap
                        offset = findOffsetPerpendicular(angleB, Player.BOUNDARY_WIDTH - posA.markerAbsoluteX, posB.markerAbsoluteY, distanceA);
                    }

                    if (offset != null) {
                        p = new Point(posA.markerAbsoluteX - offset.y, posA.markerAbsoluteY - offset.x);
                        break;
                    }
                    //Top right
                    if (posA.markerAbsoluteY == Player.BOUNDARY_HEIGHT && posB.markerAbsoluteY == Player.BOUNDARY_WIDTH) {
                        offset = findOffsetPerpendicular(angleA, Player.BOUNDARY_WIDTH - posA.markerAbsoluteX, Player.BOUNDARY_HEIGHT - posB.markerAbsoluteY, distanceB);
                    }
                    if (posB.markerAbsoluteY == Player.BOUNDARY_HEIGHT && posA.markerAbsoluteY == Player.BOUNDARY_WIDTH) {
                        //swap
                        offset = findOffsetPerpendicular(angleB, Player.BOUNDARY_WIDTH - posB.markerAbsoluteX, Player.BOUNDARY_HEIGHT - posA.markerAbsoluteY, distanceA);
                    }
                    if (offset != null) {
                        p = new Point(posA.markerAbsoluteX + offset.x, posA.markerAbsoluteY - offset.y);
                        break;
                    }
                }
            }


            if (p != null && (p.x == Double.NaN || p.y == Double.NaN)){
                int j = 0;
            }
        }

        return p;

    }


    private Point findOffsetParallel(double angleA, double angleB, double distanceA, double distanceB, double distanceC)
    {
        //ERROR
        double yOffset, xOffset;
        if (angleA > Math.PI/2){ //Case 1
            yOffset = Math.sin(Math.PI - angleA) * distanceB;
            xOffset = -Math.cos(Math.PI - angleA) * distanceB;
        } else if (angleB > Math.PI/2){ // Case 2
            yOffset = Math.sin(Math.PI - angleB) * distanceA;
            xOffset = distanceC + Math.cos(Math.PI - angleB) * distanceA;
        } else { // Case 3
            yOffset = Math.sin(angleA) * distanceB;
            xOffset = Math.cos(Math.PI - angleA) * distanceB;
        }
        return new Point(xOffset, yOffset);
    }

    private Point findOffsetPerpendicular(double angleA, double sideO, double sideP, double distanceB)
    {
        Triangle t2 = new Triangle();
        t2.sideA = sideO;
        t2.sideB = sideP;
        t2.sideC = t2.sideCFromRightAngledTriangle();
        t2.calculate();
        double yOffset, xOffset;
        if (angleA + t2.angleA < Math.PI/2) { // Case 1
            double angleY = Math.PI - (angleA + t2.angleA);
            yOffset = Math.sin(angleY) * distanceB;
            xOffset = Math.cos(angleY) * distanceB;
        } else { // Case 2
            double angleY = Math.PI - (angleA + t2.angleA);
            yOffset = -Math.sin(angleY) * distanceB;
            xOffset = Math.cos(angleY) * distanceB;
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
        this.markerAbsoluteX = markerAbsoluteX + Player.BOUNDARY_WIDTH /2;
        this.markerAbsoluteY = markerAbsoluteY + Player.BOUNDARY_HEIGHT /2;
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

class Triangle {


    public double sideC = 0;
    public double sideA = 0;
    public double sideB = 0;
    public double angleA = 0;
    public double angleB = 0;
    public double angleC = 0;


    // Have side A and B? Are RA triagnle? Use this method
    public double sideCFromRightAngledTriangle()
    {
        sideC = Math.sqrt(Math.pow(sideA,2) + Math.pow(sideB, 2));
        return sideC;
    }

    public void clear()
    {
        sideC = 0;
        sideA = 0;
        sideB = 0;
        angleA = 0;
        angleB = 0;
        angleC = 0;
    }

    public boolean calculate()
    {
        if (!canMakeTriangle()) {
            return false; // Need at least 3 parts to calculate
        }
        // Triangle inequality theory can be broken with randomised lengths, so check for it.
        // Just give up if no triangle can be made.

        if (numSides() == 3) {
            if (sideA + sideB < sideC ||
                sideA + sideC < sideB ||
                sideB + sideC < sideA) {
                return false;
            }else {
                threeSides();
                return true;
            }
        }
        if (numAngles() == 2) {
            if (sideA == 0) {
                sideA = Math.PI - (sideB + sideC);
            } else if (sideB == 0) {
                sideB = Math.PI - (sideA + sideC);
            } else {
                sideC = Math.PI - (sideA + sideB);
            }
        }

        double x = 0;
        if (angleA > 0 && sideA > 0){
            x = Math.sin(angleA)/sideA;
        } else if (angleB > 0 && sideB > 0){
            x = Math.sin(angleB)/sideB;
        } else if (angleC > 0 && sideC > 0){
            x = Math.sin(angleC)/sideC;
        }

        if (x > 0) {
            if (angleA > 0 && sideA == 0) {
                sideA = x * Math.sin(angleA);
            }

            if (angleB > 0 && sideB == 0) {
                sideB = x * Math.sin(angleB);
            }

            if (angleC > 0 && sideC == 0) {
                sideC = x * Math.sin(angleC);
            }

            // SSA might be two solutions...
            if (sideA > 0 && angleA == 0){
                angleA = Math.asin(sideA/x);
            }
            if (sideC > 0 && angleC == 0){
                angleC = Math.asin(sideC/x);
            }
            if (sideB > 0 && angleB == 0){
                angleB = Math.asin(sideB/x);
            }

        }
        //

        return false;
    }

    private boolean canMakeTriangle()
    {
        return !(numSides() == 0 || numValues() < 3);
    }

    private int numValues()
    {
        return numSides() + numAngles();
    }

    private int numSides()
    {
        return  oneOrNone(sideA) +
                oneOrNone(sideB) +
                oneOrNone(sideC);
    }

    private int numAngles()
    {
        return  oneOrNone(angleA) +
                oneOrNone(angleB) +
                oneOrNone(angleC);
    }

    private int oneOrNone(double value)
    {
        return value > 0 ? 1 : 0;
    }

    private void threeSides()
    {
        double distanceASquared = Math.pow(sideA, 2);
        double distanceBSquared = Math.pow(sideB, 2);
        double distanceCSquared = Math.pow(sideC, 2);

        // Use law of cosines c^2 = a^2 + b^2  - 2ab cos(C)
        angleA = Math.acos(
                ( distanceBSquared + distanceCSquared - distanceASquared )
                                    /
                        (2 * sideB * sideC)
        );

        angleB = Math.acos(
                ( distanceCSquared + distanceASquared - distanceBSquared )
                                    /
                        (2 * sideC * sideA)
        );

        angleC = Math.PI - (angleA + angleB);
    }
}