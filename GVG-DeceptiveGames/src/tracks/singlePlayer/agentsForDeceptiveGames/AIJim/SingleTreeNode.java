package tracks.singlePlayer.agentsForDeceptiveGames.AIJim;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;

//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class SingleTreeNode
{
    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    public static double epsilon = 1e-6;
    public static double egreedyEpsilon = 0.05;
    public StateObservation state;
    public SingleTreeNode parent;
    public SingleTreeNode[] children;
    public double totValue;
    public int nVisits;
    public Random m_rnd;
    private int m_depth;
    protected static double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};

    /// for testing only ///
    public ACTIONS action;
    public double knowledgeValue;
    public double distanceValue;
    public double scoreValue;
    /////////////////////
    
    
    public SingleTreeNode(Random rnd) {
        this(null, null, rnd, null);
    }

    public SingleTreeNode(StateObservation state, SingleTreeNode parent, Random rnd, ACTIONS thisAction) {
        this.state = state;
        this.parent = parent;
        this.m_rnd = rnd;
        children = new SingleTreeNode[Agent.NUM_ACTIONS];
        this.totValue = 0.0;
        
        /// for testing only ///
        this.action = thisAction; 
        this.knowledgeValue = 0.0;
        this.distanceValue = 0.0;
        this.scoreValue = 0.0;
        /////////////////////////////
        
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
        
        
        /// for testing only ///
//        if (state != null)
//        ArrayList<Types.ACTIONS> act = state.getAvailableActions();
//        actions = new Types.ACTIONS[act.size()];
//        for(int i = 0; i < actions.length; ++i)
//        {
//            actions[i] = act.get(i);
//        }
    }


    public void mctsSearch(ElapsedCpuTimer elapsedTimer, KnowledgeBase kb, Evo evo) {
    	
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
//        double acumTimeTreePolicy = 0;
//        double acumTimeRollout = 0;
//        double acumTimeBackup = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;
        
//        while(remaining > 40) {
//        	remaining = elapsedTimer.remainingTimeMillis();
//        }

        int remainingLimit = 5;
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit)
        {
        	// get a new weight matrix, and do a new cycle if all are already evaluated
        	WeightMatrix wm = getNextMatrix(evo);
        		
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            
            SingleTreeNode selected = treePolicy();
//            acumTimeTreePolicy += elapsedTimerIteration.elapsedMillis();
//            double timePoint = elapsedTimerIteration.elapsedMillis();
            
            double[] scores = selected.rollOut(kb, wm); 
//            acumTimeRollout += (elapsedTimerIteration.elapsedMillis() - timePoint);
            
            backUp(selected, scores);

            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
//            acumTimeBackup = acumTimeTaken - (acumTimeTreePolicy + acumTimeRollout);

            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
//            System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
        }
//        System.out.println("-- " + numIters + " -- ( total:" + acumTimeTaken + ", TreePolicy:" + acumTimeTreePolicy + ", Rollout:" + acumTimeRollout + ", Backup:" + acumTimeBackup + ")");
    }
    
    private WeightMatrix getNextMatrix(Evo evo)
    {
    	WeightMatrix wm;
    	if (Agent.USE_EVO)
    	{
        	wm = evo.getUnevaluatedMatrix();
        	if(wm == null) {
        		if(Agent.USE_BETTEREVO) // adds a mutation to the population for each rollout
        			evo.growPopulationFromSingle(evo.currentBest);
        		else
        			evo.doNewCycle();
        		wm = evo.getUnevaluatedMatrix();
        		if (wm == null)
        			System.out.println("no matrices to evaluate...");
        	}
        	return wm;
    	}
    	
    	return null;
    }

    public SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        while (!cur.state.isGameOver() && cur.m_depth < Agent.ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                SingleTreeNode expandedNode = cur.expand();
                if (expandedNode.state != null)
                	return expandedNode;
            }

            SingleTreeNode next = cur.uct();
            //SingleTreeNode next = cur.egreedy();
