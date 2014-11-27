import com.github.robocup_atan.atan.model.ActionsPlayer;
import com.github.robocup_atan.atan.model.ControllerPlayer;
import com.github.robocup_atan.atan.model.enums.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


/**
 * Created by cmitchelmore on 13/11/14.
 */
public abstract class Player implements ControllerPlayer {


    /**
     * ########################################################################################
     * ###########################          Constants        ##################################
     * ########################################################################################
     */
    public static final int
            BOUNDARY_WIDTH = 115, // The width of the pitch boundary
            BOUNDARY_HEIGHT = 78, // The height of the pitch boundary. It's key that these values be accurate for the position model to work
            LARGE_DISTANCE = 1000, // Use as a constant for large values when searching or viewing
            PENALTY_DISTANCE_FROM_CENTER = 43,
            DISTANCE_PITCH_EDGE_TO_BOUNDARY = 5,
            BALL_CROWDING_RANGE = 5,
            PLAYER_CROWDING_RANGE = 3,
            HOLDING_POSITION_RADIUS = 3; //Hard to work out EXACTLY where they are so allow some flexibility in reaching the position
    /**
     * Define a constant for each player position for readability.
     */
    public static final int
            GOALIE = 1,
            LEFT_BACK = 2,
            CENTER_LEFT_BACK = 3,
            CENTER_RIGHT_BACK = 4,
            RIGHT_BACK = 5,
            LEFT_WING = 6,
            CENTER_LEFT_MIDFIELD = 7,
            CENTER_RIGHT_MIDFIELD = 8,
            RIGHT_WING = 9,
            CENTER_LEFT_FORWARD = 10,
            CENTER_RIGHT_FORWARD = 11;

    protected static final double
            BALL_WITHIN_REACH = 0.7,
            BALL_VERY_CLOSE = 2,
            BALL_CLOSE = 10,
            STRIKER_SHOOTING_RANGE = 12.0,
            MIDFIELDER_SHOOTING_RANGE = 22.0,
            TOTAL_HALF_TICKS = 3000.0;

    protected static final int
            DRIBBLE_POWER = 6,
            LONG_DRIBBLE_POWER = 6,
            CLEARANCE_POWER = 50,
            BASE_SHOOT_POWER = 50,
            GOAL_AGGRESSION_CHANGE = 5,
            PROGRESS_AGGRESSION_CHANGE = 1,
            PROGRESS_AGGRESSION_FREQUENCY = 5;

    /*
     * ########################################################################################
     */


    /**
     * Aggression defines the teams overall player style. If the team is losing Aggression increases and if they are
     * winning it decreases. Aggression can be read as attack risk.
     *
     * Also keep track off score and elapsed ticks (once for all players)
     */
    private static int
            Aggression = 65,
            GoalsOwn = 0,
            GoalsOther = 0,
            TickCount = 0;



    protected double
            distanceToBall = 1000,
            directionToBall = 0,
            directionOwnGoal = 0,
            distanceOwnGoal = -1.0,
            directionOtherGoal = 0,
            distanceOtherGoal = -1.0,
            directionMultiplier = 1.0,
            distanceClosestForwardOwnPlayer = -1,
            directionClosestForwardOwnPlayer = -1,
            distanceClosestForwardOtherPlayer = -1,
            directionClosestForwardOtherPlayer = -1,
            ownGoalTurn,
            otherGoalTurn,
            playerRemainingStamina = 0;

    protected boolean canSeeOwnGoal = false,
            canSeeGoalLeft = false,
            canSeeGoalRight = false,
            canSeeFieldEnd = false,
            alreadySeeingOwnGoal = false,
            alreadySeeingOtherGoal = false,
            canSeeOtherGoal = false,
            canSeeOwnPenalty = false,
            needsToRetreat = false,
            haveSeenSomeMarker = true;

    protected ActionsPlayer player;
    protected Random random        = null;
    protected static int count = 0, ballPositionOffset = 0;
    protected String playerType = "";
    private static PlayerPositionModel playerPositionModel = null;


