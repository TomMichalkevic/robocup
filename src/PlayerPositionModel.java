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
        int team = ownTeam ? 1 : -1;
        ObjectRelativePosition o = new ObjectRelativePosition(p.getPlayer().getNumber(), number * team, distance, direction);
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
        ObjectRelativePosition o = new ObjectRelativePosition(p.getPlayer().getNumber(), 0, direction, distance);
        mapping.put(o.hashCode(),o);
    }


    /**
     * Filter the list of estimated positions with the given parameters.
     *
     * @param p Player (controller player abstract subclass)
     * @param type 0 = all, 1 = own team, 2 = other team, 3 = ball
     * @param xGreaterThan absolute x position should be greater than (pitch axis) Use -Player.LARGE_DISTANCE for all
     * @param yGreaterThan absolute y position should be greater than (pitch axis) Use -Player.LARGE_DISTANCE for all
     * @param xLessThan absolute x position should be less than (pitch axis) Use Player.LARGE_DISTANCE for all
     * @param yLessThan absolute y position should be less than (pitch axis) Use Player.LARGE_DISTANCE for all
     * @param maxDistance the maximum distance from the given player use Player.LARGE_DISTANCE for all
     * @param minDistance the minimum distance from the given player use 0 for all
     * @param sort what to sort the filtered objects by
     * @return A filtered array list of positions
     */
    public ArrayList<EstimatedPosition> filterObjects(Player p, int type, double xGreaterThan, double yGreaterThan, double xLessThan, double yLessThan, double maxDistance, double minDistance, String sort)
    {
        ArrayList<EstimatedPosition> filteredObjects = new ArrayList<EstimatedPosition>();
        for (Map.Entry<Integer, EstimatedPosition> entry : estimatedPositions.entrySet()) {
            int identifier = entry.getKey();
            int playerNumber = p.getPlayer().getNumber();
            if (playerNumber != identifier) {
                if (type == 0 || (identifier == 0 && type == 3) || (identifier < 0 && type == 2) || (identifier > 0 && type == 1)) {
                    EstimatedPosition position = entry.getValue();
                    if (position.x > xGreaterThan && position.y > yGreaterThan && position.x < xLessThan && position.y < yLessThan) {
                        EstimatedPosition player = estimatedPlayerPosition(playerNumber);
                        double distance = Math.sqrt(Math.pow(position.x - player.x,2) + Math.pow(position.y - player.y, 2));
                        if (distance < maxDistance && distance > minDistance) {
                            if (sort == "distance") {
                                position.sortValue = distance;
                            }
                            filteredObjects.add(position);
                        }
                    }
                }
            }
        }
        if (sort != "") {
            Collections.sort(filteredObjects, new EstimatedPositionComparator());
        }
        return filteredObjects;
    }


    /**
     * Initiates the calculations to be done using the current sense set.
     * This should only be called ONCE per tick.
     * Use the shared instance of this class to access estimated positions
     *
     * Loop through each position of fixed points we have for each player.
     * Calculate the position of the player based on every point combined with each other.
     * Positions are calculated using circular intersection.
     * Filter the points down using processPoints to give the best fit directed point for the player.
     * Convert the position back in to the axis and units used by the game.
     * Add the best guess position to estimatedPositions hash.
     *
     * Once we have positions of our own team we can use these to find the positions of the other objects relative
     * to our players.
     * Start by finding out where the ball is.
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

                estimatedPosition.absoluteDirection = realMod(estimatedPosition.absoluteDirection);
                estimatedPositions.put(estimatedPosition.identifier, estimatedPosition);
            }
        }
        findTheBall();
    }

    /**
     * Correction for java's funky handling of negative modulus
     * @param degrees
     * @return
     */
    private double realMod(double degrees)
    {
        return degrees < 0 ? degrees+360 : degrees;
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
     * Use this method to get the best guess position of the ball
     * @return an estimated position for the ball
     */
    public EstimatedPosition estimatedBallPosition()
    {
        return estimatedPositions.get(0);
    }


    /**
     * Find the direction to turn so that the player is facing away form the given object.
     * Make sure we use the shortest distance left or right
     * @param player the player that will turn
     * @param estimatedPosition the object that the player should turn away from
     * @return the direction the player should turn
     */
    public double turnDirectionForOppositeDirectionFrom(Player player, EstimatedPosition estimatedPosition)
    {
        EstimatedPosition playerEstimatedPosition = estimatedPlayerPosition(player.getPlayer().getNumber());
        int hashCode = ObjectRelativePosition.codeFor(player.getPlayer().getNumber(), estimatedPosition.identifier);
        ObjectRelativePosition relation = mapping.get(hashCode);


        if (relation != null) {
            double turn = 180 - Math.abs(relation.direction);
            return relation.direction < 0 ? turn : -turn;
        } else {
            Triangle t = new Triangle();
            t.sideA = Math.abs(playerEstimatedPosition.y - estimatedPosition.y);
            t.sideB = Math.abs(playerEstimatedPosition.x - estimatedPosition.x);
            t.sideCFromRightAngledTriangle();
            t.calculate();
            double absoluteOppositeDir = 0;
            if (playerEstimatedPosition.x > estimatedPosition.x &&
                playerEstimatedPosition.y < estimatedPosition.y) { //Case 1
                absoluteOppositeDir =  t.angleA + 180;
            } else if (playerEstimatedPosition.x < estimatedPosition.x &&
                       playerEstimatedPosition.y < estimatedPosition.y) { //Case 2
                absoluteOppositeDir =  (180 - t.angleA) + 180;
            } else if (playerEstimatedPosition.x < estimatedPosition.x &&
                        playerEstimatedPosition.y > estimatedPosition.y) { //Case 3
                absoluteOppositeDir =  t.angleA;
            } else if (playerEstimatedPosition.x > estimatedPosition.x &&
                       playerEstimatedPosition.y > estimatedPosition.y) { //Case 4
                absoluteOppositeDir =  (360 - t.angleA) - 180;
            }
            double a = playerEstimatedPosition.absoluteDirection - absoluteOppositeDir;

            if (a < 0) {
                a = realMod(a);
            }
            double right = 360 - a;
            return right < a ? right : -a;

        }
    }


    /**
     * Find the direction to turn so that the player is facing towards the given object.
     * Make sure we use the shortest distance left or right
     * @param player the player that will turn
     * @param estimatedPosition the object that the player should turn towards
     * @return the direction the player should turn
     */
    public double turnDirectionToFacePosition(Player player, EstimatedPosition estimatedPosition)
    {
        EstimatedPosition playerEstimatedPosition = estimatedPlayerPosition(player.getPlayer().getNumber());
        int hashCode = ObjectRelativePosition.codeFor(player.getPlayer().getNumber(), estimatedPosition.identifier);
        ObjectRelativePosition relation = mapping.get(hashCode);

        if (relation != null) {
            return relation.direction;
        } else {
            Triangle t = new Triangle();
            t.sideA = Math.abs(playerEstimatedPosition.y - estimatedPosition.y);
            t.sideB = Math.abs(playerEstimatedPosition.x - estimatedPosition.x);
            t.sideCFromRightAngledTriangle();
            t.calculate();
            double absoluteOppositeDir = 0;
            if (playerEstimatedPosition.x > estimatedPosition.x &&
                    playerEstimatedPosition.y < estimatedPosition.y) { //Case 1
                absoluteOppositeDir =  t.angleA;
            } else if (playerEstimatedPosition.x < estimatedPosition.x &&
                    playerEstimatedPosition.y < estimatedPosition.y) { //Case 2
                absoluteOppositeDir =  180 - t.angleA;
            } else if (playerEstimatedPosition.x < estimatedPosition.x &&
                    playerEstimatedPosition.y > estimatedPosition.y) { //Case 3
                absoluteOppositeDir =  180 + t.angleA;
            } else if (playerEstimatedPosition.x > estimatedPosition.x &&
                    playerEstimatedPosition.y > estimatedPosition.y) { //Case 4
                absoluteOppositeDir = 360 - t.angleA;
            }
            double a = playerEstimatedPosition.absoluteDirection - absoluteOppositeDir;

            if (a < 0) {
                a = realMod(a);
            }
            double right = 360 - a;
            return right < a ? right : -a;

        }
    }


    /**
     * Check if the player is in between the goal and the ball
     * @param player the player to test (OUR TEAM)
     * @return if the player is sufficiently in between the ball and the goal
     */
    protected boolean inBetweenOurGoalAndBall(Player player)
    {
        EstimatedPosition ballPos = estimatedBallPosition();
        EstimatedPosition playerPos = estimatedPlayerPosition(player.getPlayer().getNumber());
        if (ballPos != null && playerPos != null) {
            Point A = new Point(-(Player.BOUNDARY_WIDTH/2 - Player.DISTANCE_PITCH_EDGE_TO_BOUNDARY), 0);
            Point B = ballPos.centerAndDir();
            Point P = playerPos.centerAndDir();

            double normalLength = Math.sqrt((B.x-A.x)*(B.x-A.x)+(B.y-A.y)*(B.y-A.y));
            return Math.abs((P.x-A.x)*(B.y-A.y)-(P.y-A.y)*(B.x-A.x))/normalLength < Player.INTERCEPTION_MAX_DISTANCE;
        }
        return false;
    }


    /**
     * @return A point one third of the way from our goal to the ball
     */
    public Point pointBetweenBallAndOurGoal()
    {
        double ourGoalCenterX = -(Player.BOUNDARY_WIDTH/2 - Player.DISTANCE_PITCH_EDGE_TO_BOUNDARY);
        EstimatedPosition ball = estimatedBallPosition();
        return new Point(ourGoalCenterX + (ball.x - ourGoalCenterX) / 3.0, ball.y /3.0);
    }


    /**
     * ### Must be called after we have player positions ###
     *
     * Use all the players that we have a position for and have seen the ball
     * Get an estimate of where the ball is from each player
     * Average the values and assign the estimated position to the positions hash
     */
    private void findTheBall()
    {
        ArrayList<Point> ballPoints = new ArrayList<Point>();
        for (Map.Entry<Integer, EstimatedPosition> entry : estimatedPositions.entrySet()) {
            ObjectRelativePosition position = mapping.get(ObjectRelativePosition.codeFor(entry.getKey(), 0));
            if (position != null){
                EstimatedPosition observer = entry.getValue();
                double absoluteBallDirectionFromObserver = observer.absoluteDirection + position.direction;
                Point ball = null;
                double ballDir;
                if (absoluteBallDirectionFromObserver <= 90) {
                    ballDir = absoluteBallDirectionFromObserver;
                    ball = new Point(observer.x - position.distance * Math.cos(Math.toRadians(ballDir)),
                                     observer.y - position.distance * Math.sin(Math.toRadians(ballDir)));
                } else if (absoluteBallDirectionFromObserver <= 180) {
                    ballDir = absoluteBallDirectionFromObserver - 90;
                    ball = new Point(observer.x + position.distance * Math.sin(Math.toRadians(ballDir)),
                                     observer.y - position.distance * Math.cos(Math.toRadians(ballDir)));
                } else if (absoluteBallDirectionFromObserver <= 270) {
                    ballDir = absoluteBallDirectionFromObserver - 180;
                    ball = new Point(observer.x + position.distance * Math.cos(Math.toRadians(ballDir)),
                                     observer.y + position.distance * Math.sin(Math.toRadians(ballDir)));
                } else if (absoluteBallDirectionFromObserver <= 360) {
                    ballDir = absoluteBallDirectionFromObserver - 270;
                    ball = new Point(observer.x - position.distance * Math.sin(Math.toRadians(ballDir)),
                                     observer.y + position.distance * Math.cos(Math.toRadians(ballDir)));
                }
                if (ball != null) {
                    ballPoints.add(ball);
                }
            }
        }

        double xTotal = 0;
        double yTotal = 0;
        for (Point point : ballPoints) {
            xTotal += point.x;
            yTotal += point.y;
        }
        EstimatedPosition estimatedPosition = new EstimatedPosition(0);
        estimatedPosition.x = xTotal / ballPoints.size();
        estimatedPosition.x = yTotal / ballPoints.size();
        estimatedPositions.put(0, estimatedPosition);
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

    
    
    public int fromIdentifier;
    public int toIdentifier;
    
    public double direction;
    public double distance;
    
    public ObjectRelativePosition(int fromIdentifier, int toIdentifier, double direction, double distance)
    {
        this.fromIdentifier = fromIdentifier;
        this.toIdentifier = toIdentifier;
        this.direction = direction;
        this.distance = distance;
    }
    
    public int hashCode()
    {
        return fromIdentifier * 1000 + toIdentifier;
    }

    public static int codeFor(int fromIdentifier, int toIdentifier)
    {
        return fromIdentifier * 1000 + toIdentifier;
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

class EstimatedPositionComparator implements Comparator<EstimatedPosition> {
    @Override
    public int compare(EstimatedPosition o1, EstimatedPosition o2) {
        return Double.compare(o1.sortValue, o2.sortValue);
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
    public double sortValue;

    EstimatedPosition(int identifier)
    {
        totalOffset = 0;
        absoluteDirection = 0;
        x = 0;
        y = 0;
        this.identifier = identifier;
    }

    public double distanceToEstimatedPosition(EstimatedPosition estimatedPosition)
    {
        return Math.sqrt(Math.pow(x-estimatedPosition.x,2) + Math.pow(y-estimatedPosition.y,2));
    }

    public DirectedPoint centerAndDir()
    {
        return new DirectedPoint(x,y,absoluteDirection);
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