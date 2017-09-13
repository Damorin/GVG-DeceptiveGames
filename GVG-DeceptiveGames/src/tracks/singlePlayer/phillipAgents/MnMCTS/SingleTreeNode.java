package controllers.MnMCTS;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

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
    public double predValue;
    public int nVisits;
    public static Random m_rnd;
    private int m_depth;
    private int rolloutDepth;
    private static double[] lastBounds = new double[]{0,1};
    private static double[] curBounds = new double[]{0,1};
    private static double discount = .99;
    private static int max_sprite_types = 40;
    private static LearnedModel learnedModel;
    private static int forcedExploration;
    private static Vector2d awayFrom;
    private static double sqDiaWorld;

    public SingleTreeNode(Random rnd) {
        this(null, null, rnd);
    }

    public SingleTreeNode(StateObservation state, SingleTreeNode parent, Random rnd) {
        this.state = state;
        this.parent = parent;
        SingleTreeNode.m_rnd = rnd;
        children = new SingleTreeNode[Agent.NUM_ACTIONS];
        totValue = 0.0;
        predValue = Double.NaN;
        rolloutDepth = Agent.ROLLOUT_DEPTH;
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
    }

    public void initLearnedModel() {
        SingleTreeNode.learnedModel = new LearnedModel(max_sprite_types, 0.1, 100, 0.01);
        SingleTreeNode.forcedExploration = 0;
        Dimension d = state.getWorldDimension();
        SingleTreeNode.sqDiaWorld = d.height*d.height + d.width*d.width;
    }

    public void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        lastBounds[0] = curBounds[0];
        lastBounds[1] = curBounds[1];

        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        int remainingLimit = 5;
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            SingleTreeNode selected = treePolicy();
            double delta = selected.rollOut();
            backUp(selected, delta);

            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;

            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
            //System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
        }
        //System.out.println("-- rd " + rolloutDepth + " -- it " + numIters + " -- ( " + avgTimeTaken + ")");

        //        rolloutDepth += numIters > rolloutDepth*rolloutDepth ? 1 : -1;
//        rolloutDepth += numIters > rolloutDepth ? 1 : -1;
        rolloutDepth += (numIters/2+Agent.ROLLOUT_DEPTH > rolloutDepth) ? 1 : -1;