    public Player()
    {
        random = new Random(System.currentTimeMillis() + count);
        if (playerPositionModel == null) {
            playerPositionModel = new PlayerPositionModel();
        }
        count++;
    }

    public int getAggression()
    {
        return Player.Aggression;
    }

    public void setAggression(int aggression)
    {
        Player.Aggression = aggression;
    }


    /** {@inheritDoc} */
    @Override
    public void preInfo() {
        distanceToBall = 1000;
        distanceOwnGoal = 1000;
        directionOwnGoal = 0;
        distanceOtherGoal = 1000;
        directionOtherGoal = 0;
        canSeeOwnGoal = false;
        canSeeGoalLeft = false;
        canSeeGoalRight = false;
        canSeeOwnPenalty = false;
        canSeeFieldEnd = false;
        ownGoalTurn = 0.0;
        // At the start of a tick clear the positional model
        // This should only happen once for the whole team and only one player is player 1 so it's an easy test
        if (getPlayer().getNumber() == 1) {
            playerPositionModel.clearModel();
            TickCount++;

            if (TickCount == 2999) {
                Object h = stats;
            }
            if ((halfProgress() * 100) % PROGRESS_AGGRESSION_FREQUENCY == 0){
                setAggression(getAggression() + PROGRESS_AGGRESSION_CHANGE); //Make this faster if losing?
            }
        }

    }


    @Override
    public void postInfo()
    {
        playerPositionModel.estimatePositions();
//        moveToHoldingPosition();;

        if (distanceToBall <= Player.BALL_WITHIN_REACH) {
            playerHasBallAction();
        }else if (distanceToBall <= Player.BALL_VERY_CLOSE) {
            ballIsVeryCloseAction();
        }else if (distanceToBall <= Player.BALL_CLOSE) {
            ballIsCloseAction();
        }else if (distanceToBall < Player.LARGE_DISTANCE){
            ballIsFarAction();
        }else {
            ballNotVisibleAction();
        }
    }


    /**
     * Override this method in subclass for the action to take when a player has the ball
     */
    protected abstract void playerHasBallAction();


    /**
     * Override this method in subclass for the action to take when a player is within BALL_VERY_CLOSE distance
     */
    protected abstract void ballIsVeryCloseAction();


    /**
     * Override this method in subclass for the action to take when a player is within BALL_CLOSE distance
     */
    protected abstract void ballIsCloseAction();


    /**
     * Override this method in subclass for the action to take when a player is within LARGE_DISTANCE distance.
     * e.g. the ball can we seen
     */
    protected abstract void ballIsFarAction();


    /**
     * Called when player can't sense the ball
     */
    protected void ballNotVisibleAction()
    {
        moveToHoldingPosition();
    }


    /**
     * Called to move the player back to his holding position.
     * Holding position is based on the current aggression of the team
     */
    protected abstract void moveToHoldingPosition();


    /**
     * Ask the player to look around. We turn them by 45 degrees.
     */
    protected void lookAround()
    {
       getPlayer().turn(45);
    }


    /**
     * @return the estimated position of this player
     */
    protected EstimatedPosition playerPosition()
    {
        return playerPositionModel.estimatedPlayerPosition(getPlayer().getNumber());
    }


    /**
     * The shortest direction towards own goal calculated from our directions or fall back to original calculation
     * @return direction to our goal
     */
    protected double directionToOwnGoal()
    {
        if (playerPosition() != null) {
            double toLeft = -playerPosition().absoluteDirection;
            double toRight = 360 - playerPosition().absoluteDirection;
            return Math.abs(toLeft) < toRight ? toLeft : toRight;
        }
        return directionOwnGoal;
    }


    /**
     * The shortest direction towards other goal calculated from our directions or fall back to original calculation
     * @return direction to other goal
     */
    protected double directionToOtherGoal()
    {
        if (playerPosition() != null) {
            return 180 - playerPosition().absoluteDirection;
        }
        return directionOtherGoal;
    }


