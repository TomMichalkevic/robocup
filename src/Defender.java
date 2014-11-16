
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
        int position = Math.max(getAggression()/6 - 45, -45);
        switch (getPlayer().getNumber()) {
            case LEFT_BACK :
                getPlayer().move(position, -20);
                break;
            case CENTER_LEFT_BACK :
                getPlayer().move(position, -8);
                break;
            case CENTER_RIGHT_BACK :
                getPlayer().move(position, 8);
                break;
            case RIGHT_BACK :
                getPlayer().move(position, 20);
                break;
        }
        getPlayer().dash(dashValueSlow());
    }

}
