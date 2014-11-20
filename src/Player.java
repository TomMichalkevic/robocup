import com.github.robocup_atan.atan.model.ActionsPlayer;
import com.github.robocup_atan.atan.model.ControllerPlayer;
import com.github.robocup_atan.atan.model.enums.*;

import java.util.HashMap;
import java.util.Random;


/**
 * Created by cmitchelmore on 13/11/14.
 */
public abstract class Player implements ControllerPlayer {

    public static final int PITCH_BOUNDARY_X_WIDTH = 114,
            PITCH_BOUNDARY_Y_WIDTH = 70,
            PENALTY_DISTANCE_FROM_CENTER = 43,
            DISTANCE_PITCH_EDGE_TO_BOUNDARY = 5;
    /**
     * Define a constant for each player position for readability.
     */
    public static final int GOALIE = 1,
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

    protected static final double BALL_WITHIN_REACH = 0.7,
            BALL_VERY_CLOSE = 2,
            BALL_CLOSE = 15,
            STRIKER_SHOOTING_RANGE = 10.0,
            MIDFIELDER_SHOOTING_RANGE = 20.0,
            TOTAL_GAME_TICKS = 3000.0;

    protected static final int DRIBBLE_POWER = 6,
            LONG_DRIBBLE_POWER = 6,
            CLEARANCE_POWER = 50,
            BASE_SHOOT_POWER = 50,
            GOAL_AGGRESSION_CHANGE = 5;



    /**
     * Aggression defines the teams overall player style. If the team is losing aggression increases and if they are
     * winning it decreases. Aggression can be read as attack risk.
     *
     * Because we are using two sets of these players we need an aggression factor for each team.
     */
    private static int aggression = 50, goalsOwn = 0, goalsOther = 0, tickCount = 0;



