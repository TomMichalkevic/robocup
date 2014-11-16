
//~--- non-JDK imports --------------------------------------------------------

import com.github.robocup_atan.atan.model.enums.Flag;

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
        if(canSeeGoal || canSeePenalty) {
            this.getPlayer().catchBall(directionToBall);
        }
        if(canSeeGoal) {
            this.getPlayer().kick(60, 135);
        } else {
            this.getPlayer().kick(60, 0);
        }
    }

    protected void ballIsVeryCloseAction()
    {
        if(canSeeGoal || canSeePenalty) {
            needsToRetreat = true;
            getPlayer().turn(directionToBall);
            getPlayer().dash(dashValueFast());
        }
    }

    protected void ballIsCloseAction()
    {
        if(canSeeGoal || canSeePenalty) {
            needsToRetreat = true;
            getPlayer().turn(directionToBall);
            getPlayer().dash(dashValueVeryFast());
         }
    }

    protected void ballIsFarAction()
    {
        if (!canSeeGoal && !needsToRetreat) {
            if (!canSeePenalty) {
                getPlayer().turn(90);
                getPlayer().dash(dashValueFast());
            } else if ((canSeeGoalLeft || canSeeGoalRight) && !canSeeFieldEnd) {
                getPlayer().turn(-1.0 * goalTurn);
                getPlayer().dash(dashValueSlow());
            } else {
                getPlayer().turn(25 * directionMultiplier);
            }
        } else {
            if (!canSeeGoal) {
                getPlayer().turn(90);
                getPlayer().dash(dashValueSlow());
            } else if (distanceOwnGoal > 3.5) {
                if (!alreadySeeingGoal) {
                    getPlayer().turn(directionOwnGoal);
                    alreadySeeingGoal = true;
                }
                getPlayer().dash(dashValueVeryFast());
            } else {
                needsToRetreat = false;
                if (alreadySeeingGoal) {
                    getPlayer().turn(goalTurn);
                    alreadySeeingGoal = false;
                } else {
                    alreadySeeingGoal = true;
                }
            }
        }
    }

    protected void ballNotVisibleAction()
    {
        ballIsFarAction();
    }

    protected void moveToHoldingPosition()
    {
        //Goal stays in position regardless of aggression
        getPlayer().move(-50, 0);
    }


}
