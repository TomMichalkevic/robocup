
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

    /**
     * If we have the ball, attempt to pass it out first
     * then if we can't find a good pass dribble it forward if there is space
     * or just kick it clear
     */
    protected void playerHasBallAction()
    {
        int playerNumber = getPlayer().getNumber();
        if (isFowardOwnPlayer()) {
            //Kick to player
            getPlayer().kick(DRIBBLE_POWER + (int) distanceTo(closestTeamMember()), directionTo(closestTeamMember()));
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


    /**
     * If the ball is very close run towards it when there aren't many players near by
     * Otherwise move to defending track pattern
     */
    protected void ballIsVeryCloseAction()
    {

        int playersCloseToBall = numberOfOurPlayersWithRangeOfBall(BALL_CROWDING_RANGE);
        if (playersCloseToBall < 1) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(defenderDashSpeed());
        } else { //Track towards goal
            trackDefending();
        }
    }


    /**
     * If there aren't meany players near ball move towards it, else track
     */
    protected void ballIsCloseAction()
    {
        int playersCloseToBall = numberOfOurPlayersWithRangeOfBall(BALL_CROWDING_RANGE);
        if (playersCloseToBall < 2) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(defenderDashSpeed());
        } else { //Track towards goal
            trackDefending();
        }
    }


    /**
     * Track defending if the ball is far away
     */
    protected void ballIsFarAction()
    {
        trackDefending();
    }


    /**
     * If we are out of the action, move to our holding position
     */
    protected void moveToHoldingPosition()
    {
        // Don't let defenders move to far back!
        int position = Math.max(getAggression()/6 - 45 + BallPositionOffset, -45);
        switch (getPlayer().getNumber()) {
            case LEFT_BACK :
                moveToPosition(position, -20, dashValueSlow());
                break;
            case CENTER_LEFT_BACK :
                moveToPosition(position, -8, dashValueSlow());
                break;
            case CENTER_RIGHT_BACK :
                moveToPosition(position, 8, dashValueSlow());
                break;
            case RIGHT_BACK :
                moveToPosition(position, 20, dashValueSlow());
                break;
        }
    }


    /**
     * Increase dash speed when ball is in our goal area
     * @return the dash speed to be used
     */
    protected int defenderDashSpeed()
    {
        return ballInOurGoalArea() ? dashValueVeryFast() : dashValueFast();
    }


    /**
     * If the ball is in our goal area, get defenders between it and the goal
     * If we are in between the goal and the ball then spread out if we need to
     * Otherwise get in between goal and ball
     * Else move to position
     */
    protected void trackDefending()
    {
        if (ballInOurGoalArea()) {
            if (inBetweenOurGoalAndBall()) {
                if (numberOfOurPlayersWithRangeOfMe(PLAYER_CROWDING_RANGE) > 0) {
                    getPlayer().turn(oppositeDirectionTo(closestTeamMember()));
                    getPlayer().dash(dashValueSlow());
                } else {
                    lookAround();
                }
            } else {
                interceptBall();
            }
        } else {
            moveToHoldingPosition();
        }
    }


    /**
     * Get the defender in between our goal and the ball
     */
    protected void interceptBall()
    {
        Point point = goalBallInterceptionPoint();
        moveToPosition((int)point.x,(int)point.y, dashValueVeryFast());
    }



}
