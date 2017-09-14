package tracks.singlePlayer.phillipAgents.roskvist;

import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class SingleTreeNode {
	private static final double HUGE_NEGATIVE = -10000000.0;
	private static final double HUGE_POSITIVE = 10000000.0;
	public static double epsilon = 1e-6;
	public static double egreedyEpsilon = 0.05;

	public StateObservation state;
	public SingleTreeNode parent;
	public SingleTreeNode[] children;
	public double totValue;
	public int nVisits;
	public static Random m_rnd;
	private int m_depth;
	private static double[] lastBounds = new double[] { 0, 1 };
	private static double[] curBounds = new double[] { 0, 1 };

	/*
	 * Following variables are created for the frykvistMCTS only
	 */
	public static double mixMaxQ = 0.1d;
	// [1 ...*]
	public static int macroRepeat = 3;

	public static LinkedList<Vector2d> lastPlacesVisited;
	public Vector2d nodePosition;

	// The parent action that lead to this node
	private int parentAction;

	// The action picked in the last mcts run
	public static int lastRunAction;

	public static int standingStillCounter = 0;

	// The root of the tree
	private static SingleTreeNode root;

	// this could be used to implement a better reward nomalization by redoing all
	// scores each time the bounds have changed
	// public static ArrayList<SingleTreeNode> nodeList;

	// The max manhatten distance in the game, used for nomalizing distance.
	private static int manhattenMaxDistance;

	public SingleTreeNode(Random rnd, StateObservation state) {
		this(state, null, rnd, -1);
		root = this;
		setupMaxDistance(state);
	}

	public SingleTreeNode(StateObservation state, SingleTreeNode parent, Random rnd, int parentAction) {
		this.state = state;
		this.parent = parent;
		this.m_rnd = rnd;
		this.parentAction = parentAction;
		nodePosition = state == null ? null : state.getAvatarPosition();
		children = new SingleTreeNode[Agent.NUM_ACTIONS];
		totValue = 0.0;
		if (parent != null)
			m_depth = parent.m_depth + 1;
		else
			m_depth = 0;
	}

	public void mctsSearch(ElapsedCpuTimer elapsedTimer) {

		lastBounds[0] = curBounds[0];
		lastBounds[1] = curBounds[1];

		double avgTimeTaken = 0;
		double acumTimeTaken = 0;
		long remaining = elapsedTimer.remainingTimeMillis();
		int numIters = 0;

		int remainingLimit = 5;
		while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
			ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
			// SingleTreeNode selected = treePolicy();
			SingleTreeNode selected = treePolicyReversePenalty();
			// SingleTreeNode selected = treePolicyPartialExpansion();

			// double delta = selected.rollOut();

			// A more simple reward strategy
			double delta = selected.rollOutNewValueTest();

			backUp(selected, delta);

			numIters++;
			acumTimeTaken += (elapsedTimerIteration.elapsedMillis());

			avgTimeTaken = acumTimeTaken / numIters;
			remaining = elapsedTimer.remainingTimeMillis();
			// System.out.println(elapsedTimerIteration.elapsedMillis() +
			// " --> " + acumTimeTaken + " (" + remaining + ")");
		}
		// System.out.println("-- " + numIters + " -- ( " + avgTimeTaken + ")");
		// System.out.println("MIN: " + lastBounds[0] + " MAX:" + lastBounds[1]);
	}

	public SingleTreeNode treePolicy() {

		SingleTreeNode cur = this;

		while (!cur.state.isGameOver() && cur.m_depth < Agent.ROLLOUT_DEPTH) {
			if (cur.notFullyExpanded()) {
				// return cur.macroActionsExpand();
				return cur.expand();

			} else {
				// SingleTreeNode next = cur.partialExpansionUCT();
				// SingleTreeNode next = cur.mixMaxUCT();
				SingleTreeNode next = cur.uct();
				// SingleTreeNode next = cur.egreedy();
				cur = next;
			}
		}

		return cur;
	}

	// This version of the expansion has a penalty for doing a reverse move
	public SingleTreeNode treePolicyReversePenalty() {

		SingleTreeNode cur = this;

		while (!cur.state.isGameOver() && cur.m_depth < Agent.TREE_DEPTH) {
			if (cur.notFullyExpanded()) {
				return cur.expand();

			} else {

				SingleTreeNode next = cur.uctReversePenalty();

				cur = next;
			}
		}

		return cur;
	}

	/*
	 * public SingleTreeNode treePolicyPartialExpansion() { SingleTreeNode cur =
	 * this; while (!cur.state.isGameOver() && cur.m_depth <
	 * roskvist.Agent.ROLLOUT_DEPTH) { if (cur.notFullyExpanded()) {
	 * 
	 * SingleTreeNode next = cur.partialExpansionUCT();
	 * 
	 * //expansion if(next == null) { return cur.expand(); }
	 * 
	 * cur = next;
	 * 
	 * } else { SingleTreeNode next = cur.uct(); // SingleTreeNode next =
	 * cur.mixMaxUCT(); // SingleTreeNode next = cur.uct(); // SingleTreeNode next =
	 * cur.egreedy(); cur = next; } }
	 * 
	 * return cur;
	 * 
	 * }
	 */

	/*
	 * From the Mario paper section 5.2
	 */
	public SingleTreeNode macroActionsExpand() {

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
		SingleTreeNode tn = this;

		for (int idx = 0; idx < macroRepeat; idx++) {
			nextState.advance(Agent.actions[bestAction]);
			tn = new SingleTreeNode(nextState, tn, tn.m_rnd, bestAction);
			tn.parent.children[bestAction] = tn;
			nextState = nextState.copy();
		}
		return tn;

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

		SingleTreeNode tn = new SingleTreeNode(nextState, this, this.m_rnd, bestAction);
		children[bestAction] = tn;
		return tn;

	}

	public SingleTreeNode uct() {

		SingleTreeNode selected = null;
		double bestValue = -Double.MAX_VALUE;
		for (SingleTreeNode child : this.children) {
			double hvVal = child.totValue;
			double childValue = hvVal / (child.nVisits + this.epsilon);

			double uctValue = childValue
					+ Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon))
					+ this.m_rnd.nextDouble() * this.epsilon;

			// small sampleRandom numbers: break ties in unexpanded nodes
			if (uctValue > bestValue) {
				selected = child;
				bestValue = uctValue;
			}
		}

		if (selected == null) {
			throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
		}

		return selected;
	}

	private void setupMaxDistance(StateObservation state) {
		Dimension dim = state.getWorldDimension();

		manhattenMaxDistance = dim.height + dim.width;

	}

	public SingleTreeNode uctReversePenalty() {

		SingleTreeNode selected = null;
		double bestValue = -Double.MAX_VALUE;

		Vector2d lastPlaceVisited = lastPlacesVisited.peekLast();

		Vector2d pos;
		Vector2d rootPos = root.state.getAvatarPosition();
		double dist;

		for (SingleTreeNode child : this.children) {
			double hvVal = child.totValue;
			double childValue = hvVal / (child.nVisits + this.epsilon);
			// The MIXMAX part
			double mixMaxValue = mixMaxQ * hvVal + (1 - mixMaxQ) * childValue;

			// We assign a penalty to the child if the game position has been visited
			// recently
			// The penalty is dropped if we repeat the last action made though.
			// boolean penalty = (lastPlacesVisited.contains(child.nodePosition) &&
			// child.parentAction != lastRunAction) ? true : false;
			// boolean penalty = false;
			pos = child.nodePosition;
			double penalty = lastPlacesVisited.contains(pos) ? 0.9 : 1;

			if (child.nodePosition.equals(lastPlaceVisited) && child.parentAction == lastRunAction) {
				penalty = 1;
				// System.out.println("penalty removed");
			}

			// System.out.println("Counter: " + standingStillCounter);
			if (standingStillCounter > 2) {

				// penalty = 1;
			}

			// we also add a normalized distance to encourage the controller to move around
			// the map

			dist = Math.abs(pos.x - rootPos.x + pos.y - rootPos.y);
			dist = Utils.normalise(dist, 0, manhattenMaxDistance);

			double uctValue = mixMaxValue
					+ Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon))
					+ this.m_rnd.nextDouble() * this.epsilon;
			/*
			 * double uctValue = childValue + Agent.K Math.sqrt(Math.log(this.nVisits + 1) /
			 * (child.nVisits + this.epsilon)) + this.m_rnd.nextDouble() * this.epsilon;
			 */
			// We add the normalized distance to the uct value
			// uctValue+=dist * 0.1;

			// System.out.println(uctValue);
			// apply the penalty
			uctValue *= penalty;

			// small sampleRandom numbers: break ties in unexpanded nodes
			if (uctValue > bestValue) {
				selected = child;
				bestValue = uctValue;
			}
		}

		if (selected == null) {
			throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
		}

		return selected;
	}

	// Mixmax rewards
	// Mario mcts paper, 5.1
	/*
	 * public SingleTreeNode mixMaxUCT() {
	 * 
	 * SingleTreeNode selected = null; double bestValue = -Double.MAX_VALUE; for
	 * (SingleTreeNode child : this.children) { double hvVal = child.totValue;
	 * double childValue = hvVal / (child.nVisits + this.epsilon);
	 * 
	 * // The MIXMAX part double mixMaxValue = SingleTreeNode.mixMaxQ * hvVal + (1 -
	 * SingleTreeNode.mixMaxQ) * childValue;
	 * 
	 * // Replaced the childvalue with the mixmax value double uctValue =
	 * mixMaxValue + Agent.K Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits +
	 * this.epsilon)) + this.m_rnd.nextDouble() * this.epsilon;
	 * 
	 * // small sampleRandom numbers: break ties in unexpanded nodes if (uctValue >
	 * bestValue) { selected = child; bestValue = uctValue; } }
	 * 
	 * if (selected == null) { throw new
	 * RuntimeException("Warning! returning null: " + bestValue + " : " +
	 * this.children.length); }
	 * 
	 * return selected; }
	 */
	// Partial expansion
	// Mario mcts paper, 5.3
	// Compares expansion uct value to uct value of expanded nodes.
	// returns null if expansion is chosen, otherwise return best node based on uct
	/*
	 * public SingleTreeNode partialExpansionUCT() {
	 * 
	 * SingleTreeNode selected = null; double bestValue = -Double.MAX_VALUE; double
	 * worstVaule = Double.MAX_VALUE; boolean childLess = true; for (SingleTreeNode
	 * child : this.children) { if(child == null) { continue; } childLess = false;
	 * 
	 * double hvVal = child.totValue; double childValue = hvVal / (child.nVisits +
	 * this.epsilon);
	 * 
	 * double uctValue = childValue + Agent.K Math.sqrt(Math.log(this.nVisits + 1) /
	 * (child.nVisits + this.epsilon)) + this.m_rnd.nextDouble() * this.epsilon;
	 * 
	 * // small sampleRandom numbers: break ties in unexpanded nodes if (uctValue >
	 * bestValue) { selected = child; bestValue = uctValue; }
	 * 
	 * if(uctValue < worstVaule) { worstVaule = uctValue; } }
	 * 
	 * 
	 * double expansionUCTValue = partialExpansionUCTValue();
	 * 
	 * 
	 * //expansion vaule is higher than the worst child, and we should therefore
	 * expand. if(expansionUCTValue > worstVaule || childLess) { return null; }
	 * 
	 * 
	 * if (selected == null) { throw new
	 * RuntimeException("Warning! returning null: " + bestValue + " : " +
	 * this.children.length); }
	 * 
	 * return selected; }
	 * 
	 */
	// Partial expansion
	// Mario mcts paper, 5.3
	public double partialExpansionUCTValue() {

		double k = 0.5;

		int childCounter = 0;

		for (SingleTreeNode s : this.children) {
			if (s != null) {
				childCounter++;
			}
		}

		// modified from the mcts paper to resemble the uct formula already used in the
		// sample controller
		double uctValue = k + Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (1 + childCounter));

		return uctValue;
	}

	public SingleTreeNode egreedy() {

		SingleTreeNode selected = null;

		if (m_rnd.nextDouble() < egreedyEpsilon) {
			// Choose randomly
			int selectedIdx = m_rnd.nextInt(children.length);
			selected = this.children[selectedIdx];

		} else {
			// pick the best Q.
			double bestValue = -Double.MAX_VALUE;
			for (SingleTreeNode child : this.children) {
				double hvVal = child.totValue;

				// small sampleRandom numbers: break ties in unexpanded nodes
				if (hvVal > bestValue) {
					selected = child;
					bestValue = hvVal;
				}
			}

		}

		if (selected == null) {
			throw new RuntimeException("Warning! returning null: " + this.children.length);
		}

		return selected;
	}

	public double rollOut() {
		StateObservation rollerState = state.copy();
		int thisDepth = this.m_depth;

		while (!finishRollout(rollerState, thisDepth)) {

			int action = m_rnd.nextInt(Agent.NUM_ACTIONS);
			rollerState.advance(Agent.actions[action]);
			thisDepth++;
		}

		double delta = value(rollerState);

		if (delta < curBounds[0]) {
			curBounds[0] = delta;
		}
		if (delta > curBounds[1])
			curBounds[1] = delta;

		double normDelta = Utils.normalise(delta, lastBounds[0], lastBounds[1]);

		// System.out.println(normDelta);

		return normDelta;
	}

	public double rollOutConstantDepth() {
		StateObservation rollerState = state.copy();
		int thisDepth = 0;

		while (!finishRollout(rollerState, thisDepth)) {

			int action = m_rnd.nextInt(Agent.NUM_ACTIONS);
			rollerState.advance(Agent.actions[action]);
			thisDepth++;
		}

		double delta = value(rollerState);

		if (delta < curBounds[0])
			curBounds[0] = delta;
		if (delta > curBounds[1])
			curBounds[1] = delta;

		double normDelta = Utils.normalise(delta, lastBounds[0], lastBounds[1]);

		return normDelta;
	}

	public double rollOutNewValueTest() {
		StateObservation rollerState = state.copy();
		int thisDepth = this.m_depth;

		while (!finishRollout(rollerState, thisDepth)) {

			int action = m_rnd.nextInt(Agent.NUM_ACTIONS);
			rollerState.advance(Agent.actions[action]);
			thisDepth++;
		}

		double delta = valueModified(rollerState);

		// System.out.println(normDelta);

		return delta;
	}

	public double value(StateObservation a_gameState) {

		boolean gameOver = a_gameState.isGameOver();
		Types.WINNER win = a_gameState.getGameWinner();
		double rawScore = a_gameState.getGameScore();

		if (gameOver && win == Types.WINNER.PLAYER_LOSES)
			return HUGE_NEGATIVE;
		// return -1;

		if (gameOver && win == Types.WINNER.PLAYER_WINS)
			return HUGE_POSITIVE;

		return rawScore;
	}

	public double valueModified(StateObservation a_gameState) {

		boolean gameOver = a_gameState.isGameOver();
		Types.WINNER win = a_gameState.getGameWinner();
		double rawScore = a_gameState.getGameScore();

		if (gameOver && win == Types.WINNER.PLAYER_LOSES)
			return 0;
		// return -1;

		if (gameOver && win == Types.WINNER.PLAYER_WINS)
			return 1;

		double deltaScore = rawScore - root.state.getGameScore();

		// If no change in score, we can instead award the controller for moving around
		// the map
		Vector2d pos = a_gameState.getAvatarPosition();
		Vector2d rootPos = root.state.getAvatarPosition();
		double dist = Math.abs(pos.x - rootPos.x + pos.y - rootPos.y);

		dist = Utils.normalise(dist, 0, manhattenMaxDistance);

		if (deltaScore > 0) {
			return 1 - Math.pow(0.25, deltaScore);
		}
		if (deltaScore == 0) {
			// (1-t)*v0 + t*v1;
			// return (1-dist) * 0.26d + dist * 0.50d;
			return 0.5;
		}

		return 0.25;

	}

	public boolean finishRollout(StateObservation rollerState, int depth) {
		if (depth >= Agent.ROLLOUT_DEPTH) // rollout end condition.
			return true;

		if (rollerState.isGameOver()) // end of game
			return true;

		return false;
	}

	// //This depth is now seperated from the tree depth
	// public boolean finishRolloutConstantDepth(StateObservation state, int depth)
	// {
	// if (depth >= Agent.ROLLOUT_DEPTH) // rollout end condition.
	// return true;
	//
	// if (rollerState.isGameOver()) // end of game
	// return true;
	//
	// return false;
	// }

	public void backUp(SingleTreeNode node, double result) {
		SingleTreeNode n = node;
		while (n != null) {
			n.nVisits++;
			n.totValue += result;
			n = n.parent;
		}
	}

	public int mostVisitedAction() {
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		boolean allEqual = true;
		double first = -1;

		for (int i = 0; i < children.length; i++) {

			if (children[i] != null) {
				if (first == -1)
					first = children[i].nVisits;
				else if (first != children[i].nVisits) {
					allEqual = false;
				}

				double tieBreaker = m_rnd.nextDouble() * epsilon;
				if (children[i].nVisits + tieBreaker > bestValue) {
					bestValue = children[i].nVisits + tieBreaker;
					selected = i;
				}
			}
		}

		if (selected == -1) {
			// System.out.println("Unexpected selection!");
			selected = 0;
		} else if (allEqual) {
			// If all are equal, we opt to choose for the one with the best Q.
			selected = bestAction();
		}
		return selected;
	}

	// we give bonus reward for repeated actions
	public int mostVisitedActionWithRepeatReward(float reward, int lastAction) {
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		boolean allEqual = true;
		double first = -1;

		for (int i = 0; i < children.length; i++) {

			if (children[i] != null) {
				if (first == -1)
					first = children[i].nVisits;
				else if (first != children[i].nVisits) {
					allEqual = false;
				}

				double tieBreaker = m_rnd.nextDouble() * epsilon;

				// we multiply the score by reward if it is a repeated action
				double visitScore = (i == lastAction) ? reward * children[i].nVisits + tieBreaker
						: children[i].nVisits + tieBreaker;

				if (visitScore > bestValue) {
					bestValue = children[i].nVisits + tieBreaker;
					selected = i;
				}
			}
		}

		if (selected == -1) {
			System.out.println("Unexpected selection!");
			selected = 0;
		} else if (allEqual) {
			// If all are equal, we opt to choose for the one with the best Q.
			selected = bestAction();
		}
		return selected;
	}

	public int bestAction() {
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;

		for (int i = 0; i < children.length; i++) {

			double tieBreaker = m_rnd.nextDouble() * epsilon;
			if (children[i] != null && children[i].totValue + tieBreaker > bestValue) {
				bestValue = children[i].totValue + tieBreaker;
				selected = i;
			}
		}

		if (selected == -1) {
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
}