//          System.out.println("uct " + next.action);
            cur = next;
        }

        return cur;
    }


    public SingleTreeNode expand() {

        int bestAction;
        double bestValue;
        StateObservation nextState;
        
//        System.out.println("Expand");

        while(true) 
        {
        	bestAction = 0;
        	bestValue = -1;
	        for (int i = 0; i < children.length; i++) {
	            double x = m_rnd.nextDouble();
	            if (x > bestValue && children[i] == null) {
	                bestAction = i;
	                bestValue = x;
	            }
	        }
	        
	        if(bestValue < 0) //we tried all moves, we're not expanding
            	return new SingleTreeNode(this.m_rnd);;
	        
	        ACTIONS action = Agent.actions[bestAction];
	        nextState = state.copy();
	        nextState.advance(Agent.actions[bestAction]);

	        if(Agent.USE_USEFULMOVEACTION && // don't use move actions that don't actually move the agent
	        		(action == ACTIONS.ACTION_DOWN || action == ACTIONS.ACTION_UP
	        		|| action == ACTIONS.ACTION_LEFT || action == ACTIONS.ACTION_RIGHT)) {
	        	
//	        	System.out.println("  " + nextState.getAvatarPosition().toString() + "   " + state.getAvatarPosition().toString());
	        	
	        	if(nextState.getAvatarPosition().equals(state.getAvatarPosition())) {
//	        		System.out.println("  EQUAL");
		        	children[bestAction] = new SingleTreeNode(this.m_rnd); //empty node
		        	
		        	continue;
	        	}
	        }
	        
	        break;
        }

        SingleTreeNode tn = new SingleTreeNode(nextState, this, this.m_rnd, Agent.actions[bestAction]);
        children[bestAction] = tn;
        
        return tn;
    }

    public SingleTreeNode uct() {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode child : this.children)
        {
        	if(child.state == null)
        		continue;
        	
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + SingleTreeNode.epsilon);


            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);

            double uctValue = childValue +
                    Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + SingleTreeNode.epsilon));

            // small sampleRandom numbers: break ties in unexpanded nodes
            uctValue = Utils.noise(uctValue, SingleTreeNode.epsilon, this.m_rnd.nextDouble());     //break ties randomly

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }

        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
        }

        return selected;
    }

    public SingleTreeNode egreedy() {


        SingleTreeNode selected = null;

        if(m_rnd.nextDouble() < egreedyEpsilon)
        {
            //Choose randomly
            int selectedIdx = m_rnd.nextInt(children.length);
            selected = this.children[selectedIdx];

        }else{
            //pick the best Q.
            double bestValue = -Double.MAX_VALUE;
            for (SingleTreeNode child : this.children)
            {
                double hvVal = child.totValue;
                hvVal = Utils.noise(hvVal, SingleTreeNode.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                // small sampleRandom numbers: break ties in unexpanded nodes
                if (hvVal > bestValue) {
                    selected = child;
                    bestValue = hvVal;
                }
            }

        }


        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + this.children.length);
        }

        return selected;
    }


    public double[] rollOut(KnowledgeBase kb, WeightMatrix wm)
    {
    	double[] scores = new double[4];   
        int thisDepth = this.m_depth;
        int action;
        double delta;
        StateObservation rollerState = state.copy();
        KnowledgeBase rollerKb;
        HashMap<Integer, Double> scoreMap;

        if(Agent.USE_EVO) {
	        rollerKb = new KnowledgeBase(kb);
	        scoreMap = new HashMap<Integer, Double>();
	        scoreMap.put(rollerState.getGameTick(), rollerState.getGameScore());
	        
	        while (!finishRollout(rollerState,thisDepth)) {
	        	action = getWeightedAction(wm, rollerKb.featureSet, rollerState, new ArrayList<Integer>());
	            rollerState.advance(Agent.actions[action]);
	            rollerKb.update(rollerState, false);
	            scoreMap.put(rollerState.getGameTick(), rollerState.getGameScore());
	            thisDepth++;
	        }
	        rollerKb.update(rollerState, true);
        	updateEvents(rollerState, rollerKb, scoreMap);
//        	System.out.println(state.getAvatarPosition().toString() + "   "  + rollerState.getAvatarPosition().toString());
        	
        	if(rollerKb.state == null) {
        		rollerKb.update(rollerState, true);
        	}
        	
        	scores = kb.getRolloutScore(rollerKb);
        	wm.fitness = scores[0];
        	delta = value(rollerState, wm);
        	scores[0] = delta;
        	
        	kb.eventSet = rollerKb.eventSet; // add new events to the actual knowledgebase
//        	System.out.println("new: " + wm.fitness);
        }
        else {
	        while (!finishRollout(rollerState,thisDepth)) {
	        	action = m_rnd.nextInt(Agent.NUM_ACTIONS);
	            rollerState.advance(Agent.actions[action]);
	            thisDepth++;
	        }
	        
	        delta = value(rollerState, null);
        	scores[0] = delta;
        	scores[1] = 0.0;
        	scores[2] = 0.0;
        	scores[3] = 0.0;
        }
        
        if(delta < bounds[0])
            bounds[0] = delta;

        if(delta > bounds[1])
            bounds[1] = delta;
        	
        return scores;
//        return delta;
    }
    
    private int getWeightedAction(WeightMatrix wm, Map<Integer, Feature> featureSet, StateObservation rollerState, ArrayList<Integer> forbiddenActions)
    {
    	double[] actionValues = new double[Agent.NUM_ACTIONS];
    	double gibb = 0;
    	
//    	System.out.print("forbidden: ");
//    	for(Integer p : forbiddenActions) {
//    		System.out.print(Agent.actions[p] + ", ");
//    	}
//    	System.out.println("");
 
    	for (int i=0; i<Agent.NUM_ACTIONS; i++)
    	{
//    		if(forbiddenActions.contains(i))
//    			continue;
    		
    		actionValues[i] = 0;
    		for (Feature feature : featureSet.values())
        	{
    			ACTIONS action = Agent.actions[i];
    			double weight = wm.getWeight(i, action, feature);
    			
        		if(Agent.USE_BETTEREVO) {
//        			actionValues[i] += getWeightOneMoveAction(action, weight, rollerState.getAvatarPosition(), feature) * feature.getValue();
        			
        			// feature value gets less important
        			actionValues[i] += getWeightOneMoveAction(action, weight, rollerState.getAvatarPosition(), feature) * (feature.getValue() / (state.getGameTick() / 10));
        		} else {
	        		actionValues[i] += weight * feature.getValue();
        		}
        	}
    		
//    		gibb += Math.pow(-(actionValues[i] / SingleTreeNode.epsilon), Math.E);
    		gibb += Math.pow(actionValues[i], Math.E);
    	}
    	
    	double r = m_rnd.nextDouble();
    	double acum = 0;
    	
//    	System.out.println("");
    	
    	//Gibbs sampling (using epsilon?)
    	for (int i=0; i<Agent.NUM_ACTIONS; i++)
    	{
    		if(forbiddenActions.contains(i))
    			continue;
    		
//    		acum += (Math.pow(-(actionValues[i] / SingleTreeNode.epsilon), Math.E)) / (gibb);
//    		System.out.println(Agent.actions[i] + "   " + (Math.pow(actionValues[i], Math.E)) / (gibb));
    		acum += (Math.pow(actionValues[i], Math.E)) / (gibb);
    		if (acum > r) {
    			
    		// TOO SLOW!!!!
//    			if(Agent.USE_USEFULMOVEACTION && forbiddenActions.size() < 3) { // skip useless actions
//    				Types.ACTIONS action = Agent.actions[i];
//    				if(action == Types.ACTIONS.ACTION_DOWN || action == Types.ACTIONS.ACTION_UP 
//    		        		|| action == Types.ACTIONS.ACTION_LEFT || action == Types.ACTIONS.ACTION_RIGHT) {
//    					StateObservation nextState = rollerState.copy();
//    					nextState.advance(action);
//    					
//    					if(nextState.getAvatarPosition().equals(state.getAvatarPosition())) {
//    						forbiddenActions.add(i);
//    						getWeightedAction(wm, featureSet, rollerState, forbiddenActions);
//    					}
//    				}
//    			}
    			
    			return i;
    		}
    	}
    	return Agent.NUM_ACTIONS - 1; // when r is 1.0 and acum 0.99999 due to rounding errors
    }
    
    // implementeer met pathfinding: haal de gridcell op naast de avatar die op het pad ligt
    private double getWeightOneMoveAction(ACTIONS action, double weight, Vector2d avatarPos, Feature feature)
    {
    	Vector2d nextAvatarPos = avatarPos.copy();
    	
		if(feature.dist == -1)
			return 1;
    	
    	if(action == ACTIONS.ACTION_UP)
    		nextAvatarPos.add(0, state.getBlockSize());
    	else if(action == ACTIONS.ACTION_DOWN)
    		nextAvatarPos.add(0, -state.getBlockSize());
    	else if(action == ACTIONS.ACTION_LEFT)
    		nextAvatarPos.add(-state.getBlockSize(), 0);
    	else if(action == ACTIONS.ACTION_RIGHT)
    		nextAvatarPos.add(state.getBlockSize(), 0);
    	else // use action
    		return weight;
    	
    	double moveWeight;
    	if(Agent.USE_SHORTESTPATH && feature.pathStartLocation != null)
    	{	
    		double distanceToPath = feature.pathStartLocation.dist(nextAvatarPos);
    		if(distanceToPath >= state.getBlockSize() * 2)
    			moveWeight = weight;
    		else if(distanceToPath <= 0)
    			moveWeight = 1;
    		else
    			moveWeight = weight / 2;
    		
//    		System.out.println(action + ", feature " + feature.type + ", weight " + weight + ", distanceToPath " + distanceToPath + ", moveWeight " + moveWeight);
    	}
    	else
    	{
    		// distanceChange is a value between 0 and 1. 1 when the avatar moves maximally in the right direction in 1 step,
        	// 0 when the avatar moves maximally away in 1 step.
        	double distanceChange = feature.position.dist(avatarPos) - feature.position.dist(nextAvatarPos);
        	distanceChange = Utils.normalise(distanceChange, -state.getBlockSize(), state.getBlockSize());
        	
        	moveWeight = ((1 - distanceChange) + (distanceChange * weight));
        	
//        	System.out.println(action + ", feature " + feature.type + ", weight " + weight + ", distanceChg " + distanceChange + ", moveWeight " + moveWeight);
    	}
    	
    	return moveWeight;
    }
    
    private void updateEvents(StateObservation rollerState, KnowledgeBase rollerKb, HashMap<Integer, Double> scoreMap)
    {
        Iterator<Event> itEvent = rollerState.getEventsHistory().descendingIterator();
        while(itEvent.hasNext())
        {
        	Event event = itEvent.next();
        	if(event.gameStep <= state.getGameTick())
        		break;
        	
        	if(event.passiveTypeId == 0) //wall
        		continue;
        	
        	if(scoreMap.get(event.gameStep) == null) {
        		continue;
        	}
        	
//        	System.out.println(scoreMap.get(event.gameStep) + " " + event.gameStep);
//        	System.out.println(scoreMap.get(event.gameStep - 1) + " " + (event.gameStep - 1));
        	double scoreChange = scoreMap.get(event.gameStep) - scoreMap.get(event.gameStep - 1);

        	rollerKb.updateEventSet(event, scoreChange);
//        	System.out.println("step:" + event.gameStep + " fromavt:" + event.fromAvatar + " actype:" + event.activeTypeId + " pastype:" + event.passiveTypeId + ", scrchg:" + scoreChange);
        }
    }

    public double value(StateObservation a_gameState, WeightMatrix wm) {

        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();
        double rawScore;
        
        if (wm == null) {
        	rawScore = a_gameState.getGameScore();
        } else {
        	rawScore = wm.fitness;
        }

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            rawScore += HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            rawScore += HUGE_POSITIVE;

        return rawScore;
    }

    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        if(depth >= Agent.ROLLOUT_DEPTH)      //rollout end condition.
            return true;

        if(rollerState.isGameOver())          //end of game
            return true;

        return false;
    }

    public void backUp(SingleTreeNode node, double[] result)
    {
        SingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result[0];
            
            
            //testing//
            n.knowledgeValue += result[1];
            n.distanceValue += result[2];
            n.scoreValue += result[3];
            
            n = n.parent;
        }
    }
    
    public int getAction() {
    	
    	if (Agent.USE_DECISIVE) { // if an action wins the game use it, if an action loses the game don't.
    		
    		for (int i=0; i<children.length; i++) {
    			
    			StateObservation rollerState = state.copy();
    			rollerState.advance(Agent.actions[i]);
    			boolean gameOver = rollerState.isGameOver();
    	        Types.WINNER win = rollerState.getGameWinner();
    	        
    	        if(children[i] != null && gameOver && win == Types.WINNER.PLAYER_LOSES) {
    	            children[i].nVisits = -Integer.MAX_VALUE;
    	            children[i].totValue = -Double.MAX_VALUE;
    	        }
    	        
    	        double thisScore = this.state.getGameScore();
    	        double afterActionScore = rollerState.getGameScore();
    	        
    	        if(gameOver && win == Types.WINNER.PLAYER_WINS || thisScore < afterActionScore) {
    	        	return i;
    	        }
    		}
    	}
    	
    	return mostVisitedAction();
    }

    public int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = Utils.noise(childValue, SingleTreeNode.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }
        return selected;
    }

    public int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
