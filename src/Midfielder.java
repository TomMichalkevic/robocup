
//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------


/**
 * <p>Defender class.</p>
 *
 * @author The Turing Autonoma
 *
 ** Midfielder behaviour
 *
 * If have the ball
 *  If the goal is very close shoot at the goal
 *  If there is a player in front with space kick it to him
 *  If the goal is close shoot
 *  Dribble the ball (faster if a winger)
 * If ball is very close
 *  Dash towards it
 * If the ball is close
 *  Dash fast towards it
 * If the ball is far but visible
 *  Slowly move towards it
 * Else If ball not visible move to holding position
 *
 * Use aggression to calculate dash speeds and positions
 */
public class Midfielder extends Player {

    public Midfielder() {
        super();
        playerType = "Midfielder";
    }

    /**
     *  If the goal is very close shoot at the goal
     *  If there is a player in front with space kick it to him
     *  If the goal is close shoot
     *  Dribble the ball (faster if a winger)
     */
    protected void playerHasBallAction()
    {
        startAttacking();
        int playerNumber = getPlayer().getNumber();
        if (distanceOtherGoal <= STRIKER_SHOOTING_RANGE) {
            //Shoot
            getPlayer().kick(BASE_SHOOT_POWER + getAggression()/2, directionToOtherGoal());
        } else if (isFowardOwnPlayer()) {
            //Kick to player
            getPlayer().kick(DRIBBLE_POWER + (int) distanceTo(closestTeamMember()), directionTo(closestTeamMember()));
        } else if (distanceOtherGoal <= MIDFIELDER_SHOOTING_RANGE) {
            //Shoot
            getPlayer().kick(BASE_SHOOT_POWER + getAggression(), directionToOtherGoal());
        } else {
            if (playerNumber == LEFT_WING || playerNumber == RIGHT_WING) {
                // Dribble towards goal
                getPlayer().kick(LONG_DRIBBLE_POWER, directionToOtherGoal());
            } else {
                // Dribble towards goal
                getPlayer().kick(DRIBBLE_POWER, directionToOtherGoal());
            }
        }
    }


    /**
     * If the ball is very close and there are no other player then get to the ball.
     * If there are 1 other players or we don't know how many take a slower pace towards the ball
     * Other wise follow the tracking pattern
     */
    protected void ballIsVeryCloseAction()
    {
       int playersCloseToBall = numberOfOurPlayersWithRangeOfBall(BALL_CROWDING_RANGE);

        if (playersCloseToBall == 0) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueVeryFast());
        } else if (playersCloseToBall == 1 || playersCloseToBall < 0) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueFast());
        } else { //Track towards goal
            trackBall();
        }
    }


    /**
     * If the ball is very close and there are only 2 players near to the ball then get closer
     * Other wise follow the tracking pattern
     */
    protected void ballIsCloseAction()
    {
        int playersCloseToBall = numberOfOurPlayersWithRangeOfBall(BALL_CROWDING_RANGE);

        if (playersCloseToBall < 3) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueVeryFast());
        }  else { //Track towards goal
            trackBall();
        }
    }


    /**
     * If there ball is far but there aren't many players near the ball then get close quickly
     * If there are a few players then slowly move towards ball
     * Other wise follow the tracking pattern
     */
    protected void ballIsFarAction()
    {
        int playersCloseToBall = numberOfOurPlayersWithRangeOfBall(BALL_CROWDING_RANGE);

        if (playersCloseToBall < 2) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueFast());
        } else if (playersCloseToBall == 2 || playersCloseToBall < 0) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueSlow());
        }  else { //Track towards goal
            trackBall();
        }
    }


    /**
     * If we are out of the action, move to our holding position
     */
    protected void moveToHoldingPosition()
    {
        int position = Math.max(getAggression()/4 - 30 + BallPositionOffset, -35);
        switch (getPlayer().getNumber()) {
            case LEFT_WING :
                moveToPosition(position, -20);
                break;
            case CENTER_LEFT_MIDFIELD :
                moveToPosition(position, -8);
                break;
            case CENTER_RIGHT_MIDFIELD :
                moveToPosition(position, 8);
                break;
            case RIGHT_WING :
                moveToPosition(position, 20);
                break;
        }
    }


    /**
     * If we are attacking then track ball otherwise move to holding position
     * If there many players near the ball then we want to track it rather than get close to it
     * First, check if there are players close to us and if there are, spread out.
     * If we have space, follow the ball if it's far away
     * Otherwise observer surroundings
     */
    protected void trackBall()
    {
        if (Attacking){
            if (numberOfOurPlayersWithRangeOfMe(PLAYER_CROWDING_RANGE) > 0) {
                getPlayer().turn(oppositeDirectionTo(closestTeamMember()));
                getPlayer().dash(dashValueSlow());
            } else {
                if (bestDistanceToBall() > Player.BALL_TRACKING_DISTANCE) {
                    getPlayer().turn(bestDirectionToBall());
                    getPlayer().dash(dashValueSlow());
                }else {
                    lookAround();
                }
            }
        }else {
            moveToHoldingPosition();
        }

    }

}
