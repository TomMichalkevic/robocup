
//~--- non-JDK imports --------------------------------------------------------

import com.github.robocup_atan.atan.model.enums.Flag;

//~--- JDK imports ------------------------------------------------------------


/**
 * <p>Defender class.</p>
 *
 * @author The Turing Autonoma
 */
public class Midfielder extends Player {

    public Midfielder() {
        super();
        playerType = "Midfielder";
    }
    protected void playerHasBallAction()
    {
        moveToHoldingPosition();
    }

    protected void ballIsVeryCloseAction()
    {
        moveToHoldingPosition();
    }

    protected void ballIsCloseAction()
    {
        moveToHoldingPosition();
    }

    protected void ballIsFarAction()
    {
        moveToHoldingPosition();
    }

    protected void ballNotVisibleAction()
    {
        moveToHoldingPosition();
    }


    protected void moveToHoldingPosition()
    {
        // Don't let defenders move to far back!
        int position = Math.max(getAggression()/4 - 30, -35);
        switch (getPlayer().getNumber()) {
            case LEFT_WING :
                getPlayer().move(position, -20);
                break;
            case CENTER_LEFT_MIDFIELD :
                getPlayer().move(position, -8);
                break;
            case CENTER_RIGHT_MIDFIELD :
                getPlayer().move(position, 8);
                break;
            case RIGHT_WING :
                getPlayer().move(position, 20);
                break;
        }
        getPlayer().dash(dashValueSlow());
    }

}