    /**
     * @return is there any players close in front
     */
    protected boolean areNoCloseForwardPlayers()
    {
        if (playerPosition() != null) {
            return playerPositionModel.filterObjects(this, 0, playerPosition().x, -Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, 5, 0, "").size() == 0;
        }
        return false;
    }
    /**
     *
     * @return is there a visible player on our team in front of this player
     */
    protected boolean isFowardOwnPlayer()
    {
        if (playerPosition() != null) {
            return playerPositionModel.filterObjects(this, 1, playerPosition().x, -Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, 5, 0, "").size() > 0;
        }
        return false;
    }

    /**
     *
     * @return true if the ball is in between the player and other goal. False if not or not known.
     */
    protected boolean isBallOtherGoalSideOfPlayer()
    {
        if (playerPosition() != null) {
            return playerPositionModel.filterObjects(this, 3, playerPosition().x, -Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, 0, "").size() > 0;
        }
        return false;
    }

    /**
     *
     * @return true if the ball is in between the player and own goal. False if not or not known.
     */
    protected boolean isBallOwnGoalSideOfPlayer()
    {
        if (playerPosition() != null) {
            return playerPositionModel.filterObjects(this, 3, -Player.LARGE_DISTANCE, -Player.LARGE_DISTANCE, playerPosition().x, Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, 0, "").size() > 0;
        }
        return false;
    }

    /**
     * Percentage of time elapsed in half
     * @return a value between 0 and 1 representing the progress of the half
     */
    protected static double halfProgress()
    {
        return TickCount / TOTAL_HALF_TICKS;
    }


    /**
     * Filters the estimated positions of our players to leave only those within range of the ball.
     * Used to stop our team crowding round the ball
     * @param range the maximum range of players from the ball
     * @return the number of players within range of the ball
     */
    protected int numberOfOurPlayersWithRangeOfBall(double range)
    {
        if (playerPosition() != null) {
            return playerPositionModel.filterObjects(this, 3, -Player.LARGE_DISTANCE, -Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, range, 0, "").size();
        }
        return -1;
    }


    /**
     * Filters the estimated positions of our players to leave only those within range of the this player.
     * Used to stop our team bunching up with each other
     * @param range the maximum range of players from this player
     * @return the number of players within range of this player
     */
    protected int numberOfOurPlayersWithRangeOfMe(double range)
    {
        if (playerPosition() != null) {
            return playerPositionModel.filterObjects(this, 1, -Player.LARGE_DISTANCE, -Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, range, 0, "").size();
        }
        return -1;
    }


    /**
     * Used to help our players spread out
     * @param position the object we want to face away from
     * @return the direction we should turn to face away from the given object
     */
    protected double oppositeDirectionTo(EstimatedPosition position)
    {
        return playerPositionModel.turnDirectionForOppositeDirectionFrom(this, position);
    }


    /**
     * Filter the positions of our team mates and return the closest
     * @return the closest team mate
     */
    protected EstimatedPosition closestTeamMember()
    {
        if (playerPosition() != null) {
            ArrayList<EstimatedPosition> filteredTeamMates = playerPositionModel.filterObjects(this, 1, -Player.LARGE_DISTANCE, -Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, Player.LARGE_DISTANCE, Player.PLAYER_CROWDING_RANGE, 0, "distance");
            if (filteredTeamMates.size() > 0) {
                return filteredTeamMates.get(0);
            }
        }
        return null;
    }


