import javafx.geometry.Side;

import java.util.*;

/**
 * Created by cmitchelmore on 16/11/14.
 *
 * Our model for the positions of the objects on the field.
 * This should be a SINGLETON
 */
public class PlayerPositionModel {


    private HashMap<String,Point> lastKnownPositions;
    private HashMap<String,Point> knownPositions;
    private HashMap<Integer, ObjectRelativePosition> mapping;
    private ArrayList<Player> unmappedPlayersWithKnownPositions;
    private ArrayList<Player> unmappedPlayersWithUnknownPositions;
    private HashMap<Integer, ArrayList<ObjectAbsolutePosition>> absolute;
    private HashMap<Integer, EstimatedPosition> estimatedPositions;


    /**
     * Init the player position model. absolutes requires an array list for each player for observed positions
     */
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


    /**
     * Clears the currently observer positions of all players. This should be used at the end of each tick.
     * TODO: ### Consider implementing caching in case player data insufficient for this tick ###
     */
    public void clearModel()
    {
        mapping.clear();
        for (int i = 1; i <= 11; i++){
            absolute.put(i,new ArrayList<ObjectAbsolutePosition>());
        }
    }


    /**
     * Save all the sensed objects conveniently in a hash for each player
     *
     * @param p Player (controller player abstract subclass)
     * @param markerAbsoluteX the sensed object's x position in games axis
     * @param markerAbsoluteY the sensed object's y position in games axis
     * @param direction the sensed object's direction in degrees offset from player negative is left
     * @param distance the distance from the player to the object
     */
    public void addPosition(Player p, int markerAbsoluteX, int markerAbsoluteY, double direction, double distance)
    {
        int playerNumber = p.getPlayer().getNumber();
        if (playerNumber < 0) { return; }
        ObjectAbsolutePosition o = new ObjectAbsolutePosition(playerNumber, markerAbsoluteX, markerAbsoluteY, direction, distance);
        ArrayList<ObjectAbsolutePosition> positions = absolute.get(playerNumber);
        positions.add(o);
    }


    /**
     * Add all the observed players both own and other team
     *
     * @param p Player (controller player abstract subclass)
     * @param ownTeam is it our team or other team
     * @param number the number of the player seen
     * @param direction the sensed object's direction in degrees offset from player negative is left
     * @param distance the distance from the player to the object
     */
    public void addPlayer(Player p, boolean ownTeam, int number, double distance, double direction)
    {
        int relation = ownTeam ? ObjectRelativePosition.RELATION_OWN_TEAM : ObjectRelativePosition.RELATION_OTHER_TEAM;
        ObjectRelativePosition o = new ObjectRelativePosition(relation, p.getPlayer().getNumber(), number, distance, direction);
        mapping.put(o.hashCode(),o);
    }


    /**
     * Track ball here as it doesn't fit in to the other methods
     * @param p Player (controller player abstract subclass)
     * @param direction the sensed object's direction in degrees offset from player negative is left
     * @param distance the distance from the player to the object
     */
    public void addBall(Player p, double distance, double direction)
    {
        ObjectRelativePosition o = new ObjectRelativePosition(ObjectRelativePosition.RELATION_BALL, p.getPlayer().getNumber(), 0, direction, distance);
        mapping.put(o.hashCode(),o);
    }


    /**
     * Initiates the calculations to be done using the current sense set.
     * This should only be called ONCE per tick.
     * Use the shared instance of this class to access estimated positions
     * @explanation
     * Loop through each position of fixed points we have for each player.
     * Calculate the position of the player based on every point combined with each other.
     * Positions are calculated using circular intersection.
     * Filter the points down using processPoints to give the best fit directed point for the player.
     * Convert the position back in to the axis and units used by the game.
     * Add the best guess position to estimatedPositions hash
     *
     */
    public void estimatePositions()
    {
        for (Map.Entry<Integer, ArrayList<ObjectAbsolutePosition>> entry : absolute.entrySet()) {
            EstimatedPosition estimatedPosition = new EstimatedPosition(entry.getKey());
            ArrayList<ObjectAbsolutePosition> absolutes = entry.getValue();
            ArrayList<DirectedPoint> possiblePoints = new ArrayList<DirectedPoint>();
            for (int i = 0; i < absolutes.size()-1; i++) {
                for (int j = i+1; j < absolutes.size(); j++) {
                    possiblePoints.addAll(circleBasedTriangulation(absolutes.get(i), absolutes.get(j)));
                }
            }

            if (possiblePoints.size() > 0) {
                DirectedPoint pos = processPoints(possiblePoints);
                estimatedPosition.x = pos.x - Player.BOUNDARY_WIDTH/2;
                estimatedPosition.y = (pos.y - Player.BOUNDARY_HEIGHT/2) * -1; //Correction for reversed lines
                estimatedPosition.absoluteDirection = Math.toDegrees(pos.direction) % 360;
                //Correction for java's funky handling of negative modulus
                estimatedPosition.absoluteDirection = estimatedPosition.absoluteDirection < 0 ? estimatedPosition.absoluteDirection+360 : estimatedPosition.absoluteDirection;
                estimatedPositions.put(estimatedPosition.identifier, estimatedPosition);
            }
        }
    }