//        rolloutDepth = numIters;
//        rolloutDepth = Agent.ROLLOUT_DEPTH;
    }

    public SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        while (!cur.state.isGameOver() && cur.m_depth < rolloutDepth)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand();

            } else {
                SingleTreeNode next = cur.uct();
                //SingleTreeNode next = cur.egreedy();
                cur = next;
            }
        }

        return cur;
    }


    public SingleTreeNode expand() {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        StateObservation nextState = state.copy();
        nextState.advance(Agent.actions[bestAction]);

        SingleTreeNode tn = new SingleTreeNode(nextState, this, SingleTreeNode.m_rnd);
        children[bestAction] = tn;
        return tn;

    }

    public SingleTreeNode uct() {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode child : this.children)
        {
            double hvVal = child.effVal();
            double childValue =  hvVal / (child.nVisits + SingleTreeNode.epsilon);

            double uctValue = childValue +
                    Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + SingleTreeNode.epsilon)) +
                    SingleTreeNode.m_rnd.nextDouble() * SingleTreeNode.epsilon;

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

    private double effVal() {
        double hvVal = totValue;
//System.out.print("hvVal       -> " + hvVal);
        if (forcedExploration > 0) {
            hvVal += Math.sqrt(state.getAvatarPosition().sqDist(awayFrom) / sqDiaWorld);
//System.out.print(" " + hvVal);
        }
        if (true)
//        else
        {
            double prVal = learnedModel.predict(computeFeats(state));
            predValue = prVal;
            hvVal += prVal;
        }
//System.out.println(" " + hvVal);
        return hvVal;
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


    public double rollOut()
    {
        StateObservation rollerState = state.copy();
        int thisDepth = this.m_depth;
        double accDiscount = 1;

        while (!finishRollout(rollerState,thisDepth)) {
            int action = m_rnd.nextInt(Agent.NUM_ACTIONS);
            rollerState.advance(Agent.actions[action]);
            accDiscount *= discount;
            thisDepth++;
        }

        double deltaScore = rollerState.getGameScore() - state.getGameScore();
        if (deltaScore != 0)
            learnedModel.learn(computeFeats(rollerState), Math.signum(deltaScore));

        double delta = value(rollerState);

        if (!rollerState.isGameOver()) {
            if(delta < curBounds[0]) curBounds[0] = delta;
            if(delta > curBounds[1]) curBounds[1] = delta;
        }

        double normDelta = Utils.normalise(delta, lastBounds[0], lastBounds[1]);

        return normDelta * accDiscount;
    }

    public double value(StateObservation a_gameState) {
        if (a_gameState.isGameOver()) {
            if (a_gameState.getGameWinner() == Types.WINNER.PLAYER_WINS)
                return HUGE_POSITIVE;
            else
                return HUGE_NEGATIVE;
        }
        return a_gameState.getGameScore();
    }

    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        if(depth >= rolloutDepth)      //rollout end condition.
            return true;
        if(rollerState.isGameOver())               //end of game
            return true;
        return false;
    }

    public void backUp(SingleTreeNode node, double result)
    {
        SingleTreeNode n = node;
        double scoreAfter = n.state.getGameScore();
        double discountedResult = result;
        boolean updated = false;
        while(n != null)
        {
            double score = n.state.getGameScore();
            double deltaScore = scoreAfter - score;
            if (deltaScore != 0 || (updated && n.parent == null)) {
                learnedModel.learn(computeFeats(n.state), Math.signum(deltaScore));
                scoreAfter = score;
                updated = true;
            }
            n.nVisits++;
            n.totValue += discountedResult;
            discountedResult *= discount;
            n = n.parent;
        }
    }


    public int mostVisitedAction() {
        int selected = -1;
        double thisValue, bestValue = -Double.MAX_VALUE;
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

                if ((thisValue = children[i].nVisits + m_rnd.nextDouble() * epsilon) > bestValue) {
                    bestValue = thisValue;
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
            rolloutDepth ++;//= Agent.ROLLOUT_DEPTH;
//System.out.println(rolloutDepth);
            if (forcedExploration == 0) {
                awayFrom = state.getAvatarPosition();;
                forcedExploration = 100;
            }
        }
        if (forcedExploration > 0) forcedExploration--;
        children[selected].rolloutDepth = rolloutDepth;
        return selected;
    }

    public int bestAction()
    {
        int selected = -1;
        double thisValue, bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null && (thisValue = children[i].totValue + m_rnd.nextDouble() * epsilon) > bestValue) {
                bestValue = thisValue;
                selected = i;
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


    public void update(StateObservation a_gameState) {
        state = a_gameState;
        nVisits = nVisits / 2;
        totValue = Utils.normalise(totValue, curBounds[0], curBounds[1]) / 2;
        m_depth--;
        for (int i=0; i<children.length; i++) {
            SingleTreeNode tn = children[i];
            if (tn != null) {
                StateObservation nextState = state.copy();
                nextState.advance(Agent.actions[i]);
                tn.update(nextState);
            }
        }
    }


    public double[] computeFeats(StateObservation a_gameState) {
        double[] feat = new double[max_sprite_types];
        for (int i=0; i<feat.length; i++)
            feat[i] = sqDiaWorld;
        Vector2d curPos = a_gameState.getAvatarPosition();
        ArrayList<Observation>[][] og = a_gameState.getObservationGrid();
        for (ArrayList<Observation>[] og_i : og)
            for (ArrayList<Observation> og_ij : og_i)
                for (Observation obs : og_ij) {
                    double sqDist = curPos.sqDist(obs.position);
                    feat[obs.itype] = sqDist < feat[obs.itype] ? sqDist : feat[obs.itype];
                }
        for (int i=0; i<feat.length; i++) {
            double f = 1. - Math.sqrt(feat[i] / sqDiaWorld);
            feat[i] = f;
        }
        feat[0] = 0;//2 * curPos.x / d.width - 1;
        feat[1] = 0;//2 * curPos.y / d.height - 1;
        return feat;
    }


    public double[] computeFeats_(StateObservation a_gameState) {
        Vector2d curPos = a_gameState.getAvatarPosition();
        Dimension d = a_gameState.getWorldDimension();
        double sqDiag = d.height*d.height + d.width*d.width;
        double[] feat = new double[max_sprite_types];
        for (int i=0; i<feat.length; i++)
            feat[i] = 0;
        ArrayList<ArrayList<Observation>[]> allObs = new ArrayList<ArrayList<Observation>[]>();
        allObs.add(a_gameState.getNPCPositions(curPos));
        allObs.add(a_gameState.getImmovablePositions(curPos));
        allObs.add(a_gameState.getMovablePositions(curPos));
        allObs.add(a_gameState.getResourcesPositions(curPos));
        allObs.add(a_gameState.getPortalsPositions(curPos));
        for (ArrayList<Observation>[] o1 : allObs)
            if (o1 != null)
                for (ArrayList<Observation> o2 : o1)
                    if (!o2.isEmpty())
                        feat[o2.get(0).itype] = 1 - Math.sqrt(o2.get(0).sqDist / sqDiag);
        return feat;
    }


    public void printTree() {
        if ( parent == null )
            System.out.println("digraph MCTS {");
        System.out.println(hashCode() + " [label=\"" + nVisits + "|" + totValue + "|" + predValue + "\"]");
        for (int i=0; i<children.length; i++) {
            SingleTreeNode tn = children[i];
            if (tn != null) {
                tn.printTree();
                System.out.println(hashCode() + " -> " + tn.hashCode() + " [label=" + i + "]");
            }
        }
        if ( parent == null )
            System.out.println("}");
    }

}
