import com.github.robocup_atan.atan.model.ControllerPlayer;
import com.github.robocup_atan.atan.model.enums.PlayMode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by cmitchelmore on 13/11/14.
 */
public abstract class Player implements ControllerPlayer {

    /**
     * Define a constant for each player position for readability.
     */
    public static final int GOALIE = 1;
    public static final int LEFT_BACK = 2;
    public static final int CENTER_LEFT_BACK = 3;
    public static final int CENTER_RIGHT_BACK = 4;
    public static final int RIGHT_BACK = 5;
    public static final int LEFT_WING = 6;
    public static final int CENTER_LEFT_MIDFIELD = 7;
    public static final int CENTER_RIGHT_MIDFIELD = 8;
    public static final int RIGHT_WING = 9;
    public static final int CENTER_LEFT_FORWARD = 10;
    public static final int CENTER_RIGHT_FORWARD = 11;

    /**
     * Aggression defines the teams overall player style. If the team is losing aggression increases and if they are
     * winning it decreases. Aggression can be read as attack risk.
     *
     * Because we are using two sets of these players we need an aggression factor for each team.
     */
    private static int aggressionEast = 50;
    private static int aggressionWest = 50;


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


    /** {@inheritDoc} */
    @Override
    public void infoHearPlayMode(PlayMode playMode)
    {
        if (playMode == PlayMode.BEFORE_KICK_OFF)
            this.moveToStartingPositions(this.getPlayer().getNumber());
    }

    /**
     *
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
}
