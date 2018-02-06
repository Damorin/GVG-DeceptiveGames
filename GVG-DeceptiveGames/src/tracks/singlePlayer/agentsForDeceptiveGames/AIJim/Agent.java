package tracks.singlePlayer.agentsForDeceptiveGames.AIJim;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

//import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

//import org.json.JSONException;
//import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {

    public static int NUM_ACTIONS;
    public static int ROLLOUT_DEPTH = 10;
//    public static double K = Math.sqrt(2);
    public static double K = 0;
    public static Types.ACTIONS[] actions;
    public static boolean USE_EVO = true;
    public static boolean USE_KB = true;
    public static boolean USE_DECISIVE = false;
    public static boolean USE_SHORTESTPATH = true;
    public static boolean USE_USEFULMOVEACTION = false;
    public static boolean USE_BETTEREVO = true;
    public static boolean USE_BIASMUTATION = true;
    public static boolean USE_KNOWLEDGEFACTOR = true;
    public static boolean USE_RESETSCORECHANGE = false;
    public static boolean USE_COURAGEOVERTIME = true;
    
    /**
     * Random generator for the agent.
     */
    private SingleMCTSPlayer mctsPlayer;
    
    private KnowledgeBase kb;
    private Evo evo;

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
    	Random rand = new Random();
    	
        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        for(int i = 0; i < actions.length; ++i)
        {
            actions[i] = act.get(i);
        }
        NUM_ACTIONS = actions.length;

        //Create the player.
        mctsPlayer = new SingleMCTSPlayer(rand);
        
        if (USE_EVO) {
        	kb = new KnowledgeBase();
        	evo = new Evo(kb, rand);
        }
        
        if (USE_SHORTESTPATH) {
        	ShortestPath.initialize(so);
        }
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

//        ArrayList<Observation> obs[] = stateObs.getFromAvatarSpritesPositions();
//        ArrayList<Observation> grid[][] = stateObs.getObservationGrid();

        //Set the state observation object as the new root of the tree.
        mctsPlayer.init(stateObs);
        
        if (USE_EVO) {
	        kb.update(stateObs, true);
	        evo.doNewCycle();
        }
        
        if (USE_SHORTESTPATH && stateObs.getGameTick() % 200 == 0) // every 200 gamesteps clear the unreachableFeatureIds
    		ShortestPath.unreachableFeatureIds.clear();
        
        if (USE_RESETSCORECHANGE && stateObs.getGameTick() % 200 == 0) // reset score change every 200 gamesteps
        	kb.resetScoreChange();
        
        //Determine the action using MCTS...
        int action = mctsPlayer.run(elapsedTimer, kb, evo);
        
        //// for testing only ////
//        if (stateObs.getGameTick() > 50 && stateObs.getGameTick() <= 50) {
//        	MCTSTrees.put(mctsPlayer.getSingleTree());
        	
//        	JSONObject JSONFile = new JSONObject();
            
//            try {
//    	        JSONObject data = new JSONObject().put("data", MCTSTrees);
//    	        JSONFile.put("core", data);
//            } catch (JSONException ex) {
//            	System.out.println(ex);
//            }
            
//            System.out.println("Average nr of rollouts: " + (nrOfRollouts / iterations));

//            try {
//	            logger.write(JSONFile.toString());
//	            logger.close();
//            }
//            catch(Exception e) {
//            	
//            }
            
//            try {
//            	Thread.sleep(3000);
//            } catch(Exception e){}
//        }
//        nrOfRollouts += mctsPlayer.m_root.nVisits;
//        iterations++;

        //... and return it.
        return actions[action];
    }
    
    public static boolean isMoveAction(Types.ACTIONS action) {
    	if(action == Types.ACTIONS.ACTION_DOWN || action == Types.ACTIONS.ACTION_UP
    			|| action == Types.ACTIONS.ACTION_LEFT || action == Types.ACTIONS.ACTION_RIGHT)
    		return true;
    	return false;
    }

//    @Override
//    public void draw(Graphics2D g)
//    {
//        g.drawString("avg rollouts: " + (nrOfRollouts/iterations), 10, 40);
//    }
}