//                double childValue = children[i].totValue / (children[i].nVisits + SingleTreeNode.epsilon);
            	double childValue = children[i].totValue;
                childValue = Utils.noise(childValue, SingleTreeNode.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    public boolean notFullyExpanded() {
        for (SingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }
    
    // for testing only ////////////////////////
//    public JSONObject nodeToJSON() {
//    	
//    	String text = "";
//    	String act;
//    	
//    	JSONObject node = new JSONObject();
//    	JSONArray jsonChildren = new JSONArray();
//    	
//    	if (this.state != null) {
//	    	if (this.action == null) {
//	    		act = "Root tick-" + this.state.getGameTick();
//	    	} else {
//	    		act = this.action.toString();
//	    	}
//	    	text += "Action: " + act + " Value: " + totValue + " Visits: " + nVisits + ", kc: " + this.knowledgeValue + ", dc: " + this.distanceValue + ", sc" + this.scoreValue;
//    	
//	    	for (int i=0; i<this.children.length; i++) {
//				if (this.children[i] != null)
//					jsonChildren.put(this.children[i].nodeToJSON());
//			}
//    	}
//    	else {
//    		text += "NULL";
//    	}
//    	
//    	try {
//			node.put("text", text);
//			node.put("state", new JSONObject().put("opened", true));
//			node.put("children", jsonChildren);
//    	}
//    	catch (JSONException ex) {}
//    	
//    	return node;
//    }
    /////////////////////////////////////////
}
