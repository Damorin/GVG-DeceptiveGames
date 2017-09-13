package tracks.singlePlayer.submittedAgents.ToVo1;

import core.game.StateObservation;
import core.game.StateObservationMulti;
import tools.ElapsedCpuTimer;

import java.util.Random;

public class SingleMCTSPlayer
{


    /**
     * Root of the tree.
     */
    public SingleTreeNode m_root = null;

    /**
     * Random generator.
     */
    public Random m_rnd;


    public SingleMCTSPlayer(Random a_rnd)
    {
        m_rnd = a_rnd;
        for(int i = 0; i < SingleTreeNode.par_rollout_depth_max+1; i++)
            SingleTreeNode.episode[i] = new SingleTreeNode.EpisodeEntry();
        SingleTreeNode.InitStatics();
    }

    /**
     * Inits the tree with the new observation state in the root.
     * @param a_gameState current state of the game.
     */
    public void init(StateObservation a_gameState)
    {
        double currentScore = SingleTreeNode.value(a_gameState);

        //Set the game observation to a newly root node.
        //System.out.println("learning_style = " + learning_style);
        if((m_root == null)||(SingleTreeNode.nextRootNode == null)) {
            SingleTreeNode.lastRealReward = 0.0;
            m_root = new SingleTreeNode(m_rnd);
        }
        else{
            SingleTreeNode.lastRealReward = currentScore - SingleTreeNode.rootNode.stateScore;

            m_root = SingleTreeNode.nextRootNode;
            m_root.parent = null;
            SingleTreeNode.m_depth_offset = m_root.m_depth;

            m_root.nVisits = m_root.nVisits * Math.pow(1.0-SingleTreeNode.par_forgettingRate_search, SingleTreeNode.currentSearchID - m_root.lastEvaluationSearch);
            m_root.lastEvaluationSearch = SingleTreeNode.currentSearchID;

        }

        m_root.stateScore = currentScore;
        SingleTreeNode.rootNode = m_root;
        SingleTreeNode.rootState = a_gameState;

    }

    /**
     * Runs MCTS to decide the action to take. It does not reset the tree.
     * @param elapsedTimer Timer when the action returned is due.
     * @return the action to execute in the game.
     */
    public int run(ElapsedCpuTimer elapsedTimer)
    {
        //Do the search within the available time.
        m_root.mctsSearch(elapsedTimer);

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedActionEnhanced();
        //int action = m_root.bestAction();
        return action;
    }

}
