package tracks.singlePlayer.agentsForDeceptiveGames.Rooot;


import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 26/02/14
 * Time: 15:17
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {

	public Observer obs;
    private static double GAMMA = 0.90;
    private static long BREAK_MS = 10;
    private static int SIMULATION_DEPTH = 7;
    private static int POPULATION_SIZE = 50;

    private static double RECPROB = 0.1;
    private double MUT = (1.0 / SIMULATION_DEPTH);
    private int N_ACTIONS;
    
    private LinkedList<Types.ACTIONS> winSteps;
    private LinkedList<Types.ACTIONS> searchSteps;

    private ElapsedCpuTimer timer;

    private int genome[][][];
    protected Random randomGenerator;

    private int numSimulations;

    /**
     * Public constructor with state observation and time due.
     *
     * @param stateObs     state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        randomGenerator = new Random();
    	obs = new Observer(stateObs);
        N_ACTIONS = obs.actions.size();
        
        initGenome(stateObs);
        winSteps = new LinkedList<Types.ACTIONS>();
        searchSteps = new LinkedList<Types.ACTIONS>();
        obs.offLearning(stateObs);
    }


    double microbial_tournament(int[][] actionGenome, StateObservation stateObs, StateHeuristic heuristic) throws TimeoutException {
        /*int a, b, c, W, L;
        int i;


        a = (int) ((POPULATION_SIZE - 1) * randomGenerator.nextDouble());
        do {
            b = (int) ((POPULATION_SIZE - 1) * randomGenerator.nextDouble());
        } while (a == b);

        double score_a = simulate(stateObs, heuristic, actionGenome[a]);
        double score_b = simulate(stateObs, heuristic, actionGenome[b]);

        if (score_a > score_b) {
            W = a;
            L = b;
        } else {
            W = b;
            L = a;
        }

        int LEN = actionGenome[0].length;

        for (i = 0; i < LEN; i++) {
            if (randomGenerator.nextDouble() < RECPROB) {
                actionGenome[L][i] = actionGenome[W][i];
            }
        }


        for (i = 0; i < LEN; i++) {
            if (randomGenerator.nextDouble() < MUT) actionGenome[L][i] = randomGenerator.nextInt(N_ACTIONS);
        }

        return Math.max(score_a, score_b);
		*/
    	
    	int a, b;
    	a = randomGenerator.nextInt(POPULATION_SIZE);
    	do {
    		b = randomGenerator.nextInt(POPULATION_SIZE);
    	}while(a == b);
    	
    	double score_a = simulate(stateObs, heuristic, actionGenome[a]);
    	double score_b = simulate(stateObs, heuristic, actionGenome[b]);
    	/*int W, L;
    	W = (score_a > score_b)?a:b;
    	L = (score_a > score_b)?b:a;
    	for(int i = 0; i < SIMULATION_DEPTH; i++){
    		if(randomGenerator.nextDouble() < RECPROB){
    			actionGenome[L][i] = actionGenome[W][i];
    		}
    	}
    	for(int i = 0; i < SIMULATION_DEPTH; i++){
    		if(randomGenerator.nextDouble() < MUT){
    			actionGenome[W][i] = randomGenerator.nextInt(N_ACTIONS);
    		}
    	}*/
    	
    	return Math.max(score_a, score_b);
    	
    	
    	//int rnd = randomGenerator.nextInt(POPULATION_SIZE);
    	//double score = simulate(stateObs, heuristic, actionGenome[rnd]);
    	//return score;
    }

    private void initGenome(StateObservation stateObs) {
    	obs.actions = stateObs.getAvailableActions();
    	N_ACTIONS = obs.actions.size();
        genome = new int[N_ACTIONS][POPULATION_SIZE][SIMULATION_DEPTH];
        
        // Randomize initial genome
        for (int i = 0; i < genome.length; i++) {
            for (int j = 0; j < genome[i].length; j++) {
                for (int k = 0; k < genome[i][j].length; k++) {
                    genome[i][j][k] = randomGenerator.nextInt(N_ACTIONS);
                }
            }
        }
           
    }


    private double simulate(StateObservation stateObs, StateHeuristic heuristic, int[] policy) throws TimeoutException {


        //System.out.println("depth" + depth);
        long remaining = timer.remainingTimeMillis();
        if (remaining < BREAK_MS) {
            //System.out.println(remaining);
            throw new TimeoutException("Timeout");
        }


        int depth = 0;
        stateObs = stateObs.copy();
        for (; depth < policy.length; depth++) {
            Types.ACTIONS action = obs.action_mapping.get(policy[depth]);
            StateObservation stCopy = stateObs.copy();
            stateObs.advance(action);
            winSteps.offer(action);
            obs.onLearning(stCopy, stateObs);
            if (stateObs.isGameOver()) {
            	//obs.onLearning(stCopy, stateObs);
                break;
            }
           
        }
        if(stateObs.getGameWinner() != Types.WINNER.PLAYER_WINS){
        	winSteps.clear();
        }

        numSimulations++;
        double score = Math.pow(GAMMA, depth) * heuristic.evaluateState(stateObs);
        //double score = heuristic.evaluateState(stateObs);
        return score;
    }
    
    public double selection(int[][] actionGenes, StateHeuristic heuristic, StateObservation stateObs){
    	int a = randomGenerator.nextInt(N_ACTIONS), b;
    	do {
    		b = randomGenerator.nextInt(N_ACTIONS);
    	}while(a == b);
    	double score_a = 0;
    	double score_b = 0;
    	try {
    		score_a = simulate(stateObs, heuristic, actionGenes[a]);
    		score_b = simulate(stateObs, heuristic, actionGenes[b]);
		} catch (TimeoutException e) {
			
		}
    	int W, L;
    	W = (score_a > score_b)?a:b;
    	L = (score_a > score_b)?b:a;
    	for(int i = 0; i < SIMULATION_DEPTH; i++){
    		if(randomGenerator.nextDouble() < RECPROB){
    			actionGenes[L][i] = actionGenes[W][i];
    		}
    	}
    	for(int i = 0; i < SIMULATION_DEPTH; i++){
    		if(randomGenerator.nextDouble() < MUT){
    			actionGenes[W][i] = randomGenerator.nextInt(N_ACTIONS);
    		}
    	}
    	
    	return Math.max(score_a, score_b);
    }

    public Types.ACTIONS Search(StateObservation stateObs, StateHeuristic heuristic, int iteration){
    	
    	double[] maxScore = new double[N_ACTIONS];
    	for(int i = 0; i < N_ACTIONS; i++){
    		maxScore[i] = Double.NEGATIVE_INFINITY;
    	}
    	
    	out:
    	for(int i = 0; i < iteration; i++){
    		for(Types.ACTIONS action : obs.actions){
    			StateObservation stCopy = stateObs.copy();
                stCopy.advance(action);
                
                if(stCopy.getGameScore() > stateObs.getGameScore() && stCopy.getGameWinner() != Types.WINNER.PLAYER_LOSES && obs.numNPCs(stCopy) == obs.numNPCs(stateObs)){
                	return action;
                }
                
    			double score = 0;
    			int rnd = randomGenerator.nextInt(N_ACTIONS);
    			int int_act = obs.r_action_mapping.get(action);
    			//score = selection(genome[int_act], heuristic, stCopy);
    			try {
    				score = simulate(stCopy, heuristic, genome[int_act][rnd]);
    			}catch(TimeoutException e){
    				break out; 
    			}
    			if(score > maxScore[int_act]){
    				maxScore[int_act] = score;
    			}
    			
    			for(int j = 0; j < SIMULATION_DEPTH; j++){
                	if(randomGenerator.nextDouble() > MUT) genome[int_act][rnd][j] = randomGenerator.nextInt(N_ACTIONS);
                }
    		}
    	}
    	
    	return obs.action_mapping.get(Utils.argmax(maxScore));
//    		int rndPos = randomGenerator.nextInt(N_ACTIONS);
//    		int rndAct = randomGenerator.nextInt(N_ACTIONS);
//    		int backup = steps[rndPos];
//    		steps[rndPos] = rndAct;
//    		double score = 0;
//    		try {
//				score = simulate(stateObs, heuristic, steps);
//			} catch (TimeoutException e) {
//				break;
//			}
//    		if(score > maxScore){
//    			maxScore = score;
//    		}
//    		else{
//    			steps[rndPos] = backup;
//    		}
//    	}
//    	
//    	return maxScore;
    }
    
    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     *
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {    	
    	//System.out.println("npc size: " + obs.npcTypes.size());
    	//System.out.println("harmful size: " + obs.harmfulNPCs.size());
    	//ArrayList<Observation>[] npcs = stateObs.getNPCPositions();
    	//if(npcs != null) System.out.println("NPC size : " + npcs.length);
    	//if(obs.hasResource) System.out.println("has src!");
    	
        this.timer = elapsedTimer;
        numSimulations = 0;
        obs.map = stateObs.getObservationGrid();
        
        

        //if(winSteps.size() > 0 && !obs.hasNPC){
        if(winSteps.size() > 0){
        	System.out.print("winner step: ");
        	Types.ACTIONS action = winSteps.poll();
        	System.out.println(action);
        	return action;
        }
        
//        if(searchSteps.size() > 0){
//        	System.out.print("searching for step: ");
//        	Types.ACTIONS action = searchSteps.poll();
//        	System.out.println(action);
//        	return action;
//        }
        
        initGenome(stateObs);
        boolean useEA = true;
        if(useEA){
        	Types.ACTIONS act = Search(stateObs, new WinScoreHeuristic(obs), 1000);
            obs.avatarVisited(stateObs, act);
        	//System.out.println(act);
        	return act;
        }
        
        
        Types.ACTIONS lastGoodAction = null;
        double maxScores = Double.NEGATIVE_INFINITY;
        
        WinScoreHeuristic heuristic = new WinScoreHeuristic(obs);
        for(Types.ACTIONS action : obs.actions){
        	
        	StateObservation stCopy = stateObs.copy();
            stCopy.advance(action);
            if(stCopy.getGameScore() > stateObs.getGameScore() && stCopy.getGameWinner() != Types.WINNER.PLAYER_LOSES && obs.numNPCs(stCopy) == obs.numNPCs(stateObs)){
            	return action;
            }
            
            
            int int_act = obs.r_action_mapping.get(action);
            
            double score = 0;
            int rnd = randomGenerator.nextInt(POPULATION_SIZE);
            //int rnd = randomGenerator.nextInt(N_ACTIONS*N_ACTIONS*N_ACTIONS);
            try{
            	//simulate(stateObs, heuristic, genome[randomGenerator.nextInt(N_ACTIONS)][randomGenerator.nextInt(POPULATION_SIZE)]);
            	score = simulate(stCopy, heuristic, genome[int_act][rnd]);
            	//score = simulate(stCopy, heuristic, acts[rnd]) + randomGenerator.nextDouble()*0.00001;
            }catch(TimeoutException e){
            	break;
            }
            
            if(score > maxScores){
            	searchSteps.clear();
            	for(int i = 0; i < SIMULATION_DEPTH*0.3; i++){
            		searchSteps.offer(obs.action_mapping.get(genome[int_act][rnd][i]));
            	}
            	maxScores = score;
            	lastGoodAction = action;
            }
            
            //for(int i = 0; i < SIMULATION_DEPTH; i++){
            //	if(randomGenerator.nextDouble() > MUT) genome[int_act][rnd][i] = randomGenerator.nextInt(N_ACTIONS);
            //}
        }
        //System.out.println("closest enemy: " + obs.closestEnemy(stateObs)/(obs.mapX*obs.mapY));
     
        
        //lastGoodAction = this.obs.action_mapping.get(Utils.argmax(maxScores));
        
        obs.avatarVisited(stateObs, lastGoodAction);
        //obs.printVisitedRecord();
        //System.out.println("srcDistance: " + (1 - obs.closestSRC(stateObs)));
        //System.out.println("npcDistance: " + 3*(1 - obs.closestNPC(stateObs)));
        //System.out.println("explore: " + 5*(1 - obs.visitedCount(stateObs)));
        //long remaining = timer.remainingTimeMillis();
        //System.out.println(remaining);
        
        StateObservation stCopy = stateObs.copy();
        while(timer.remainingTimeMillis() > 10){
        	StateObservation backup = stCopy.copy();
        	stCopy.advance(obs.action_mapping.get(randomGenerator.nextInt(N_ACTIONS)));
        	obs.onLearning(backup, stCopy);
        }
        
        return lastGoodAction;
    }


    @Override
    public void draw(Graphics2D g)
    {
        g.drawString("Num Simulations: " + numSimulations, 10, 20);
    	int half_block = (int) (obs.blockSize*0.5);
        for(int j = 0; j < obs.map[0].length; ++j)
        {
            for(int i = 0; i < obs.map.length; ++i)
            {
                if(obs.map[i][j].size() > 0)
                {
                    Observation firstObs = obs.map[i][j].get(0); //grid[i][j].size()-1
                    //Three interesting options:
                    //int print = firstObs.category; //firstObs.itype; //firstObs.obsID;
                    int print = firstObs.itype;
                    g.drawString(print + "", i*obs.blockSize+half_block,j*obs.blockSize+half_block);
                }
                else{
                	g.drawString("", i*obs.blockSize+half_block,j*obs.blockSize+half_block);
                }
            }
        }
    }
}
