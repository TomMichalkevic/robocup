
//~--- non-JDK imports --------------------------------------------------------

import com.github.robocup_atan.atan.model.AbstractTeam;
import com.github.robocup_atan.atan.model.ControllerCoach;
import com.github.robocup_atan.atan.model.ControllerPlayer;

/**
 * A class to setup a Simple Silly AbstractTeam.
 *
 * @author Atan
 */
public class SimplySillyTeam extends AbstractTeam {

    /**
     * Constructs a new simple silly team.
     *
     * @param name The team name.
     * @param port The port to connect to SServer.
     * @param hostname The SServer hostname.
     * @param hasCoach True if connecting a coach.
     */
    public SimplySillyTeam(String name, int port, String hostname, boolean hasCoach) {
        super(name, port, hostname, hasCoach);
    }

    /**
     * {@inheritDoc}
     *
     * The first controller of the team is the goalie and the others are players (11 is for the captain).
     * Player numbers are 1-11 but number is 0-10 so we add one to correct for this.
     * We define constants in Player class to make code that differentiates between players clearer.
     */
    @Override
    public ControllerPlayer getNewControllerPlayer(int number) {
        number++;
        if (number == Player.GOALIE) {
            return new Goalie();
        } else if (number == Player.LEFT_BACK ||
                number == Player.CENTER_LEFT_BACK ||
                number == Player.CENTER_RIGHT_BACK ||
                number == Player.RIGHT_BACK ) {
            return new Defender();
        } else if (number == Player.LEFT_WING ||
                number == Player.CENTER_LEFT_MIDFIELD ||
                number == Player.CENTER_RIGHT_MIDFIELD ||
                number == Player.RIGHT_WING ) {
            return new Midfielder();
        }else if (number == Player.CENTER_LEFT_FORWARD ||
                  number == Player.CENTER_RIGHT_FORWARD) {
            return new Striker();
        } else {
            return new Simple();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Generates a new coach.
     */
    @Override
    public ControllerCoach getNewControllerCoach() {
        return new Coach();
    }
}
