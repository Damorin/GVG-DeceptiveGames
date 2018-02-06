package tracks.singlePlayer.agentsForDeceptiveGames.roskvist;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.LinkedList;
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

    private int repeatCounter;
    private int repeatAction;
    private int lastAction;


    //Used to remember last nodes visited
	private final int lastPlacesQueueSize = 5;
    private LinkedList<Vector2d> lastPlacesVisited;
    private int standingStillCounter = 0;

    /**
     * Creates the MCTS player with a sampleRandom generator object.
     * @param a_rnd sampleRandom generator object.
     */
    public SingleMCTSPlayer(Random a_rnd)
    {
        m_rnd = a_rnd;
        //m_root = new SingleTreeNode(a_rnd);

        //init the last node list
        lastPlacesVisited = new LinkedList<Vector2d>();
    }

    /**
     * Inits the tree with the new observation state in the root.
     * @param a_gameState current state of the game.
     */
    public void init(StateObservation a_gameState)
    {
        //Set the game observation to a newly root node.
        m_root = new SingleTreeNode(m_rnd, a_gameState);


    }

    /**
     * Runs MCTS to decide the action to take. It does not reset the tree.
     * @param elapsedTimer Timer when the action returned is due.
     * @return the action to execute in the game.
     */
    public int runReapeatAction(ElapsedCpuTimer elapsedTimer)
    {
        if(repeatCounter>0){
            repeatCounter--;
            System.out.println("RepeatAction: "+repeatAction);
            return repeatAction;
        }

        //Do the search within the available time.
        m_root.mctsSearch(elapsedTimer);

        //Determine the best action to take and return it.
        //int action = m_root.mostVisitedAction();
        repeatAction = m_root.mostVisitedAction();
        System.out.println("NICHT!REPEAT!Action: "+repeatAction);
        repeatCounter = SingleTreeNode.macroRepeat-1;
        //int action = m_root.bestAction();
        return repeatAction;
    }

    public int run(ElapsedCpuTimer elapsedTimer)
    {


    	//Set the list of recently visited nodes
    	SingleTreeNode.lastPlacesVisited = lastPlacesVisited;
    	SingleTreeNode.standingStillCounter = standingStillCounter;

        //Do the search within the available time.
        m_root.mctsSearch(elapsedTimer);

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedAction();
        addPlaceVisited(m_root.children[action]);
        SingleTreeNode.lastRunAction = action;

        //int action = m_root.mostVisitedActionWithRepeatReward(1f, lastAction);
        //lastAction = action;
        //int action = m_root.bestAction();
        return action;
    }

	public void addPlaceVisited(SingleTreeNode node) {
		
		Vector2d pos = node.nodePosition;
		
		if(pos.equals(lastPlacesVisited.peekLast())) {
			standingStillCounter++;
		}
		else {
			standingStillCounter = 0;
		}
		
//		//System.out.println("Queue size " + lastPlacesVisited.size());
//		if(lastPlacesVisited.contains(pos)) {
//			return;
//		}
//		
		if(lastPlacesVisited.size() < lastPlacesQueueSize) {
			lastPlacesVisited.add(pos);
			
		}
		else {
			lastPlacesVisited.remove();
			lastPlacesVisited.add(pos);
			//System.out.println("add");
		}
		

	}

}
