import com.github.robocup_atan.atan.model.ActionsPlayer;
import com.github.robocup_atan.atan.model.ControllerPlayer;
import com.github.robocup_atan.atan.model.enums.*;

import java.util.HashMap;
import java.util.Random;


/**
 * Created by cmitchelmore on 13/11/14.
 */
public abstract class Player implements ControllerPlayer {

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
            MIDFIELDER_SHOOTING_RANGE = 20.0;

    protected static final int DRIBBLE_POWER = 6,
            LONG_DRIBBLE_POWER = 6,
            CLEARANCE_POWER = 50,
            BASE_SHOOT_POWER = 50;



    /**
     * Aggression defines the teams overall player style. If the team is losing aggression increases and if they are
     * winning it decreases. Aggression can be read as attack risk.
     *
     * Because we are using two sets of these players we need an aggression factor for each team.
     */
    private static int aggressionEast = 50,
            aggressionWest = 50;



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
            otherGoalTurn;

    protected boolean canSeeOwnGoal = false,
            canSeeGoalLeft = false,
            canSeeGoalRight = false,
            canSeeFieldEnd = false,
            alreadySeeingOwnGoal = false,
            alreadySeeingOtherGoal = false,
            canSeeOtherGoal = false,
            canSeeOwnPenalty = false,
            needsToRetreat = false,
            haveSeenSomeMarker = false;

    protected ActionsPlayer player;
    protected Random random        = null;
    protected static int count = 0, ballPositionOffset = 0;
    protected String playerType = "";
    private static PlayerPositionModel playerPositionModel = null;

    public int getAggression()
    {
        return this.getPlayer().isTeamEast() ? Player.aggressionEast : Player.aggressionWest;
    }

    public void setAggression(int aggression)
    {
        if (this.getPlayer().isTeamEast()) {
            Player.aggressionEast = aggression;
        }else {
            Player.aggressionWest = aggression;
        }
    }


    public Player()
    {
        random = new Random(System.currentTimeMillis() + count);
        if (playerPositionModel == null) {
            playerPositionModel = new PlayerPositionModel();
        }
        count++;
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

    }

    protected abstract void playerHasBallAction();
    protected abstract void ballIsVeryCloseAction();
    protected abstract void ballIsCloseAction();
    protected abstract void ballIsFarAction();
    protected abstract void ballNotVisibleAction();
    protected abstract void moveToHoldingPosition();

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


    @Override
    public void postInfo()
    {
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
        if (playMode == PlayMode.KICK_OFF_OWN) {
            this.moveToKickoffPositions(playerNumber);
        } else if (playMode == PlayMode.BEFORE_KICK_OFF) {
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
                this.getPlayer().move(-20, -20);
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


    protected int dashValueVeryFast()
    {
        return getAggression() + 40 + random.nextInt(30);
    }

    protected int dashValueFast()
    {
        return getAggression() - 20 + random.nextInt(30);
    }

    protected int dashValueSlow()
    {
        return getAggression() - 60 + random.nextInt(30);
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
    public void infoSeeFlagOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                               double bodyFacingDirection, double headFacingDirection)
    {
        canSeeFieldEnd = true;
    }


    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOwn(Flag flag, double distance, double direction, double distChange,
                                      double dirChange, double bodyFacingDirection, double headFacingDirection)
    {
        canSeeOwnPenalty = true;
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
                              int catchCount, int moveCount, int changeViewCount) {}

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
                                 double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCenter(Flag flag, double distance, double direction, double distChange, double dirChange,
                                  double bodyFacingDirection, double headFacingDirection)
    {
        if (!haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
            playerPositionModel.addPosition(this, 0,0, direction, distance);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                                     double bodyFacingDirection, double headFacingDirection)
    {
        int i = 0;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOther(Flag flag, double distance, double direction, double distChange,
                                       double dirChange, double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOther(Flag flag, double distance, double direction, double distChange,
                                        double dirChange, double bodyFacingDirection, double headFacingDirection) {}


    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagRight(Flag flag, double distance, double direction, double distChange, double dirChange,
                                 double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagLeft(Flag flag, double distance, double direction, double distChange, double dirChange,
                                double bodyFacingDirection, double headFacingDirection)
    {
        // CENTER,
        // LEFT, LEFT_10, LEFT_20, LEFT_30,
        // OTHER_10, OTHER_20, OTHER_30, OTHER_40, OTHER_50,
        // OWN_10, OWN_20, OWN_30, OWN_40, OWN_50,
        // RIGHT, RIGHT_10, RIGHT_20, RIGHT_30;
        if (!haveSeenSomeMarker) {
            haveSeenSomeMarker = true;
        }
    }



}
