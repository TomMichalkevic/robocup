
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
     */
    @Override
    public ControllerPlayer getNewControllerPlayer(int number)
    {
        return new Simple();
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
