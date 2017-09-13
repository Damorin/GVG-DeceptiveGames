package controllers.Rooot;

import controllers.Rooot.Observer;
import core.game.StateObservation;
import ontology.Types;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 11/02/14
 * Time: 15:44
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class WinScoreHeuristic extends StateHeuristic {

    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;

    double initialNpcCounter = 0;
    private Observer obs;

    public WinScoreHeuristic(Observer obs) {
    	this.obs = obs;
    }

    public double evaluateState(StateObservation stateObs) {
    	int X = obs.mapX;
    	int Y = obs.mapY;
    	int blockSize = obs.blockSize;
    	int D = X*Y*blockSize*blockSize;
    	//int D = X*Y;
    	
    	
        double gameScore = obs.rawScore(stateObs);
        double awayDistance = 3*obs.closestEnemy(stateObs)/D; 
        double chaseDistance = (-1)*obs.closestFriend(stateObs)/D;
        double resourceDistance = 1 - obs.closestSRC(stateObs)/D;
        double portalDistance = 1 - obs.closestPortal(stateObs)/D;
        double explore = 1 - (double)obs.visitedCount(stateObs)/(X*Y);
        
        boolean gameOver = obs.gameover(stateObs);
        Types.WINNER win = obs.winner(stateObs);

        
        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        //System.out.println("chaseDistance: " + chaseDistance);
        //System.out.println("awayDistance: " + awayDistance);
        //System.out.println("srcDistance: " + resourceDistance);
        //System.out.println("portalDistance: " + portalDistance);
        //System.out.println("explore:" + 5*explore);
        //return 5*explore + npcDistance + gameScore + 2*resourceDistance;
        
        return 5*explore + gameScore + portalDistance + 2*resourceDistance + awayDistance + chaseDistance;
        //return gameScore + resourceDistance;
        //return explore;
    }


}