    /**
     * Move the player towards the given position if possible
     *
     * @param x the destination x position
     * @param y the destination y position
     */
    protected void moveToPosition(int x, int y)
    {
        EstimatedPosition estimatedPosition =  playerPositionModel.estimatedPlayerPosition(getPlayer().getNumber());
        if (estimatedPosition == null){ // If we don't have a position move a random direction
            getPlayer().turn(90);
            getPlayer().dash(dashValueSlow());
            return;
        }
        Triangle t = new Triangle();
        double xDelta = x - estimatedPosition.x;
        double yDelta = y - estimatedPosition.y;
        t.sideA = Math.abs(xDelta);
        t.sideB = Math.abs(yDelta);
        double distance = t.sideCFromRightAngledTriangle();
        if (distance < HOLDING_POSITION_RADIUS) {
            lookAround();
        } else {
            t.calculate();

            double absoluteTargetAngle;

            if (xDelta < 0 && yDelta < 0) {// Case 1
                absoluteTargetAngle = Math.PI/2 - t.angleA;
            }else if (xDelta > 0 && yDelta < 0) {// Case 2
                absoluteTargetAngle = Math.PI/2 + t.angleA;
            }else if (xDelta > 0 && yDelta > 0) {// Case 3
                absoluteTargetAngle = 3*Math.PI/2 - t.angleA;
            }else {// Case 4
                absoluteTargetAngle = 3*Math.PI/2 + t.angleA;
            }
            double absoluteTargetAngleDegrees = Math.toDegrees(absoluteTargetAngle) % 360;
            absoluteTargetAngleDegrees = absoluteTargetAngleDegrees < 0 ? absoluteTargetAngleDegrees + 360 : absoluteTargetAngleDegrees;
            double relativePlayerAngle = absoluteTargetAngleDegrees  - estimatedPosition.absoluteDirection;
            getPlayer().turn(relativePlayerAngle);
            getPlayer().dash(dashValueSlow());
        }

    }


    /**
     * @param player the number of the player
     * Method to move players to their starting positions based on their number.
     * move is x,y looking at the pitch with -x left and -y up
     */
    public void moveToStartingPositions(int player)
    {
        switch (player) {
            case GOALIE :
                this.getPlayer().move(-50, 0);
                break;
            case LEFT_BACK :
                this.getPlayer().move(-40, -20);
                break;
            case CENTER_LEFT_BACK :
                this.getPlayer().move(-40, -8);
                break;
            case CENTER_RIGHT_BACK :
                this.getPlayer().move(-40, 8);
                break;
            case RIGHT_BACK :
                this.getPlayer().move(-40, 20);
                break;
            case LEFT_WING :
                this.getPlayer().move(-20, -20);
                break;
            case CENTER_LEFT_MIDFIELD :
                this.getPlayer().move(-20, -8);
                break;
            case CENTER_RIGHT_MIDFIELD :
                this.getPlayer().move(-20, 8);
                break;
            case RIGHT_WING :
                this.getPlayer().move(-20, 20);
                break;
            case CENTER_LEFT_FORWARD :
                this.getPlayer().move(-5, -5);
                break;
            case CENTER_RIGHT_FORWARD :
                this.getPlayer().move(-5, 5);
        }

    }


    /**
     * @param player the number of the player
     * Method to move players to their kick off positions based on their number.
     * move is x,y looking at the pitch with -x left and -y up
     */
    public void moveToKickoffPositions(int player)
    {
        switch (player) {
            case GOALIE :
                this.getPlayer().move(-50, 0);
                break;
            case LEFT_BACK :
                this.getPlayer().move(-30, -30);
                break;
            case CENTER_LEFT_BACK :
                this.getPlayer().move(-30, -8);
                break;
            case CENTER_RIGHT_BACK :
                this.getPlayer().move(-30, 8);
                break;
            case RIGHT_BACK :
                this.getPlayer().move(-20, 20);
                break;
            case LEFT_WING :
                this.getPlayer().move(0, -20);
                break;
            case CENTER_LEFT_MIDFIELD :
                this.getPlayer().move(-5, -8);
                break;
            case CENTER_RIGHT_MIDFIELD :
                this.getPlayer().move(-5, 8);
                break;
            case RIGHT_WING :
                this.getPlayer().move(0, 20);
                break;
            case CENTER_LEFT_FORWARD :
                this.getPlayer().move(0, -1);
                break;
            case CENTER_RIGHT_FORWARD :
                this.getPlayer().move(0, 1);
        }

    }


