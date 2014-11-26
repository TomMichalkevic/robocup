
//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------


/**
 * <p>Defender class.</p>
 *
 * @author The Turing Autonoma
 *
 ** Defender behaviour
 *
 * If have the ball
 *  If there is a player in front with space kick it to him
 *  If there are no players me dribble the ball up to half way
 *  Clear the ball
 * If ball is very close
 *  Dash towards it
 * If the ball is close and between me and our goal
 *  Dash fast towards it
 * If the ball is far but visible and between me and our goal
 *  Slowly move towards it
 * Else If ball not visible move to holding position
 *
 * Use aggression to calculate dash speeds and positions
 */
public class Defender extends Player {

    public Defender() {
       super();
       playerType = "Defender";
    }

    protected void playerHasBallAction()
    {
        int playerNumber = getPlayer().getNumber();
        if (isFowardOwnPlayer()) {
            //Kick to player
            getPlayer().kick(DRIBBLE_POWER + (int) distanceClosestForwardOwnPlayer, directionClosestForwardOwnPlayer);
        } else if (areNoCloseForwardPlayers()) {
            if (playerNumber == LEFT_BACK || playerNumber == RIGHT_BACK) {
                // Let wingers go a bit faster
                getPlayer().kick(LONG_DRIBBLE_POWER, directionToOtherGoal());
            } else {
                getPlayer().kick(DRIBBLE_POWER, directionToOtherGoal());
            }
        } else {
            getPlayer().kick(CLEARANCE_POWER, directionToOtherGoal());
        }
    }

    protected void ballIsVeryCloseAction()
    {
        getPlayer().turn(directionToBall);
        if (isBallOwnGoalSideOfPlayer()) { //Don't burn defender stamina attacking back
            getPlayer().dash(dashValueFast());
        } else {
            getPlayer().dash(dashValueSlow());
        }
    }

    protected void ballIsCloseAction()
    {
        if (isBallOwnGoalSideOfPlayer()) { //Don't burn defender stamina attacking back
            getPlayer().turn(directionToBall);
            getPlayer().dash(dashValueVeryFast());
        } else {
            moveToHoldingPosition();
        }
    }

    protected void ballIsFarAction()
    {
        if (isBallOwnGoalSideOfPlayer()) { //Don't burn defender stamina attacking back
            getPlayer().turn(directionToBall);
            getPlayer().dash(dashValueFast());
        } else {
            moveToHoldingPosition();
        }
    }


    protected void moveToHoldingPosition()
    {
        // Don't let defenders move to far back!
        int position = Math.max(getAggression()/6 - 45 + ballPositionOffset, -45);
        switch (getPlayer().getNumber()) {
            case LEFT_BACK :
                moveToPosition(position, -20);
                break;
            case CENTER_LEFT_BACK :
                moveToPosition(position, -8);
                break;
            case CENTER_RIGHT_BACK :
                moveToPosition(position, 8);
                break;
            case RIGHT_BACK :
                moveToPosition(position, 20);
                break;
        }
    }

}
