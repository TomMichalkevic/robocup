
//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------


/**
 * <p>Silly class.</p>
 *
 * @author Atan
 */
public class Goalie extends Player {

    public Goalie() {
        super();
        playerType = "Goalie";
    }

    protected void playerHasBallAction()
    {
        if(canSeeOwnGoal || canSeeOwnPenalty) {
            this.getPlayer().catchBall(bestDirectionToBall());
        }
        if(canSeeOwnGoal) {
            this.getPlayer().kick(60, 135);
        } else {
            this.getPlayer().kick(60, 0);
        }
    }

    protected void ballIsVeryCloseAction()
    {
        if(canSeeOwnGoal || canSeeOwnPenalty) {
            needsToRetreat = true;
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueFast());
        }
    }

    protected void ballIsCloseAction()
    {
        if(canSeeOwnGoal || canSeeOwnPenalty) {
            needsToRetreat = true;
            getPlayer().turn(bestDirectionToBall());
            getPlayer().dash(dashValueVeryFast());
         }
    }

    protected void ballIsFarAction()
    {
        if (!canSeeOwnGoal && !needsToRetreat) {
            if (!canSeeOwnPenalty) {
                getPlayer().turn(90);
                getPlayer().dash(dashValueFast());
            } else if ((canSeeGoalLeft || canSeeGoalRight) && !canSeeFieldEnd) {
                getPlayer().turn(-1.0 * ownGoalTurn);
                getPlayer().dash(dashValueSlow());
            } else {
                getPlayer().turn(25 * directionMultiplier);
            }
        } else {
            if (!canSeeOwnGoal) {
                getPlayer().turn(90);
                getPlayer().dash(dashValueSlow());
            } else if (distanceOwnGoal > 3.5) {
                if (!alreadySeeingOwnGoal) {
                    getPlayer().turn(directionToOwnGoal());
                    alreadySeeingOwnGoal = true;
                }
                getPlayer().dash(dashValueVeryFast());
            } else {
                needsToRetreat = false;
                if (alreadySeeingOwnGoal) {
                    getPlayer().turn(ownGoalTurn);
                    alreadySeeingOwnGoal = false;
                } else {
                    alreadySeeingOwnGoal = true;
                }
            }
        }
    }


    /**
     * If the ball isn't visible just do what we would do if it was far away
     */
    protected void ballNotVisibleAction()
    {
        ballIsFarAction();
    }

    /**
     * Move to the center of the goal
     */
    protected void moveToHoldingPosition()
    {
        //Goal stays in position regardless of aggression
        moveToPosition(-50, 0, dashValueSlow());
    }


}