    /**
     * Dash value should be faster if more stamina is left and the aggression is higher
     * @return A higher speed dash value
     */
    protected int dashValueVeryFast()
    {
        int initialPower = getAggression() + 50;
        return staminaBoost(initialPower);
    }

    /**
     * Dash value should be faster if more stamina is left and the aggression is higher
     * @return A high speed dash value
     */
    protected int dashValueFast()
    {
        int initialPower = getAggression() + 10;
        return staminaBoost(initialPower);
    }

    /**
     * Dash value should be faster if more stamina is left and the aggression is higher
     * @return A low speed dash value
     */
    protected int dashValueSlow()
    {
        int initialPower = getAggression() - 30;
        return staminaBoost(initialPower);
    }


    /**
     * If we have plenty of stamina and the half is finishing we can move faster
     * @param initialPower the power before factoring in stamina
     * @return the power adjusted by factoring in stamina
     */
    protected int staminaBoost(int initialPower)
    {
        double usableStamina = (playerRemainingStamina * Player.halfProgress());
        return (int)(usableStamina / (TOTAL_HALF_TICKS - TickCount)) + initialPower;
    }


    /** {@inheritDoc} */
    @Override
    public void infoHearPlayMode(PlayMode playMode)
    {
        int playerNumber = this.getPlayer().getNumber();
        if (playerNumber == 1) {
            if (playMode == PlayMode.GOAL_OWN) {
                GoalsOwn = GoalsOwn + 1;
                setAggression(getAggression() - GOAL_AGGRESSION_CHANGE);
            }else if (playMode == PlayMode.GOAL_OTHER) {
                GoalsOther = GoalsOther + 1;
                setAggression(getAggression() + GOAL_AGGRESSION_CHANGE);
            }
        }

        if (playMode == PlayMode.KICK_OFF_OWN) {
            this.moveToKickoffPositions(playerNumber);
        } else if (playMode == PlayMode.BEFORE_KICK_OFF || playMode == PlayMode.KICK_OFF_OTHER) {
            this.moveToStartingPositions(playerNumber);
        }
    }


    /** {@inheritDoc} */
    @Override
    public ActionsPlayer getPlayer()
    {
        return player;
    }
    /** {@inheritDoc} */
    @Override
    public void setPlayer(ActionsPlayer p)
    {
        player = p;
    }
    /** {@inheritDoc} */
    @Override
    public String getType()
    {
        return playerType;
    }


    /** {@inheritDoc} */
    @Override
    public void infoSeeBall(double distance, double direction, double distChange, double dirChange,
                            double bodyFacingDirection, double headFacingDirection)
    {
        distanceToBall = distance;
        directionToBall = direction;
        playerPositionModel.addBall(this, distance, direction);
    }


    /** {@inheritDoc} */
    @Override
    public void setType(String newType) {}

    /** {@inheritDoc} */
    @Override
    public void infoHearError(Errors error) {}

    /** {@inheritDoc} */
    @Override
    public void infoHearOk(Ok ok) {}

    /** {@inheritDoc} */
    @Override
    public void infoHearWarning(Warning warning) {}

    /** {@inheritDoc} */
    @Override
    public void infoHearReferee(RefereeMessage refereeMessage) {}


    /** {@inheritDoc} */
    @Override
    public void infoHearPlayer(double direction, String string) {}