    /**
     * Use this method to get the best guess position of the given player number (OWN TEAM only)
     * @param playerNumber the player to get direction of
     * @return an estimated position for the player
     */
    public EstimatedPosition estimatedPlayerPosition(int playerNumber)
    {
        return estimatedPositions.get(playerNumber);
    }


    /**
     * Given an array of directed points find the best fit point.
     * This is quite an extreme cutting because we are using circular intersection it can be that there are 2
     * intersections within the pitch for some combination of points. (The intersections that are out of bounds are
     * eliminated prior to this stage)
     *
     * Sort the points by x; then cut, y; then cut, direction; then cut.
     *
     * Turns out averaging angles is a little tricky when it wraps around 0. Using this formula
     *
     *                   sum_i_from_1_to_N sin(a[i])
     *  a = arctangent ---------------------------
     *                   sum_i_from_1_to_N cos(a[i])
     * @param remainingPoints the points to average
     * @return The average point of the set
     */
    private DirectedPoint processPoints(ArrayList<DirectedPoint> remainingPoints)
    {

        Collections.sort(remainingPoints, new PointXComparator());
        int cut = remainingPoints.size()/10; // Aggressive 10% top and bottom cut off (We can expect a huge range (4-2000) points)
        remainingPoints = new ArrayList<DirectedPoint>(remainingPoints.subList(cut, remainingPoints.size()-cut));
        Collections.sort(remainingPoints, new PointYComparator());
        remainingPoints = new ArrayList<DirectedPoint>(remainingPoints.subList(cut, remainingPoints.size()-cut));
        Collections.sort(remainingPoints, new PointDirectionComparator());
        remainingPoints = new ArrayList<DirectedPoint>(remainingPoints.subList(cut, remainingPoints.size()-cut));
        double xTotal = 0;
        double yTotal = 0;
        double cosT = 0;
        double sinT = 0;

        for (DirectedPoint p : remainingPoints) {
            xTotal+=p.x;
            yTotal+=p.y;
            cosT += Math.cos(p.direction);
            sinT += Math.sin(p.direction);
        }
        return new DirectedPoint(xTotal/remainingPoints.size(), yTotal/remainingPoints.size(), Math.atan2(sinT, cosT));
    }


    /**
     * After trying a basic trig based approach to triangulation I decided to use circular intersection, which turned out
     * to be a lot more straight forward.
     *
     * Create a couple of our own Circle class objects.
     * The circle class has the intersection calculation as a method which returns an array of points.
     * For each point, eliminate it if it's outside the bounds of our pitch.
     * With the left over points calculate the ABSOLUTE angle from the player to the marker (0 is left, 270 is down)
     * Subtract the players 'facing' direction to get the players ABSOLUTE direction on the pitch.
     * TODO: find out why angle has much higher variance than positions
     *
     * @param posA the first position to use
     * @param posB the second position to use
     * @return either 0, 1 or 2 points of intersection given the points and distances
     */
    private ArrayList<DirectedPoint> circleBasedTriangulation(ObjectAbsolutePosition posA, ObjectAbsolutePosition posB)
    {

        Circle a = new Circle(posA.markerAbsoluteX, posA.markerAbsoluteY, posA.distance);
        Circle b = new Circle(posB.markerAbsoluteX, posB.markerAbsoluteY, posB.distance);
        ArrayList<DirectedPoint> points = a.findIntersectionWithCircle(b);

        Triangle t = new Triangle();
        ArrayList<DirectedPoint> remainingPoints = new ArrayList<DirectedPoint>();

        for (DirectedPoint p : points){
            t.clear();
            if (p.x >= 0 && p.y >= 0 && p.x <= Player.BOUNDARY_WIDTH && p.y <= Player.BOUNDARY_HEIGHT) {
                double xDelta = posA.markerAbsoluteX - p.x;
                double yDelta = posA.markerAbsoluteY - p.y;
                t.sideA = Math.abs(xDelta);
                t.sideB = Math.abs(yDelta);
                t.sideC = posA.distance;
                t.calculate();
                double absoluteAngleFromPlayerToMarker;
                if (xDelta > 0 && yDelta < 0) {// Case 1
                    absoluteAngleFromPlayerToMarker = Math.PI + t.angleB;
                }else if (xDelta > 0 && yDelta > 0) {// Case 2
                    absoluteAngleFromPlayerToMarker = Math.PI - t.angleB;
                }else if (xDelta < 0 && yDelta < 0) {// Case 3
                    absoluteAngleFromPlayerToMarker = - t.angleB;
                }else {// Case 4
                    absoluteAngleFromPlayerToMarker = t.angleB;
                }
                p.direction = absoluteAngleFromPlayerToMarker - Math.toRadians(posA.direction); //If negative is counter clockwise
                remainingPoints.add(p);
            }
        }
        return remainingPoints;
    }

 }


