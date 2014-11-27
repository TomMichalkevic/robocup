/* IN2001 - Artificial Intelligence - Coursework 1 - Chris Mitchelmore, Tomas Michalkevic - PEAS

Robocup Task Environment
Agent type: Soccer player
Performance measure: Can succeed in scoring a goal against an opposing team. Can also defend against the opposing team and prevent them from scoring a goal.
Environment: 2D soccer field.
Actuators: Agent’s movement on the field and its interaction with the ball. Agent’s interaction with other players, referee and a coach.
Sensors: Virtual flag locations and ability to her other players, coach and the referee. Ability to see player locations and the location of the ball.

Properties of the task environment:
Partially observable: the agents can only see the ball if the distance between the player and the ball is under the maximum visibility distance, otherwise they have to operate using other sensor data such as flags and goal locations.
Multi-agent: All the players in a team have to work together to achieve the mutual goal of scoring a goal and stopping the other team from doing the same. There are at least 4 distinct agents on each team: goalkeeper, midfielder, defender, and striker.
Stochastic: The environment is stochastic because there is no way to determine the behavior of the players in the other team reliably. The only option is to react to actions of the other team real-time.
Episodic: This is because the actions taken in each previous game do not affect the actions taken in the current or future games.
Dynamic: The state of the environment is constantly changing regardless whether an agent performs an action or not. The positions of the players and the ball on the field constantly change.
Continuous: Agents have to cooperate with other players on the field
Known: Ordinary laws of physics are applied to the environment. Players can move in different directions and a ball can be kicked which will go in the given directions and then slowly stop.

The agents coded are simple reflex agents who perform actions at a given time based on if statements.


*/


//~--- non-JDK imports --------------------------------------------------------
import com.github.robocup_atan.atan.model.ActionsPlayer;
import com.github.robocup_atan.atan.model.ControllerPlayer;
import com.github.robocup_atan.atan.model.enums.Errors;
import com.github.robocup_atan.atan.model.enums.Flag;
import com.github.robocup_atan.atan.model.enums.Line;
import com.github.robocup_atan.atan.model.enums.Ok;
import com.github.robocup_atan.atan.model.enums.PlayMode;
import com.github.robocup_atan.atan.model.enums.RefereeMessage;
import com.github.robocup_atan.atan.model.enums.ServerParams;
import com.github.robocup_atan.atan.model.enums.ViewAngle;
import com.github.robocup_atan.atan.model.enums.ViewQuality;
import com.github.robocup_atan.atan.model.enums.Warning;
import java.util.ArrayList;
import org.apache.log4j.Logger;

//~--- JDK imports ------------------------------------------------------------
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * @author Team Turing Autonoma
 */
public class Simple implements ControllerPlayer {
    private static int    count         = 0;
    private static Logger log           = Logger.getLogger(Simple.class);
    private Random        random        = null;
    private boolean       canSeeOwnGoal = false;
    private boolean       canSeeNothing = true;
    private boolean       canSeeBall    = false;
    private boolean       canSeeOpponentGoal = false;
    private double        directionBall;
    private double        directionOwnGoal;
    private double        directionOpponentGoal;
    private double        directionCenter;
    private double        distanceBall;
    private double        distanceOwnGoal;
    private double        distanceOpponentGoal;
    private double        distanceCenter;
    private double        closestPlayerBallDistance = 100;
    private double        closestPlayerBallDistanceUpperBound = 100;
    private ActionsPlayer player;
    private SeenPlayer    closestPlayer;
    private SeenPlayer    closestOtherPlayer;
    private List<TeamMember> teamMembers = new ArrayList();


    //JG variables
    //This arraylist stores the players that are seen by the agent, things like their facing direction etc for later use
    //The most important bit is that it calculates the seen player's distance to the ball based on the Cosine rule
    private List<SeenPlayer> allPlayers = new ArrayList();

    //Constants
    static final int      WALKSPEED = 20;
    static final int      JOGSPEED = 50;
    static final int      RUNSPEED = 70;
    static final int      SPRINTSPEED = 100;
    static double         POSSESSIONDISTANCE = 1.5;
    static double         POSSESSIONRADIUS = 5.0;

    //Stores the kick dir, kick power, run speed and turn direction in a variable for later, this is better than calling dash 100 times for example
    private double        finalKickDirection = 0.0;
    private int           finalKickPower = 0;
    private int           finalRunSpeed = 0;
    private double        finalTurnDirection = 0;

    //If the player mode is playon this is true
    private boolean       playOn = false;
    //If the agent instance is in possession of the ball then this is true
    private boolean       inPossessionOfBall = false;
    //If I updated the arraylist then this is true, if it is false it is likely im off pitch or offside
    private boolean       updatedThePlayerList = false;
    //Sets which player has the ball if it isn't me, data is based on the distances obtained inside the allPlayers arraylist
    private SeenPlayer    playerWithTheBall;

    /**
     * Constructs a new simple client.
     */
    public Simple() {
        random = new Random(System.currentTimeMillis() + count);
        count++;
    }

    /** {@inheritDoc} */
    @Override
    public ActionsPlayer getPlayer() {
        return player;
    }

    /** {@inheritDoc} */
    @Override
    public void setPlayer(ActionsPlayer p) {
        player = p;
    }

