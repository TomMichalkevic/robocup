
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


    /**
     * Turn on attacking
     * Decide if we should shoot, pass or dribble
     */
    protected void playerHasBallAction()
    {
        startAttacking();
        if (distanceOtherGoal <= STRIKER_SHOOTING_RANGE) {
            //Shoot
            this.getPlayer().kick(BASE_SHOOT_POWER + getAggression(), directionToOtherGoal());
        }else if (isFowardOwnPlayer()) {
            //Kick to player
            this.getPlayer().kick(DRIBBLE_POWER + (int) distanceTo(closestTeamMember()), directionTo(closestTeamMember()));
        } else {
            // Dribble towards goal
            this.getPlayer().kick(DRIBBLE_POWER, directionToOtherGoal());
        }
    }


    /**
     * If there are no players near the ball and it's goal side then go for it, otherwise leave it to midfield
     * Track the ball
     */
    protected void ballIsVeryCloseAction()
    {
        int playersCloseToBall = numberOfOurPlayersWithRangeOfBall(BALL_CROWDING_RANGE);
        if (playersCloseToBall == 0 && isBallOtherGoalSideOfPlayer()) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueVeryFast());
        } else { //Track towards goal
            trackAttacking();
        }
    }


    /**
     * If the ball is close then either get there very fast if there are no players near or
     * fast if there is one
     * otherwise track
     */
    protected void ballIsCloseAction()
    {
        int playersCloseToBall = numberOfOurPlayersWithRangeOfBall(BALL_CROWDING_RANGE);
        if (playersCloseToBall == 0 && isBallOtherGoalSideOfPlayer()) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueVeryFast());
        } else if (playersCloseToBall < 2 && isBallOtherGoalSideOfPlayer()) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueFast());
        } else { //Track towards goal
            trackAttacking();
        }
    }


    /**
     * If the ball is far track pattern
     */
    protected void ballIsFarAction()
    {
        trackAttacking();
    }


    /**
     * If we are out of the action, move to our holding position
     */
    protected void moveToHoldingPosition()
    {
        // Move players further up the field, in holding position, when more aggressive
        int position = getAggression()/2 - 25 + BallPositionOffset;
        switch (getPlayer().getNumber()) {
            case CENTER_RIGHT_FORWARD:
                moveToPosition(position, 10);
                break;
            case CENTER_LEFT_FORWARD:
                moveToPosition(position, -10);
                break;
        }
    }


    /**
     * * If we are attacking then track otherwise move to holding position
     * If there many players near the ball then we want to get ahead of it to score
     * If we are tracking distance in front then spread out
     * If we are spread ahead of the ball then look around
     */
    protected void trackAttacking()
    {
        if (Attacking){
            if ((isBallOtherGoalSideOfPlayer() && bestDistanceToBall() < Player.BALL_TRACKING_DISTANCE) || isBallOwnGoalSideOfPlayer()) {
                getPlayer().turn(directionToOtherGoal());
                getPlayer().dash(dashValueFast());
            } else if (numberOfOurPlayersWithRangeOfMe(PLAYER_CROWDING_RANGE) > 0) {
                getPlayer().turn(directionToOtherGoal());
                getPlayer().dash(dashValueSlow());
            } else {
                lookAround();
            }
        }else {
            moveToHoldingPosition();
        }
    }

}