    static HashMap<Integer, HashMap<String, ArrayList<Double>>> stats = new HashMap<Integer, HashMap<String, ArrayList<Double>>>();
    /** {@inheritDoc} */
    @Override
    public void infoSenseBody(ViewQuality viewQuality, ViewAngle viewAngle, double stamina, double unknown,
                              double effort, double speedAmount, double speedDirection, double headAngle,
                              int kickCount, int dashCount, int turnCount, int sayCount, int turnNeckCount,
                              int catchCount, int moveCount, int changeViewCount)
    {
        HashMap<String, ArrayList<Double>> stat = stats.get(getPlayer().getNumber());
        if (stat == null) {
            stat = new HashMap<String, ArrayList<Double>>();
            stat.put("stamina", new ArrayList<Double>());
            stat.put("unknown", new ArrayList<Double>());
            stat.put("effort", new ArrayList<Double>());
            stat.put("speedAmount", new ArrayList<Double>());
            stat.put("speedDirection", new ArrayList<Double>());
            stat.put("headAngle", new ArrayList<Double>());
            stats.put(getPlayer().getNumber(), stat);
        }
        stat.get("stamina").add(stamina);
        stat.get("unknown").add(unknown);
        stat.get("effort").add(effort);
        stat.get("speedAmount").add(speedAmount);
        stat.get("speedDirection").add(speedDirection);
        stat.get("headAngle").add(headAngle);
        this.playerRemainingStamina = stamina;
    }


    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagGoalOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                                   double bodyFacingDirection, double headFacingDirection)
    {
        if (!alreadySeeingOwnGoal) {
            directionMultiplier *= -1.0;
        }
        if (flag.compareTo(Flag.CENTER) == 0) {
            distanceOwnGoal = distance;
            directionOwnGoal = direction;
            canSeeOwnGoal = true;
            ownGoalTurn = 180;
        }
        if (flag.compareTo(Flag.LEFT) == 0) {
            canSeeGoalLeft = true;
            ownGoalTurn = 90;
        }
        if (flag.compareTo(Flag.RIGHT) == 0) {
            canSeeGoalRight = true;
            ownGoalTurn = -90;
        }
    }



    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagGoalOther(Flag flag, double distance, double direction, double distChange, double dirChange,
                                     double bodyFacingDirection, double headFacingDirection)
    {
        //-7
        if(flag.compareTo(Flag.CENTER) == 0)
        {
            distanceOtherGoal = distance;
            directionOtherGoal = direction;
            canSeeOtherGoal = true;
            otherGoalTurn = 180;
        }
    }


    /** {@inheritDoc} */
    @Override
    public void infoSeePlayerOther(int number, boolean goalie, double distance, double direction, double distChange,
                                   double dirChange, double bodyFacingDirection, double headFacingDirection) 
    {
        playerPositionModel.addPlayer(this, false, number, distance, direction);
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeePlayerOwn(int number, boolean goalie, double distance, double direction, double distChange,
                                 double dirChange, double bodyFacingDirection, double headFacingDirection) 
    {
        playerPositionModel.addPlayer(this, true, number, distance, direction);
    }

    /** {@inheritDoc} */
    @Override
    public void infoPlayerParam(double allowMultDefaultType, double dashPowerRateDeltaMax,
                                double dashPowerRateDeltaMin, double effortMaxDeltaFactor, double effortMinDeltaFactor,
                                double extraStaminaDeltaMax, double extraStaminaDeltaMin,
                                double inertiaMomentDeltaFactor, double kickRandDeltaFactor,
                                double kickableMarginDeltaMax, double kickableMarginDeltaMin,
                                double newDashPowerRateDeltaMax, double newDashPowerRateDeltaMin,
                                double newStaminaIncMaxDeltaFactor, double playerDecayDeltaMax,
                                double playerDecayDeltaMin, double playerTypes, double ptMax, double randomSeed,
                                double staminaIncMaxDeltaFactor, double subsMax) {}

    /** {@inheritDoc} */
    @Override
    public void infoPlayerType(int id, double playerSpeedMax, double staminaIncMax, double playerDecay,
                               double inertiaMoment, double dashPowerRate, double playerSize, double kickableMargin,
                               double kickRand, double extraStamina, double effortMax, double effortMin) {}




    @Override
    public void infoCPTOther(int unum) {}

    /** {@inheritDoc} */
    @Override
    public void infoCPTOwn(int unum, int type) {}

    /** {@inheritDoc} */
    @Override
    public void infoServerParam(HashMap<ServerParams, Object> info) {}


    /** {@inheritDoc} */
    @Override
    public void infoSeeLine(Line line, double distance, double direction, double distChange, double dirChange,
                            double bodyFacingDirection, double headFacingDirection) {}


    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagOther(Flag flag, double distance, double direction, double distChange, double dirChange,
                                 double bodyFacingDirection, double headFacingDirection)
    {
        int flagY = flagToY(flag);
        int flagX = BOUNDARY_WIDTH /2;
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);

    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                               double bodyFacingDirection, double headFacingDirection)
    {
        //Flags from behind our own goal. X constant, Y changes
        int flagY = flagToY(flag);
        int flagX = -BOUNDARY_WIDTH/2;
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        canSeeFieldEnd = true;
    }


    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCenter(Flag flag, double distance, double direction, double distChange, double dirChange,
                                  double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = 0;
        int multiple = 0;
        switch (flag) {
            case CENTER:
                multiple = 0;
                break;
            case LEFT:
                multiple = -1;
                break;
            case RIGHT:
                multiple = 1;
                break;
        }
        int flagY = multiple * ((BOUNDARY_HEIGHT -2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2);
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
    }


    private static HashMap<String, String> stuff = new HashMap<String, String>();
    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                                     double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = -(BOUNDARY_WIDTH -2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2;
        int flagY = (BOUNDARY_HEIGHT -2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2 * (flag == Flag.LEFT ? -1 : 1);
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOther(Flag flag, double distance, double direction, double distChange,
                                       double dirChange, double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = (BOUNDARY_WIDTH -2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2;
        int flagY = (BOUNDARY_HEIGHT -2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2 * (flag == Flag.LEFT ? -1 : 1);
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOther(Flag flag, double distance, double direction, double distChange,
                                        double dirChange, double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = PENALTY_DISTANCE_FROM_CENTER;
        int flagY = 0;
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOwn(Flag flag, double distance, double direction, double distChange,
                                      double dirChange, double bodyFacingDirection, double headFacingDirection)
    {
        canSeeOwnPenalty = true;
        int flagX = -PENALTY_DISTANCE_FROM_CENTER;
        int flagY = 0;
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagRight(Flag flag, double distance, double direction, double distChange, double dirChange,
                                 double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = flagToX(flag);
        int flagY = BOUNDARY_HEIGHT /2;
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagLeft(Flag flag, double distance, double direction, double distChange, double dirChange,
                                double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = flagToX(flag);
        int flagY = -BOUNDARY_HEIGHT /2;
        playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
    }


    /**
     * @param flag the flag enum to get the value for
     * @return X co ord for the flag
     */
    private int flagToX(Flag flag)
    {
        int x = 0;
        switch (flag) {
            case OTHER_10:
                x = 10;
            break;
            case OTHER_20:
                x = 20;
                break;
            case OTHER_30:
                x = 30;
                break;
            case OTHER_40:
                x = 40;
                break;
            case OTHER_50:
                x = 50;
                break;
            case OWN_10:
                x = -10;
                break;
            case OWN_20:
                x = -20;
                break;
            case OWN_30:
                x = -30;
                break;
            case OWN_40:
                x = -40;
                break;
            case OWN_50:
                x = -50;
                break;
            case CENTER:
                x = 0;
                break;

        }
        return x;

    }


    /**
     * @param flag the flag enum to get the value for
     * @return Y co ord for the flag
     */
    private int flagToY(Flag flag)
    {
        int y = 0;
        switch (flag) {
            case CENTER:
                y = 0;
                break;
            case RIGHT_10:
                y = 10;
                break;
            case RIGHT_20:
                y = 20;
                break;
            case RIGHT_30:
                y = 30;
                break;
            case LEFT_10:
                y = -10;
                break;
            case LEFT_20:
                y = -20;
                break;
            case LEFT_30:
                y = -30;
                break;
        }
        return y;
    }


}
