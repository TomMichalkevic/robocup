
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

    protected void playerHasBallAction()
    {
        int playerNumber = getPlayer().getNumber();
        if (distanceOtherGoal <= STRIKER_SHOOTING_RANGE) {
            //Shoot
            getPlayer().kick(BASE_SHOOT_POWER + getAggression()/2, directionToOtherGoal());
        } else if (isFowardOwnPlayer()) {
            //Kick to player
            getPlayer().kick(DRIBBLE_POWER + (int) distanceClosestForwardOwnPlayer, directionClosestForwardOwnPlayer);
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
            if (numberOfOurPlayersWithRangeOfMe(PLAYER_CROWDING_RANGE) > 0) {

                getPlayer().turn(oppositeDirectionTo(closestTeamMember()));
                getPlayer().dash(dashValueSlow());
            } else {
                // Do nothing...?
                lookAround();
            }
        }
    }


    protected void ballIsCloseAction()
    {
        int playersCloseToBall = numberOfOurPlayersWithRangeOfBall(BALL_CROWDING_RANGE);

        if (playersCloseToBall < 3) {
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueVeryFast());
        }  else { //Track towards goal
            if (numberOfOurPlayersWithRangeOfMe(PLAYER_CROWDING_RANGE) > 0) {

                getPlayer().turn(oppositeDirectionTo(closestTeamMember()));
                getPlayer().dash(dashValueSlow());
            } else {
                // Do nothing...?
                lookAround();
            }
        }
    }

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
            if (numberOfOurPlayersWithRangeOfMe(PLAYER_CROWDING_RANGE) > 0) {

                getPlayer().turn(oppositeDirectionTo(closestTeamMember()));
                getPlayer().dash(dashValueSlow());
            } else {
                // Do nothing...?
                lookAround();
            }
        }
    }


    protected void moveToHoldingPosition()
    {
        // Don't let defenders move to far back!
        int position = Math.max(getAggression()/4 - 30 + ballPositionOffset, -35);
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

}