/**
 * Helper class to track the absolute positions of sensed fixed objects
 */
class ObjectAbsolutePosition {


    public double markerAbsoluteX;
    public double markerAbsoluteY;
    public int number;
    public double direction;
    public double distance;

    /**
     *
     * @param number the player number that sensed the object
     * @param markerAbsoluteX the absolute x position of the observed object
     * @param markerAbsoluteY the absolute y position of the observed object
     * @param direction the direction of the object from the players center
     * @param distance the distance from the player to the object
     */
    public ObjectAbsolutePosition(int number, int markerAbsoluteX, int markerAbsoluteY, double direction, double distance)
    {
        //NORMALISE THE AXIS. Lazy Lazy Casty Casty!!
        this.markerAbsoluteX = (double)markerAbsoluteX + ((double)Player.BOUNDARY_WIDTH) /2;
        this.markerAbsoluteY = (double)Player.BOUNDARY_HEIGHT - ((double)markerAbsoluteY + ((double)Player.BOUNDARY_HEIGHT) /2);
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


/**
 * Point representation. Built in class was being funny with doubles.
 */
class Point {
    public double x;
    public double y;
    public Point(double x, double y){
        this.x = x;
        this.y = y;
    }

    /**
     * Convenience method for calculating the distance between this point and another
     * @param p the point to find the distance from
     * @return distance from this point to the given point
     */
    public double distanceFromPoint(Point p) 
    {
        return Math.sqrt(Math.pow(x-p.x,2) + Math.pow(y-p.y,2));  
    }
}

/**
 * Extend Point to add direction
 * LEFT is 0 degrees...
 */
class DirectedPoint extends Point {
    public double direction;
    public DirectedPoint(double x, double y, double direction){
        super(x, y);
        this.direction = direction;
    }
}

/**
 * A representation of a dynamic objects position
 *
 * Can be our team or other or the ball.
 * Ball direction is trajectory
 */
class EstimatedPosition {

    public int identifier; // (-11 to 11) - other team + our team 0=ball
    public double totalOffset;
    public double x;
    public double y;
    public double absoluteDirection;

    EstimatedPosition(int identifier)
    {
        totalOffset = 0;
        absoluteDirection = 0;
        x = 0;
        y = 0;
        this.identifier = identifier;
    }
}


/**
 * Allows sorting of points based on Y value
 */
class PointYComparator implements Comparator<Point> {
    @Override
    public int compare(Point o1, Point o2) {
        return Double.compare(o1.y,o2.y);
    }
}


/**
 * Allows sorting of points based on X value
 */
class PointXComparator implements Comparator<Point> {
    @Override
    public int compare(Point o1, Point o2) {
        return Double.compare(o1.x,o2.x);
    }
}


/**
 * Allows sorting of directed points based on direction
 * Yes, this doesn't really make that much sense... but it's used to allow us to trim the range of a given set of angles
 */
class PointDirectionComparator implements Comparator<DirectedPoint> {
    @Override
    public int compare(DirectedPoint o1, DirectedPoint o2) {
        return Double.compare(o1.direction, o2.direction);
    }
}


/**
 * Our custom representation of a circle.
 * Used to encapsulate the logic for intersection calculations
 *
 * Used with observed positions where center is position of the object and radius is the distance to the observing player
 */
class Circle {
    
    public double radius;
    public Point center;


    /**
     *
     * @param x center x pos
     * @param y center y pos
     * @param radius radius of the circle
     */
    public Circle(double x, double y, double radius)
    {
        center = new Point(x,y);
        this.radius = radius;
    }

    /**
     * Find the intersection of this circle and another. The full explanation of the equation can be found here:
     * http://paulbourke.net/geometry/circlesphere/
     *
     * @param circle2
     * @return an array of (1,2 or 3)points of intersection (this could have been a regular point but it saves creating
     * extra objects when converting. Just set direction to 0)
     */
    public ArrayList<DirectedPoint> findIntersectionWithCircle(Circle circle2)
    {
        //Calculate distance between centres of circle
        ArrayList<DirectedPoint> points = new ArrayList<DirectedPoint>();
        double distanceBetweenCenters = center.distanceFromPoint(circle2.center);
  
        double m = radius + circle2.radius;
        double n = radius - circle2.radius;

        if (n < 0) {
            n = n * -1;
        }
        
        //No points of intersection (circles don't touch or are contained within each other)
        if (distanceBetweenCenters > m || distanceBetweenCenters < n) {
            return  points;
        }

        
        //Circle is the same as this one (Not going to happen in our use case)
        if (distanceBetweenCenters == 0 && radius == circle2.radius){
            return  points;
        }

        // Intersection calculation
        double a = (Math.pow(radius,2) - Math.pow(circle2.radius, 2) + Math.pow(distanceBetweenCenters,2)) / (2 * distanceBetweenCenters);
        double h = Math.sqrt(radius * radius - a * a);

        DirectedPoint p = new DirectedPoint(center.x + (a /distanceBetweenCenters) * (circle2.center.x -center.x),
                                            center.y + (a /distanceBetweenCenters) * (circle2.center.y -center.y), 0);
        points.add(p);

        //Circles touch exactly once (unlikely...)
        if (distanceBetweenCenters == radius + circle2.radius) {
            return points;
        }

        DirectedPoint p1 = new DirectedPoint(p.x + (h /distanceBetweenCenters) * (circle2.center.y - center.y),
                             p.y - (h /distanceBetweenCenters) * (circle2.center.x - center.x), 0);

        DirectedPoint p2 = new DirectedPoint(p.x - (h /distanceBetweenCenters) * (circle2.center.y - center.y),
                             p.y + ( h /distanceBetweenCenters) * (circle2.center.x - center.x), 0);

        points.clear();
        points.add(p1);
        points.add(p2);
        return points;
    }

}


/**
 * Convenience class for handling triangle logic.
 * Given 3 bits of information (including at least 1 side) we can calculate everything else about the triangle.
 */
class Triangle {


    public double sideC = 0;
    public double sideA = 0;
    public double sideB = 0;
    public double angleA = 0;
    public double angleB = 0;
    public double angleC = 0;


    /**
     * Have side A and B?
     * Are you SURE the triangle is a right angled triangle? Then use this method
     * @return side C using pythagoras' theorum
     */
    public double sideCFromRightAngledTriangle()
    {
        sideC = Math.sqrt(Math.pow(sideA,2) + Math.pow(sideB, 2));
        return sideC;
    }


    /**
     * If using for calculations in a loop, save creating new objects by clearing and reusing this one.
     */
    public void clear()
    {
        sideC = 0;
        sideA = 0;
        sideB = 0;
        angleA = 0;
        angleB = 0;
        angleC = 0;
    }


    /**
     * Once you have set information about the triangle using the public properties then ask the triangle to calculate
     * everything else.
     *
     * @return true if a triangle can be made with the given information
     */
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


    /**
     * Helper method for triangle calculations
     * @return can we make a triangle?
     */
    private boolean canMakeTriangle()
    {
        return !(numSides() == 0 || numValues() < 3);
    }


    /**
     * Helper method for triangle calculations
     * @return how many values we have
     */
    private int numValues()
    {
        return numSides() + numAngles();
    }


    /**
     * Helper method for triangle calculations
     * @return how many sides there are
     */
    private int numSides()
    {
        return  oneOrNone(sideA) +
                oneOrNone(sideB) +
                oneOrNone(sideC);
    }


    /**
     * Helper method for triangle calculations
     * @return how many angles we have
     */
    private int numAngles()
    {
        return  oneOrNone(angleA) +
                oneOrNone(angleB) +
                oneOrNone(angleC);
    }


    /**
     * Helper method for triangle calculations
     * @return 1 if the value is greater than 0 else 0
     */
    private int oneOrNone(double value)
    {
        return value > 0 ? 1 : 0;
    }


    /**
     * Law of the cosines. Called by the calculate method, if we have 3 sides, to solve the angles.
     */
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