    /** {@inheritDoc} */
    @Override
    public void preInfo() {
        canSeeOwnGoal = false;
        canSeeBall    = false;
        canSeeNothing = true;
        canSeeOpponentGoal = false;
        inPossessionOfBall = false;
        finalKickDirection = 0.0;
        finalKickPower = 0;
        finalRunSpeed = 60;
        finalTurnDirection = 0;
        canSeeOpponentGoal = false;
        updatedThePlayerList = false;
    }

    /** {@inheritDoc} */
    @Override
    public void postInfo() {
        switch (this.getType().charAt(0)) {
            case 'G':
                if (canSeeNothing) {
                    canSeeNothingAction(); //Goalie can't see anything
                } else if (canSeeBall) {
                    canSeeBallAction();  //Goalie can see the ball
                }else if (canSeeOwnGoal) {
                    canSeeOwnGoalAction(); //Goalie can see its own Goal                    
                }else {
                    canSeeAnythingAction(); //Goalie can see anything
                }
                break;
            case 'A':
                findClosestPlayerToBall();
                if (canSeeNothing) { //can see nothing
                    canSeeNothingAction();
                } else if (canSeeOwnGoal) { //Can see our own goal
                    canSeeOwnGoalAction();
                }  else if (canSeeBall) { //Can see the ball
                    canSeeBallAction();
                } else { //Cant see ball or goal but can see something
                    canSeeAnythingAction();
                }
                //Use variables to assign the players actions after the logic has been run
                //This stops dash, kick and turn being called multiple times
                if (finalTurnDirection != 0) {
                    //If the player needs to turn then they will turn
                    this.getPlayer().turn(finalTurnDirection);
                }
                if (finalRunSpeed != 0.0) {
                    //If they need to run and the mode is playon then they will run
                    if (this.playOn == true) {
                        this.getPlayer().dash(finalRunSpeed);
                    } else if (this.playOn == false) {
                        if (this.distanceBall > POSSESSIONDISTANCE) {
                            this.getPlayer().dash(finalRunSpeed);
                        }
                    }
                }
                //If the playmode is playon and if the agent needs to kick the ball and they are in possession of the ball and the ball is within the possession distance then kick it
                if (finalKickPower != 0 && playOn && inPossessionOfBall && distanceBall < POSSESSIONDISTANCE) {
                    this.getPlayer().kick(finalKickPower, finalKickDirection);
                }
                break;
            case 'M': // Players 4,5,6,7,8 checks to see which action should be taken depending on what the agent can actually see
                if (canSeeNothing) {
                    canSeeNothingAction();
                } else if (canSeeBall) {
                    canSeeBallAction();
                } else if (canSeeOwnGoal) {
                    canSeeOwnGoalAction();
                } else {
                    canSeeAnythingAction();
                }
                break;

            /**MP- Checks actuators to decide what actions to take given the viewed percept **/
            case 'D': //MP- Players 9, 10, 11.
                if (canSeeNothing) {//MP- Execute when you can see nothing
                    canSeeNothingAction();
                } else if (canSeeBall) {//MP- Exectue when you can see only the ball
                    canSeeBallAction();
                } else if (canSeeOwnGoal) {//MP- Exectue when you can see only our goal
                    canSeeOwnGoalAction();
                } else {//MP- Can see objects on the pitch               
                    canSeeAnythingAction();
                }
                break;
            default :
                throw new Error("<<<--- POST INFO PLAYER NOT FOUND --->>>");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagRight(Flag flag, double distance, double direction, double distChange, double dirChange,
                                 double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagLeft(Flag flag, double distance, double direction, double distChange, double dirChange,
                                double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                               double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagOther(Flag flag, double distance, double direction, double distChange, double dirChange,
                                 double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCenter(Flag flag, double distance, double direction, double distChange, double dirChange,
                                  double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
        directionCenter = direction;
        distanceCenter = distance;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                                     double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagCornerOther(Flag flag, double distance, double direction, double distChange,
                                       double dirChange, double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOwn(Flag flag, double distance, double direction, double distChange,
                                      double dirChange, double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagPenaltyOther(Flag flag, double distance, double direction, double distChange,
                                        double dirChange, double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagGoalOwn(Flag flag, double distance, double direction, double distChange, double dirChange,
                                   double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
        if (flag == Flag.CENTER) {
            canSeeOwnGoal    = true;
            distanceOwnGoal  = distance;
            directionOwnGoal = direction;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeFlagGoalOther(Flag flag, double distance, double direction, double distChange, double dirChange,
                                     double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
        canSeeOpponentGoal = true;
        //Primary target is middle of goal
        if (flag == flag.CENTER) {
            directionOpponentGoal = direction;
            distanceOpponentGoal = distance;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeLine(Line line, double distance, double direction, double distChange, double dirChange,
                            double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing = false;
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeePlayerOther(int number, boolean goalie, double distance, double direction, double distChange,
                                   double dirChange, double bodyFacingDirection, double headFacingDirection) {
        //Switch statement for custom operation of the Attackers
        switch (this.getPlayer().getNumber()) {
            case 2: case 3:
                SeenPlayer seenPlayer = new SeenPlayer(number, goalie, distance, direction, distChange,
                        dirChange, bodyFacingDirection, headFacingDirection, false, true);
                //If current agent can see the ball aswell as this player then calculate the distance to them
                if (canSeeBall == true) {
                    seenPlayer.calculateDistance(distanceBall, distance, directionBall, direction);
                } else {
                    //Else set the distance to far away
                    seenPlayer.distanceFromBall = 1000;
                }
                boolean found = false;
                //This arraylist is for storing the seen players in memory, if the player has not been seen before then they are added to the array
                //Loop through and find if the player already exists in the arraylist
                for (int i = 0; i < allPlayers.size(); ++i) {
                    SeenPlayer player = allPlayers.get(i);
                    if (player.number == number && player.isTeammate == false) {
                        //update their details in the arraylist
                        allPlayers.set(i, seenPlayer);
                        found = true;
                        updatedThePlayerList = true;
                    }
                }
                //If they haven't been found in the arraylist then create them
                if (found == false) {
                    allPlayers.add(seenPlayer);
                    updatedThePlayerList = true;
                }
            default:
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeePlayerOwn(int number, boolean goalie, double distance, double direction, double distChange,
                                 double dirChange, double bodyFacingDirection, double headFacingDirection) {

        teamMembers.clear(); //clears the teamMembers array so that it can be repopulated with the most uptodate values
        TeamMember teamMembers = new TeamMember(number, distance, direction, distChange,dirChange);
        if (canSeeBall == true) { //if the agent can see the ball
            teamMembers.calculateDistance(distanceBall, distance, directionBall, direction); //calculate the relative distance to the ball
        }
        this.teamMembers.add(teamMembers); //adds the team member to the array

        switch (this.getPlayer().getNumber()) {
            case 2: case 3:
                SeenPlayer seenPlayer = new SeenPlayer(number, goalie, distance, direction, distChange,
                        dirChange, bodyFacingDirection, headFacingDirection, true, false);
                //If the ball can be seen then calculate the distance between it and the seen player
                //Else set the distance to 1000
                if (canSeeBall == true) {
                    seenPlayer.calculateDistance(distanceBall, distance, directionBall, direction);
                } else {
                    seenPlayer.distanceFromBall = 1000;
                }
                boolean found = false;
                //This arraylist is for storing the seen players in memory, if the player has not been seen before then they are added to the array
                //Loop through and find if the player already exists in the arraylist     
                for (int i = 0; i < allPlayers.size(); ++i) {
                    SeenPlayer player = allPlayers.get(i);
                    if (player.number == number && player.isTeammate == true) {
                        //update their details in the arraylist
                        seenPlayer.updated = true;
                        allPlayers.set(i, seenPlayer);
                        found = true;
                        updatedThePlayerList = true;
                    }
                }
                //If they haven't been found in the arraylist then create them
                if (found == false) {
                    seenPlayer.updated = true;
                    allPlayers.add(seenPlayer);
                    updatedThePlayerList = true;
                }

                boolean updatedThePlayerWithTheBall = false;
                //Once arraylist has finished updating find the closest player to the ball
                if (allPlayers.size() > 0) {
                    //Assume the first seen player has the ball if null
                    if (playerWithTheBall == null) {
                        playerWithTheBall = allPlayers.get(0);
                    }
                    //But we want to try to prove it wrong, someone else could have the ball...
                    for (int i = 0; i < allPlayers.size(); ++i) {
                        SeenPlayer player = allPlayers.get(i);

                        //If the seen player is within the possession radius we Count them as having the ball
                        //and if they are closer to the ball than the current teammate asigned as in possession and their a teammate and their details have been updated
                        //then set them to in possession
                        if (player.isTeammate && player.updated == true && player.distanceFromBall < POSSESSIONRADIUS && player.distanceFromBall <= playerWithTheBall.distanceFromBall) {
                            playerWithTheBall = player;
                            updatedThePlayerWithTheBall = true;
                        } else if (player.updated == false || (allPlayers.size() == i+1 && updatedThePlayerWithTheBall == false)) {
                            //If none is updated to be nearer the ball then none has the ball
                            playerWithTheBall = null;
                        }
                    }
                }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoSeeBall(double distance, double direction, double distChange, double dirChange,
                            double bodyFacingDirection, double headFacingDirection) {
        canSeeNothing      = false;
        canSeeBall    = true;
        distanceBall  = distance;
        directionBall = direction;

        for (int i = 0; i < teamMembers.size(); i++) { //loops through the team members array
            TeamMember member = teamMembers.get(i); //selects the team member at current array position            
            if(closestPlayerBallDistance == 0.0 && closestPlayerBallDistanceUpperBound == 2.5){ //checks the current values of the global variables for player distance to ball            
                closestPlayerBallDistance = 100; //resets the global variable to 100 the maximum length of the pitch (as an uninitialised double is 0.0 which would interfere with distance checking
                closestPlayerBallDistanceUpperBound = 100; //resets the global variable to 100 the maximum length of the pitch (as an uninitialised double is 0.0 which would interfere with distance checking
            }
            if(member.distanceFromBall < closestPlayerBallDistance){ // checks to see if the current members distance from the ball is less than the global variable
                closestPlayerBallDistance = member.distanceFromBall; // assigns the global variable to the current closest members distance to the ball
                closestPlayerBallDistanceUpperBound = closestPlayerBallDistance + 2.5; // adds 2.5 to the smallest distance to provide an upper bound to do distance checks against.
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void infoHearReferee(RefereeMessage refereeMessage) {}

    /** {@inheritDoc}
     * This method was altered to move players into their correct positions on the pitch
     * We have attempted to arrange them in a 3-5-2 formation
     */
    @Override
    public void infoHearPlayMode(PlayMode playMode) {
        //If the playmode is before kick off set the players to their positions
        if (playMode == PlayMode.BEFORE_KICK_OFF) {
            this.pause(1000);
            switch (this.getPlayer().getNumber()) {
                case 1 : //Positions Goalie
                    this.getPlayer().move(-50, 0);
                    break;
                case 2 : //Positions Attacker
                    this.getPlayer().move(-7, 5);
                    break;
                case 3 : //Positions Attacker
                    this.getPlayer().move(-10, -10);
                    break;
                case 4 : //Positions Midfielder
                    this.getPlayer().move(-20, 0);
                    break;
                case 5 : //Positions Midfielder
                    this.getPlayer().move(-25, 10);
                    break;
                case 6 : //Positions Midfielder
                    this.getPlayer().move(-25, -10);
                    break;
                case 7 : //Positions Midfielder
                    this.getPlayer().move(-20, 20);
                    break;
                case 8 : //Positions Midfielder
                    this.getPlayer().move(-20, -20);
                    break;
                case 9 : //Positions Defender
                    this.getPlayer().move(-36, 0);
                    break;
                case 10 : //Positions Defender
                    this.getPlayer().move(-30, 15);
                    break;
                case 11 : //Positions Defender
                    this.getPlayer().move(-30, -15);
                    break;
                default :
                    throw new Error("number must be initialized before move");
            }
        } else if (playMode == PlayMode.PLAY_ON) {
            //If the playmode is set to play on then a variable stating it
            switch (this.getPlayer().getNumber()) {
                case 2 : case 3:
                    this.playOn = true;
                    break;
            }
        } else if (playMode != PlayMode.PLAY_ON) {
            //If the playmode is not playon then set the playon variable to false
            switch (this.getPlayer().getNumber()) {
                case 2 : case 3:
                    this.playOn = false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void infoHearPlayer(double direction, String message) {}

    /** {@inheritDoc} */
    @Override
    public void infoSenseBody(ViewQuality viewQuality, ViewAngle viewAngle, double stamina, double unknown,
                              double effort, double speedAmount, double speedDirection, double headAngle,
                              int kickCount, int dashCount, int turnCount, int sayCount, int turnNeckCount,
                              int catchCount, int moveCount, int changeViewCount) {}

    /** {@inheritDoc} */
    @Override
    public String getType() {
        String type;
        switch (this.getPlayer().getNumber()) {
            case 1 : // Set's the player number 1 to be of type Goalie
                type = "Goalie";
                break;
            case 2 : case 3 : // Set's the player numbers 2 and 3 to be of type Attacker
                type = "Attacker";
                break;
            case 4 : case 5 : case 6 : case 7 : case 8 : // Set's the player numbers 4,5,6,7 and 8 to be of type Midfielder
                type = "Midfielder";
                break;
            case 9 : case 10 : case 11 : // Set's the player numbers 9, 10 and 11 to be of type Defender
                type = "Defender";
                break;
            default :
                throw new Error("Player doesn't exist");
        }
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public void setType(String newType) {}

    /** {@inheritDoc} */
    @Override
    public void infoHearError(Errors error) {}

    /** {@inheritDoc} */
    @Override
    public void infoHearOk(Ok ok) {}

    /** {@inheritDoc} */
    @Override
    public void infoHearWarning(Warning warning) {}

    /** {@inheritDoc} */
    @Override
    public void infoPlayerParam(double allowMultDefaultType, double dashPowerRateDeltaMax,
                                double dashPowerRateDeltaMin, double effortMaxDeltaFactor, double effortMinDeltaFactor,
                                double extraStaminaDeltaMax, double extraStaminaDeltaMin,
                                double inertiaMomentDeltaFactor, double kickRandDeltaFactor,
                                double kickableMarginDeltaMax, double kickableMarginDeltaMin,
                                double newDashPowerRateDeltaMax, double newDashPowerRateDeltaMin,
                                double newStaminaIncMaxDeltaFactor, double playerDecayDeltaMax,
                                double playerDecayDeltaMin, double playerTypes, double ptMax, double randomSeed,
                                double staminaIncMaxDeltaFactor, double subsMax) {}

    /** {@inheritDoc} */
    @Override
    public void infoPlayerType(int id, double playerSpeedMax, double staminaIncMax, double playerDecay,
                               double inertiaMoment, double dashPowerRate, double playerSize, double kickableMargin,
                               double kickRand, double extraStamina, double effortMax, double effortMin) {}

    /** {@inheritDoc} */
    @Override
    public void infoCPTOther(int unum) {}

    /** {@inheritDoc} */
    @Override
    public void infoCPTOwn(int unum, int type) {}

    /** {@inheritDoc} */
    @Override
    public void infoServerParam(HashMap<ServerParams, Object> info) {}

    /**
     * This is the action performed when the player can see the ball.
     * It involves running at it and kicking it...
     */
    private void canSeeBallAction() {
        switch (this.getType().charAt(0)) {
            case 'G' :
                if (distanceBall <= 0.7) {
                    if (canSeeOpponentGoal) {
                        turnTowardOpponentGoal();
                        kickAtFirstTeammate();
                    } else {
                        turnTowardOpponentGoal();
                        kickAtFirstTeammate();
                    }
                } else if (distanceBall > 0.8 && distanceBall <= 5) {//MP- If in the box, where hands are allowed
                    turnTowardBall();
                    getPlayer().dash(SPRINTSPEED);//MP- Run to the ball
                    if (distanceBall <= 0.7) {
                        turnTowardOpponentGoal();
                        kickAtFirstTeammate();
                    }
                    getPlayer().catchBall(directionBall);//MP- Once infront of the ball, pick it up
                    turnTowardOpponentGoal();
                    kickAtFirstTeammate();
                } else if (distanceBall > 5 && distanceBall < 10) {//MP- Ball close, but not able to pick up
                    turnTowardBall();
                    getPlayer().dash(SPRINTSPEED);//MP- Run to the ball
                    if (distanceBall <= 0.7) {
                        turnTowardOpponentGoal();
                        kickAtFirstTeammate();
                    }
                    turnTowardOpponentGoal();
                    kickAtFirstTeammate();//MP- Once infront of the ball attempt to kick it at a teammate
                } else {//MP- Ball out of acceptable range, wait till closer
                    turnTowardBall();
                }
                break;
            case 'A':
                //Find out if someone has the ball
                //If the player is within shoot range then shoot
                //Else run towards opponent goal
                //Else kick the ball to a teammate
                if (inPossessionOfBall == true) {
                    if (canSeeOpponentGoal && directionOpponentGoal <= 30.0) {
                        //Turn to opp goal
                        finalTurnDirection = directionOpponentGoal;
                        //Kick at goal
                        kickAtGoal();
                    } else if (canSeeOpponentGoal) {
                        //Turn toward ball
                        makeTurnTowardBall();
                        //Dribble towards opponent goal
                        dribbleBallTowardsOppGoal();
                    } else if (updatedThePlayerList) {
                        //Kick ball at a teammate if a player is confirmed as visible
                        kickAtFirstTeammate();
                    } else {
                        //Dribble towards the opponent goal
                        dribbleBallTowardsOppGoal();
                    }
                } else if (inPossessionOfBall == false && playerWithTheBall == null) {
                    //If noone in possession of ball then turn to it
                    makeTurnTowardBall();
                } else {
                    //Run into the attacking space
                    runIntoAttackingSpace();
                }
                break;
            case 'M':  // Players 4/5/6/7/8
                if (distanceBall < 0.7) { //Boolean checks to see if the distance to ball is less than 0.7
                    getPlayer().kick(50, directionOpponentGoal); //get's the current player and kicks the ball with a power of 50 towards the opponent's goal
                    turnTowardBall(); //turns toward the ball after it's been kicked to keep it in sight  
                }else if (closestPlayerBallDistanceUpperBound >= distanceBall){ //Boolean checks to see if the current closets team members distance to the ball is less than this agents
                    turnTowardBall(); //turns toward the ball to make sure agent is directly facing ball
                    getPlayer().dash(RUNSPEED); // fast dashes towards the ball
                }else if (distanceBall > 25){ //Boolean checks to see if the current distance to the ball is greater than 25
                    turnTowardBall(); //turns toward the ball to make sure agent is directly facing ball
                    getPlayer().dash(JOGSPEED); //fast dashes towards the ball to keep with the ball moving across the pitch
                }
                break;

            /**MP- Check if you are near the ball and react by clearing the ball or tackling. Also, stay out of the goal box && don't go past the half way line**/
            case 'D' : //MP-Players 9, 10, 11 
                if (distanceOwnGoal > 7 && distanceOwnGoal < 50) {
                    if (distanceBall < 0.7) { //MP- If infront of the ball, kick it to a team mate
                        turnTowardBall();
                        turnTowardOpponentGoal();
                        kickAtFirstTeammate();
                    } else if (distanceBall >= 0.8 && distanceBall < 15 ) {//MP- If nearby to the ball, run to it and pass it to a team mate
                        turnTowardBall();
                        getPlayer().dash(SPRINTSPEED);
                        turnTowardOpponentGoal();
                        kickAtFirstTeammate();
                    } else if (closestPlayerBallDistanceUpperBound >= distanceBall){//MP- Checks if the current closest teammate is closer to the ball than I am
                        turnTowardBall();
                        getPlayer().dash(RUNSPEED); //MP- Run to the ball
                    } else { //MP- If not near the ball, stay in area
                        turnTowardBall();
                    }
                } else if( distanceOwnGoal < 7 ) {/*MP- Check to see if you are in our own goal area. If not, run back into our own half*/
                    turnTowardOwnGoal();
                    turnOneEighty();
                    getPlayer().dash(randomDashValueFast());
                    if (canSeeBall) {
                        turnTowardBall();
                    } else {
                        turnNinety();
                    }
                } else if( distanceOwnGoal > 50 ) {/*MP- Check to see if you are approaching the halfway line. If not, run back into our own half*/
                    turnTowardOpponentGoal();
                    turnOneEighty();
                    getPlayer().dash(randomDashValueFast());
                    if (canSeeBall) {
                        turnTowardBall();
                    } else {
                        turnNinety();
                    }
                }
                break;
            default :
                throw new Error("<<<--- CAN SEE BALL ACTION PLAYER NOT FOUND --->>>");
        }
    }

    /**
     * If the player can see anything that is not a ball or a goal, it turns.
     */
    private void canSeeAnythingAction() {
        switch (this.getType().charAt(0)) {
            case 'G' :
                //MP- Do nothing, if not the ball or the goal the goalie doesn't need to do anything
                break;
            case 'A' :
                //Will run the player into space, if it has to avoid players it avoids, else it turns to the ball
                if (directionBall != 0.0 && runIntoAttackingSpace() == false) {
                    finalTurnDirection = directionBall;
                    //If the player has noone to avoid then the agent will turn to the best place
                } else if (runIntoAttackingSpace() == false) {
                    //Determine where the player should go as a final resort
                    turnToPosition();
                }
                break;
            case 'M' : // Players 4/5/6/7/8   
                turnFourtyFive(); //Turns the agent 45 degrees until a more desirable action is true                       
                break;

            /**MP- If you can't see the ball or the opposition, maintain position and rotate til another action takes precedence**/
            case 'D' : //MP- Players 9, 10, 11
                turnNinety(); //MP- Keep turning until another percept is detected
                break;
            default :
                throw new Error("<<<--- CAN SEE ANYTHING ACTION PLAYER NOT FOUND --->>>");
        }
    }

    /**
     * If the player can see nothing, it turns 180 degrees.
     */
    private void canSeeNothingAction() {
        switch (this.getType().charAt(0)) {
            case 'G' : //MP- If you can see the ball, face it
                if (canSeeBall) {
                    turnTowardBall();
                } else if (canSeeOpponentGoal) {//MP- If you can't see the ball, face the opposition
                    turnTowardOpponentGoal();
                } else if (canSeeOwnGoal) {//MP- if you can only see your own goal, face the goal, then turn 180 to face opposition
                    turnTowardOwnGoal();
                    turnOneEighty();
                } else {//MP- Last resort, turn and wait for another percept
                    turnNinety();
                }
                break;
            case 'A' :
                //Attacker, as player sees nothing it will turn 180
                finalTurnDirection = 180;
                break;
            case 'M': // Players 4/5/6/7/8   
                turnOneEighty(); // turns agent 180 degrees so that he can hopefully see something to carry out a desirable action 
                break;

            /**MP- If you can see nothing at all, keep turning until a more useful percept is detected**/
            case 'D' : //MP- Players 9, 10, 11
                turnNinety(); //MP- Keep turning until a more useful percept is detected
                break;
            default :
                throw new Error("<<<--- CAN SEE NOTHING ACTION PLAYER NOT FOUND --->>>");
        }
    }

    /**
     * If the player can see its own goal, it goes and stands by it...
     */
    private void canSeeOwnGoalAction() {
        switch (this.getType().charAt(0)) {
            case 'G' :
                if(canSeeOwnGoal) {//MP- facing the wrong direction, need to turn around
                    turnTowardOwnGoal(); //MP- Once you're back infront of the goal, face the opposition
                    turnOneEighty();
                } else {
                    turnNinety();
                    canSeeNothingAction();
                }
                break;
            case 'A' :
                //Attacker does not go near goal so will run out to midfield
                if (inPossessionOfBall && updatedThePlayerList) {
                    //If in possession of ball and there are players visible I am not offside and I can kick to a teammate
                    kickAtFirstTeammate();
                } else if (inPossessionOfBall) {
                    //If in possession of ball turn to opponent goal
                    dribbleBallTowardsOppGoal();
                } else {
                    //Turn around by default
                    finalTurnDirection = 180;
                }
                break;
            case 'M' : // Players 4/5/6/7/8  
                if(canSeeOwnGoal && canSeeBall){ // Boolean check to see if agent can see both their own goal and the ball
                    if (distanceBall < 0.7) { //Boolean checks to see if the distance to ball is less than 0.7
                        getPlayer().kick(50, directionOpponentGoal); //get's the current player and kicks the ball with a power of 50 towards the opponent's goal
                        turnTowardBall(); //Turns toward the ball after it's been kicked to keep it in sight  
                    }else if (closestPlayerBallDistanceUpperBound >= distanceBall){ //Boolean checks to see if the current closets team members distance to the ball is less than this agents
                        turnTowardBall(); //Turns toward the ball to make sure agent is directly facing ball
                        getPlayer().dash(randomDashValueFast()); // fast dashes towards the ball
                    }else if (distanceBall > 25){ //Boolean checks to see if the current distance to the ball is greater than 25
                        turnTowardBall(); //Turns toward the ball to make sure agent is directly facing ball
                        getPlayer().dash(randomDashValueFast()); //Fast dashes towards the ball to keep with the ball moving across the pitch
                    }
                }else{
                    turnNinety(); // Turns agent 90 degrees until a more desirable action can be carried out
                }
                break;

            /**MP- If you are facing our goal you're facing the wrong way. Therefore, either clear the ball from our goal area if you are in possession or 
             *move back to a more appropriate position
             **/
            case 'D' ://MP- Players 9, 10, 11
                if(canSeeOwnGoal && canSeeBall){//MP- If you are facing our goal and have the ball, clear it in the direction of the opponents goal
                    if (distanceBall < 0.7) {
                        getPlayer().kick(50, directionOpponentGoal);
                        turnTowardBall();
                    } else if (distanceBall > 25){//MP- If you are close to the ball but not in possesion, move to it
                        turnTowardBall();
                        getPlayer().dash(randomDashValueFast());
                    }
                } else if (canSeeOpponentGoal) {/*MP- If you don't have the ball and you're facing the wrong way, turn around, if you can see 
                                                    the opponent goal face that, if not, just turn around*/
                    turnTowardOpponentGoal();
                } else {
                    turnOneEighty();
                    if (distanceOwnGoal < 10) {//MP- If you are too close to the goal, move away
                        getPlayer().dash(randomDashValueFast());
                    }
                }
                break;
            default :
                throw new Error("<<<--- CAN SEE OWN GOAL ACTION PLAYER NOT FOUND --->>>");
        }
    }

    /**
     * Randomly choose a fast dash value.
     * @return
     */
    private int randomDashValueFast() {
        return 30 + random.nextInt(100);
    }

    /**
     * Turn towards the ball.
     */
    private void turnTowardBall() {
        getPlayer().turn(directionBall);
    }

    /**
     * Turn towards the ball.
     */
    private void makeTurnTowardBall() {
        finalTurnDirection = directionBall;
    }

    /**
     * Turn towards our goal.
     */
    private void makeTurnTowardOpponentGoal() {
        finalTurnDirection = directionOpponentGoal;
    }

    private void kickAtFirstTeammate() {
        //Will kick towards the first player in the arraylist that is over a certain distance away
        //Beaty of this is that it doesn't require visibility of the players, as the players are in memory it can kick to where the agent thinks the teammate is
        for (int i = 0; i < allPlayers.size(); i++) {
            SeenPlayer player = allPlayers.get(i);
            if (player.distance > POSSESSIONRADIUS && player.isTeammate) {
                //Cast double to int for kick distance
                Double d = player.distance; Integer in = d.intValue();

                //assign the final kick power and direction
                this.finalKickPower = in;
                this.finalKickDirection = player.direction;
                break;
            }
        }
    }

    //If the ball hasn't been kicked then the player can't see any other useful players
    //Therefore they will turn to the opponent goal using a series of kicks and run with the ball
    private void dribbleBallTowardsOppGoal() {
        //The dribble kick power is set
        this.finalKickPower = 10;
        this.finalRunSpeed = JOGSPEED;
        if (canSeeOpponentGoal) {
            //If I can see opponent goal then turn to it if i am not facing it
            if (directionOpponentGoal < -25.0 && directionOpponentGoal > 25.0 ) {
                if (directionOpponentGoal < -25.0) {
                    this.finalKickDirection = 15;
                    finalTurnDirection = 25;
                } else {
                    this.finalKickDirection = -15;
                    finalTurnDirection = -25;
                }
            } else {
                finalTurnDirection = directionOpponentGoal;
            }
        } else {
            //Kick the ball backwards to turn the player around with the ball and to avoid loosing the ball
            this.finalKickDirection = directionOpponentGoal;
            finalTurnDirection = directionOpponentGoal;
        }
    }

    //Will run the player into space avoiding players returns true if player had to be avoided
    //Prioritises moving towards opponent goal
    //Determines which team has the ball and decides whether to retreat or attack
    private boolean runIntoAttackingSpace() {
        //Returns false if none had to be avoided to move
        Boolean status = false;
        for (int i = 0; i < allPlayers.size(); i++) {
            SeenPlayer player = allPlayers.get(i);
            if (player.distance < 5.0 && player.direction > -15 && player.direction < 15) {
                //If a player has to be avoided then set direction to +-45 to stop them colliding badly
                if (player.direction < -15.0) {
                    finalTurnDirection = player.direction - 45;
                } else {
                    finalTurnDirection = player.direction + 45;
                }
                finalRunSpeed =  JOGSPEED;
                //If a player has to be avoided then return true
                status = true;
            }
        }
        //Noone had to be avoided so turn towards the opp goal
        if (status == false) {
            finalTurnDirection = directionOpponentGoal;
        }
        //If the player list hasn't been updated then the player is offside or is off pitch etc
        if (!updatedThePlayerList) {
            //If the player list hasn't been updated then something is wrong so turn around
            finalTurnDirection = 180;
        }
        return status;
    }

    //Used to determine the place that the player will go to if they cannot see the ball or their own goal
    private void turnToPosition() {
        //If the opponents goal can be seen then run forward if not offside, else retreat
        //If the player cannot see an opponent it is likely offside so will turn around
        if (canSeeOpponentGoal) {
            if (updatedThePlayerList == false) {
                finalTurnDirection = 180;
            } else {
                finalTurnDirection = directionOpponentGoal;
            }
        } else {
            //Cant see opponent goal so run into space
            finalRunSpeed = JOGSPEED;
            runIntoAttackingSpace();
        }
    }

    //Finds the closest person to the ball, also determines if it's myself if noone has the ball
    private void findClosestPlayerToBall() {
        if (playerWithTheBall == null) {
            //Check to see if I am nearest the ball and in possession range
            if (canSeeBall && this.distanceBall < POSSESSIONDISTANCE) {
                inPossessionOfBall = true;
            }
        }
    }

    //Assumes that you can see the opponent goal and is within a certain range
    private void kickAtGoal() {
        //Kicks ball at goal with a variance of 10 degrees
        finalKickDirection = directionOpponentGoal - 5 + Math.random() * 10;
        //Cast double to int for kick distance
        Double d = distanceOpponentGoal; Integer in = d.intValue();
        finalKickPower = in * 2;
    }

    /**
     * Turn towards our goal.
     */
    private void turnTowardOwnGoal() {
        getPlayer().turn(directionOwnGoal);
    }

    /**
     * Turn towards our goal.
     */
    private void turnTowardOpponentGoal() {
        getPlayer().turn(directionOpponentGoal);
    }

    /**
     * Pause the thread.
     * @param ms How long to pause the thread for (in ms).
     */
    private synchronized void pause(int ms) {
        try {
            this.wait(ms);
        } catch (InterruptedException ex) {
            log.warn("Interrupted Exception ", ex);
        }
    }

    /**
     * Turns player 90
     */
    private void turnNinety() {
        getPlayer().turn(90);
    }

    /**
     * Turns player 180
     */
    private void turnOneEighty() {
        getPlayer().turn(180);
    }

    /**
     * Turns player 45
     */
    private void turnFourtyFive() {
        getPlayer().turn(45);
    }
}


/**
 *Storage of the seen player inside memory, just returns all of the values
 *This is the cool bit, the idea is that all of the 21 players on pitch(doesn't include self) are stored in the arraylist
 *This arraylist is updated when the player is seen either by creating the instance of them or updating their existing entry
 *The arraylist is the able to be searched through so that I any could at any point find the player, "1" in the array and determine how far they are from the ball
 *And if I can't see the ball for the frame refresh I know that on one of the previous frames they were X distance from ball
 *I can therefore make a decision based on the fact that player "1" was X distance away on a previous frame and I could pass to where I think they are
 *Also this class calculates the seen players distance to the ball if I can see ball along with the player which is used to stop the players always running toward the ball
 *Instead it can be used to determine some sort of possession system
 **/
class SeenPlayer {

    private static Logger log           = Logger.getLogger(SeenPlayer.class);
    public int number;
    public boolean goalie;
    public double distance;
    public double direction;
    public double distChange;
    public double dirChange;
    public double bodyFacingDirection;
    public double headFacingDirection;
    public boolean hasBall;
    public boolean isTeammate;
    public double distanceFromBall;
    public boolean updated;

    public SeenPlayer (int num, boolean goal, double dist, double dir, double distC,
                       double dirC, double bod, double head, boolean teamMate, boolean update) {
        number = num;
        goalie = goal;
        distance = dist;
        direction = dir;
        distChange = distC;
        dirChange = dirC;
        bodyFacingDirection = bod;
        headFacingDirection = head;
        isTeammate = teamMate;
        hasBall = false;
        updated = update;
    }

    //Takes distance to the ball, then the player, then the angle the ball is at and then angle that the other player is at relative to the player
    public void calculateDistance(double distanceA, double distanceB, double angleA, double angleB) {
        //Calulate the angle that the ball/player is at relative
        if (angleA < 0) {
            double angleD = Math.abs(angleA);
            angleB += angleD;
        }
        if (angleB < 0) {
            double angleD = Math.abs(angleB);
            angleA += angleD;
        }
        double angleC = angleA - angleB;
        //Calculates distance of player from ball based on cosine
        //Based on rule here: http://www.mathstat.strath.ac.uk/basicmaths/332_sineandcosinerules.html
        double cosSq = ((Math.pow(distanceA, 2)) + (Math.pow(distanceB, 2))) - (2 * distanceA * distanceB * (Math.cos(Math.toRadians(Math.abs(angleC)))));
        double result = Math.sqrt(cosSq);
        //Returns the players exact distance to the ball
        distanceFromBall = result;
    }

}


/**
 * Team Member ONLY class.
 * Used to store all the appropriate values so that they can be used inside an array to determine
 * factors such as team member distance from ball
 */
class TeamMember {
    public int number;
    public double distance;
    public double direction;
    public double distChange;
    public double dirChange;
    public double distanceFromBall;

    public TeamMember (int number, double distance, double direction, double distChange,double dirChange) {
        this.number = number;
        this.distance = distance;
        this.direction = direction;
        this.distChange = distChange;
        this.dirChange = dirChange;
    }

    //Takes distance to the ball, then the player, then the angle the ball is at and then angle that the other player is at relative to the player
    public void calculateDistance(double distanceABall, double distanceBPlayer, double angleABall, double angleBPlayer) {
        //Calulate the angle that the ball is at relative
        if (angleABall < 0) {
            double angleD = Math.abs(angleABall);
            angleBPlayer += angleD;
        }
        //Calulate the angle that the player is at relative
        if (angleBPlayer < 0) {
            double angleD = Math.abs(angleBPlayer);
            angleABall += angleD;
        }

        double angleC = angleABall - angleBPlayer;
        //Calculates distance of player from ball based on cosine 
        //Based on rule here: http://www.mathstat.strath.ac.uk/basicmaths/332_sineandcosinerules.html
        double cosSq = ((Math.pow(distanceABall, 2)) + (Math.pow(distanceBPlayer, 2))) - (2 * distanceABall * distanceBPlayer * (Math.cos(Math.toRadians(Math.abs(angleC)))));
        double result = Math.sqrt(cosSq);
        distanceFromBall = result;
    }

}