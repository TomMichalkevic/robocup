
//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------


/**
 * <p>Defender class.</p>
 *
 * @author The Turing Autonoma
 *
 * Striker behaviour
 *
 * If have the ball
 *  If the goal is really close shoot at the goal
 *  If there is a player in front with space kick it to him
 *  Kick the ball forwards
 * If the ball is close
 *  If in opponents half dash fast towards it
 *  Else hold position
 * If the ball is far but visible
 *  If ball is between goal and player fast dash
 *  Else hold position
 * Else If ball not visible move to holding position
 *
 * Use aggression to calculate dash speeds and positions
 */

public class Striker extends Player {


    public Striker()
    {
        super();
        playerType = "Striker";
    }


    protected void playerHasBallAction()
    {
        if (distanceOtherGoal <= STRIKER_SHOOTING_RANGE) {
            //Shoot
            this.getPlayer().kick(BASE_SHOOT_POWER + getAggression(), directionToOtherGoal());
        }else if (isFowardOwnPlayer()) {
            //Kick to player
            this.getPlayer().kick(DRIBBLE_POWER + (int) distanceClosestForwardOwnPlayer, directionClosestForwardOwnPlayer);
        } else {
            // Dribble towards goal
            this.getPlayer().kick(DRIBBLE_POWER, directionToOtherGoal());
        }
    }

    protected void ballIsVeryCloseAction()
    {
        getPlayer().turn(bestDirectionToBall());
        if (isBallOtherGoalSideOfPlayer()) { //Don't burn striker stamina tracking back
            getPlayer().dash(dashValueFast());
        } else {
            getPlayer().dash(dashValueSlow());
        }
    }

    protected void ballIsCloseAction()
    {
        if (isBallOtherGoalSideOfPlayer()) { //Don't burn striker stamina tracking back
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueVeryFast());
        } else {
            moveToHoldingPosition();
        }
    }

    protected void ballIsFarAction()
    {
        if (isBallOtherGoalSideOfPlayer()) { //Don't burn striker stamina tracking back
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueFast());
        } else {
            moveToHoldingPosition();
        }
    }


    protected void moveToHoldingPosition()
    {
        // Move players further up the field, in holding position, when more aggressive
        int position = getAggression()/2 - 25 + ballPositionOffset;
        switch (getPlayer().getNumber()) {
            case CENTER_RIGHT_FORWARD:
                moveToPosition(position, 10);
                break;
            case CENTER_LEFT_FORWARD:
                moveToPosition(position, -10);
                break;
        }
    }

}
