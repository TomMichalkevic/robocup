
//~--- non-JDK imports --------------------------------------------------------

import com.github.robocup_atan.atan.model.enums.Flag;

//~--- JDK imports ------------------------------------------------------------


/**
 * <p>Defender class.</p>
 *
 * @author The Turing Autonoma
 */
public class Defender extends Player {

    public Defender() {
       super();
       playerType = "Defender";
    }
    /** {@inheritDoc} */
    @Override
    public void preInfo() {
        super.preInfo();
        distanceToBall = 1000;
        distanceGoal = 1000;
        canSeeGoal = false;
        canSeeGoalLeft = false;
        canSeeGoalRight = false;
        canSeePenalty = false;
        canSeeFieldEnd = false;
        goalTurn = 0.0;
    }

    /** {@inheritDoc} */
    @Override
    public void postInfo()
    {
        if (distanceToBall < 15)
        {
            if (distanceToBall < 0.7)
            {
                if(canSeeGoal || canSeePenalty)
                    this.getPlayer().catchBall(directionToBall);

                if(canSeeGoal)
                    this.getPlayer().kick(60, 135);
                else
                    this.getPlayer().kick(60, 0);
            }
            else if(canSeeGoal || canSeePenalty)
            {
                if(distanceToBall < 2)
                {
                    needsToRetreat = true;

                    getPlayer().turn(directionToBall);
                    getPlayer().dash(randomDashValueFast());
                }
                else
                {
                    needsToRetreat = true;

                    getPlayer().turn(directionToBall);
                    getPlayer().dash(randomDashValueVeryFast());
                }
            }
        }
        else
        {
            if(!canSeeGoal && !needsToRetreat)
            {
                if(!canSeePenalty)
                {
                    getPlayer().turn(90);
                    getPlayer().dash(randomDashValueFast());
                }
                else if((canSeeGoalLeft || canSeeGoalRight) && !canSeeFieldEnd)
                {
                    getPlayer().turn(-1.0 * goalTurn);
                    getPlayer().dash(randomDashValueSlow());
                }
                else
                    getPlayer().turn(25 * directionMultiplier);
            }
            else
            {
                if(!canSeeGoal)
                {
                    getPlayer().turn(90);
                    getPlayer().dash(randomDashValueSlow());
                }
                else if(distanceGoal > 3.5)
                {
                    if(!alreadySeeingGoal)
                    {
                        getPlayer().turn(directionOwnGoal);
                        alreadySeeingGoal = true;
                    }

                    getPlayer().dash(randomDashValueVeryFast());
                }
                else
                {
                    needsToRetreat = false;

                    if(alreadySeeingGoal)
                    {
                        getPlayer().turn(goalTurn);
                        alreadySeeingGoal = false;
                    }
                    else
                        alreadySeeingGoal = true;
                }
            }
        }
    }



    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                               double bodyFacingDirection, double headFacingDirection)
    {
        canSeeFieldEnd = true;
    }


    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOwn(Flag flag, double distance, double direction, double distChange,
                                      double dirChange, double bodyFacingDirection, double headFacingDirection)
    {
        canSeePenalty = true;
    }


    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagGoalOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                                   double bodyFacingDirection, double headFacingDirection)
    {
        if(!alreadySeeingGoal)
            directionMultiplier *= -1.0;

        if(flag.compareTo(Flag.CENTER) == 0)
        {
            distanceGoal = distance;
            directionOwnGoal = direction;

            canSeeGoal = true;

            goalTurn = 180;
        }
        if(flag.compareTo(Flag.LEFT) == 0)
        {
            canSeeGoalLeft = true;
            goalTurn = 90;
        }
        if(flag.compareTo(Flag.RIGHT) == 0)
        {
            canSeeGoalRight = true;
            goalTurn = -90;
        }
    }
}