    protected double distanceToBall = 1000,
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
            haveSeenSomeMarker = true,
            inHoldingPosition = false;

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
        return Player.aggression;
    }

    public void setAggression(int aggression)
    {
        Player.aggression = aggression;
    }

    /** {@inheritDoc} */
    @Override
    public void preInfo() {
        distanceToBall = 1000;
        distanceOwnGoal = 1000;
        directionOwnGoal = 0;
        distanceOtherGoal = 1000;
        directionOtherGoal = 0;
        distanceClosestForwardOwnPlayer = -1;
        directionClosestForwardOwnPlayer = -1;
        distanceClosestForwardOtherPlayer = -1;
        directionClosestForwardOtherPlayer = -1;
        canSeeOwnGoal = false;
        canSeeGoalLeft = false;
        canSeeGoalRight = false;
        canSeeOwnPenalty = false;
        canSeeFieldEnd = false;
        ownGoalTurn = 0.0;
        if (getPlayer().getNumber() == 1) {
            playerPositionModel.clearModel();
            tickCount++;
        }

    }

    protected abstract void playerHasBallAction();
    protected abstract void ballIsVeryCloseAction();
    protected abstract void ballIsCloseAction();
    protected abstract void ballIsFarAction();
    protected void ballNotVisibleAction()
    {
        if (inHoldingPosition) {
            lookAround();
        }else {
            moveToHoldingPosition();
        }
    }
    protected abstract void moveToHoldingPosition();
    protected void lookAround()
    {

    }

    protected boolean areNoCloseForwardPlayers()
    {
        return (distanceClosestForwardOwnPlayer > 8 && distanceClosestForwardOtherPlayer > 8) ||
                (distanceClosestForwardOwnPlayer < 0 && distanceClosestForwardOtherPlayer < 0);
    }
    /**
     *
     * @return is there a visible player on our team in front of this player
     */
    protected boolean isFowardOwnPlayer()
    {
        return distanceClosestForwardOwnPlayer > 0;
    }

    /**
     *
     * @return true if the ball is in between the player and other goal. False if not or not known.
     */
    protected boolean isBallOtherGoalSideOfPlayer()
    {
        return true;
    }

    /**
     *
     * @return true if the ball is in between the player and own goal. False if not or not known.
     */
    protected boolean isBallOwnGoalSideOfPlayer()
    {
        return true;
    }

    protected static double halfProgress()
    {
        return tickCount/TOTAL_GAME_TICKS;
    }


    @Override
    public void postInfo()
    {
        playerPositionModel.estimatePositions();
        if (distanceToBall <= Player.BALL_WITHIN_REACH) {
            playerHasBallAction();
        }else if (distanceToBall <= Player.BALL_VERY_CLOSE) {
            ballIsVeryCloseAction();
        }else if (distanceToBall <= Player.BALL_CLOSE) {
            ballIsCloseAction();
        }else if (distanceToBall < 1000){
            ballIsFarAction();
        }else {
            ballNotVisibleAction();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoHearPlayMode(PlayMode playMode)
    {
        int playerNumber = this.getPlayer().getNumber();
        if (playerNumber == 1) {
           if (playMode == PlayMode.GOAL_OWN) {
               goalsOwn++;
               setAggression(getAggression() - GOAL_AGGRESSION_CHANGE);
           }else if (playMode == PlayMode.GOAL_OTHER) {
               goalsOther++;
               setAggression(getAggression() + GOAL_AGGRESSION_CHANGE);
           }
        }

        if (playMode == PlayMode.KICK_OFF_OWN) {
            this.moveToKickoffPositions(playerNumber);
        } else if (playMode == PlayMode.BEFORE_KICK_OFF || playMode == PlayMode.KICK_OFF_OTHER) {
            this.moveToStartingPositions(playerNumber);
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


    /** {@inheritDoc} */

    protected int staminaBoost(int initialPower)
    {
        double usableStamina = (playerRemainingStamina * Player.halfProgress());
        return (int)(usableStamina / (TOTAL_GAME_TICKS-tickCount)) + initialPower;
    }


    protected int dashValueVeryFast()
    {
        int initialPower = getAggression() + 50;
        return staminaBoost(initialPower);
    }

    protected int dashValueFast()
    {
        int initialPower = getAggression() + 10;
        return staminaBoost(initialPower);
    }

    protected int dashValueSlow()
    {
        int initialPower = getAggression() - 30;
        return staminaBoost(initialPower);
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

    /** {@inheritDoc} */
    @Override
    public void infoSenseBody(ViewQuality viewQuality, ViewAngle viewAngle, double stamina, double unknown,
                              double effort, double speedAmount, double speedDirection, double headAngle,
                              int kickCount, int dashCount, int turnCount, int sayCount, int turnNeckCount,
                              int catchCount, int moveCount, int changeViewCount)
    {
        this.playerRemainingStamina = stamina;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagGoalOther(Flag flag, double distance, double direction, double distChange, double dirChange,
                                     double bodyFacingDirection, double headFacingDirection)
    {

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
        //TODO: Test that this player is actually in front
        if (distanceClosestForwardOtherPlayer < 0 || distanceClosestForwardOtherPlayer > distance) {
            distanceClosestForwardOtherPlayer = distance;
            directionClosestForwardOtherPlayer = direction;
        }
        playerPositionModel.addPlayer(this, false, number, distance, direction);
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeePlayerOwn(int number, boolean goalie, double distance, double direction, double distChange,
                                 double dirChange, double bodyFacingDirection, double headFacingDirection) 
    {
        //TODO: Test that this player is actually in front
        if (distanceClosestForwardOwnPlayer < 0 || distanceClosestForwardOwnPlayer > distance) {
            distanceClosestForwardOwnPlayer = distance;
            directionClosestForwardOwnPlayer = direction;
        }
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
        int flagX = PITCH_BOUNDARY_X_WIDTH/2;
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                               double bodyFacingDirection, double headFacingDirection)
    {
        int flagY = flagToY(flag);
        int flagX = -PITCH_BOUNDARY_X_WIDTH/2;
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        }
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
        int flagY = multiple * ((PITCH_BOUNDARY_Y_WIDTH-2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2);
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance); //TODO distance could be negative
        }
    }
    private static HashMap<String, String> stuff = new HashMap<String, String>();
    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                                     double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = -(PITCH_BOUNDARY_X_WIDTH-2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2;
        int flagY = (PITCH_BOUNDARY_Y_WIDTH-2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2 * (flag == Flag.LEFT ? -1 : 1);
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOther(Flag flag, double distance, double direction, double distChange,
                                       double dirChange, double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = (PITCH_BOUNDARY_X_WIDTH-2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2;
        int flagY = (PITCH_BOUNDARY_Y_WIDTH-2*DISTANCE_PITCH_EDGE_TO_BOUNDARY)/2 * (flag == Flag.LEFT ? -1 : 1);
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOther(Flag flag, double distance, double direction, double distChange,
                                        double dirChange, double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = PENALTY_DISTANCE_FROM_CENTER;
        int flagY = 0;
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOwn(Flag flag, double distance, double direction, double distChange,
                                      double dirChange, double bodyFacingDirection, double headFacingDirection)
    {
        canSeeOwnPenalty = true;
        int flagX = -PENALTY_DISTANCE_FROM_CENTER;
        int flagY = 0;
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagRight(Flag flag, double distance, double direction, double distChange, double dirChange,
                                 double bodyFacingDirection, double headFacingDirection)
    {
        int flagX = flagToX(flag);
        int flagY = PITCH_BOUNDARY_Y_WIDTH/2;
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagLeft(Flag flag, double distance, double direction, double distChange, double dirChange,
                                double bodyFacingDirection, double headFacingDirection)
    {
        // CENTER,
        // RIGHT, RIGHT_10, RIGHT_20, RIGHT_30;
        // LEFT, LEFT_10, LEFT_20, LEFT_30,
        // OTHER_10, OTHER_20, OTHER_30, OTHER_40, OTHER_50,
        // OWN_10, OWN_20, OWN_30, OWN_40, OWN_50,


        stuff.put(flag + " " + getPlayer().getNumber(), "dir: "+direction+" dis:"+distance);

        int flagX = flagToX(flag);
        int flagY = -PITCH_BOUNDARY_Y_WIDTH/2;
        if (haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, flagX, flagY, direction, distance);
        }
    }

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
                y = 10;
                break;
            case LEFT_20:
                y = 20;
                break;
            case LEFT_30:
                y = 30;
                break;
        }
        return y;
    }


}
