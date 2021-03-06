package tracks.singlePlayer.agentsForDeceptiveGames.AIJim;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:13
 */
public class SingleMCTSPlayer
{
    /**
     * Root of the tree.
     */
    public SingleTreeNode m_root;

    /**
     * Random generator.
     */
    public Random m_rnd;

    /**
     * Creates the MCTS player with a sampleRandom generator object.
     * @param a_rnd sampleRandom generator object.
     */
    public SingleMCTSPlayer(Random a_rnd)
    {
        m_rnd = a_rnd;
        m_root = new SingleTreeNode(a_rnd);
    }

    /**
     * Inits the tree with the new observation state in the root.
     * @param a_gameState current state of the game.
     */
    public void init(StateObservation a_gameState)
    {
        //Set the game observation to a newly root node.
        m_root = new SingleTreeNode(m_rnd);
        m_root.state = a_gameState;

    }

    /**
     * Runs MCTS to decide the action to take. It does not reset the tree.
     * @param elapsedTimer Timer when the action returned is due.
     * @return the action to execute in the game.
     */
    public int run(ElapsedCpuTimer elapsedTimer, KnowledgeBase kb, Evo evo)
    {
        //Do the search within the available time.
        m_root.mctsSearch(elapsedTimer, kb, evo);

        //Determine the best action to take and return it.
        int action = m_root.getAction();
        //int action = m_root.bestAction();
        
        return action;
    }
    
    // for testing only ////////////////////////
//    public JSONObject getSingleTree() {
//    	return m_root.nodeToJSON();
//    }
    // for testing only ////////////////////////
}
