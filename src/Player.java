import com.github.robocup_atan.atan.model.ActionsPlayer;
import com.github.robocup_atan.atan.model.ControllerPlayer;
import com.github.robocup_atan.atan.model.enums.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


/**
 * Created by cmitchelmore on 13/11/14.
 */
public abstract class Player implements ControllerPlayer {

    /**
     * Define a constant for each player position for readability.
     */
    public static final int GOALIE = 1, LEFT_BACK = 2, CENTER_LEFT_BACK = 3, CENTER_RIGHT_BACK = 4, RIGHT_BACK = 5,
            LEFT_WING = 6, CENTER_LEFT_MIDFIELD = 7, CENTER_RIGHT_MIDFIELD = 8, RIGHT_WING = 9,
            CENTER_LEFT_FORWARD = 10, CENTER_RIGHT_FORWARD = 11;

    /**
     * Aggression defines the teams overall player style. If the team is losing aggression increases and if they are
     * winning it decreases. Aggression can be read as attack risk.
     *
     * Because we are using two sets of these players we need an aggression factor for each team.
     */
    private static int aggressionEast = 50,  aggressionWest = 50;



    protected double distanceToBall = 1000, directionToBall = 0, directionOwnGoal = 0, distanceGoal = -1.0;
    protected boolean canSeeGoal = false, canSeeGoalLeft = false, canSeeGoalRight = false, canSeeFieldEnd = false,
            alreadySeeingGoal = false, canSeePenalty = false;

    protected ActionsPlayer player;
    protected Random  random        = null;
    protected static int count         = 0;
    protected double     dirMultiplier = 1.0;
    protected double     goalTurn;
    protected boolean 	  needsToRetreat = false;
    protected String playerType = "";

    public int getAggression() {

        return this.getPlayer().isTeamEast() ? Player.aggressionEast : Player.aggressionWest;
    }

    public void setAggression(int aggression) {
        if (this.getPlayer().isTeamEast()) {
            Player.aggressionEast = aggression;
        }else {
            Player.aggressionWest = aggression;
        }
    }

    public Player() {
        random = new Random(System.currentTimeMillis() + count);
        count++;
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


    protected int randomDashValueVeryFast()
    {
        return 100 + random.nextInt(30);
    }

    protected int randomDashValueFast()
    {
        return 30 + random.nextInt(100);
    }

    /**
     * Randomly choose a slow dash value.
     * @return
     */
    protected int randomDashValueSlow()
    {
        return -10 + random.nextInt(50);
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
    public void infoSeeLine(Line line, double distance, double direction, double distChange, double dirChange,
                            double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeeBall(double distance, double direction, double distChange, double dirChange,
                            double bodyFacingDirection, double headFacingDirection)
    {
        distanceToBall = distance;
        directionToBall = direction;
    }

    /** {@inheritDoc} */
    @Override
    public String getType()
    {
        return playerType;
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
    public void infoSeeFlagRight(Flag flag, double distance, double direction, double distChange, double dirChange,
                                 double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagLeft(Flag flag, double distance, double direction, double distChange, double dirChange,
                                double bodyFacingDirection, double headFacingDirection) {}


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
                                     double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeePlayerOther(int number, boolean goalie, double distance, double direction, double distChange,
                                   double dirChange, double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeePlayerOwn(int number, boolean goalie, double distance, double direction, double distChange,
                                 double dirChange, double bodyFacingDirection, double headFacingDirection) {}

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
    public void infoSeeFlagOther(Flag flag, double distance, double direction, double distChange, double dirChange,
                                 double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCenter(Flag flag, double distance, double direction, double distChange, double dirChange,
                                  double bodyFacingDirection, double headFacingDirection) {}

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                                     double bodyFacingDirection, double headFacingDirection) {}

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
    public void preInfo() {


    }
}