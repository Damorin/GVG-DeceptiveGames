package tracks.singlePlayer.submittedAgents.ToVo1;

import core.game.StateObservation;
import core.game.StateObservationMulti;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;
//import tools.Metrics;    // comment this out when submitting to GVGAI!

import java.util.ArrayList;
import java.util.Random;

public class SingleTreeNode {

	// --- global configuration ---//
	public static final int MCTS_ITERATIONS = 100;
	public static final double UCB_Cp = Math.sqrt(2);
	public static final double par_bestAction_visitsTolerance = 0.0; // the relative tolerance [0-1] where nodes are
																		// still considered as equal in visits, when
																		// choosing the most visited action after search
																		// ends (0 - no tolerance)

	public static final int par_numMemorizedNodes = 1; // with how many nodes to expand the tree in each iteration (-1 =
														// all)
	public static final boolean par_onlyFinalScoring = true; // perform scoring only at episode end
	public static final double par_Qinit = 0.0;
	public static final double par_Qasum = 0.0;
	public static final double par_lambda = 0.6;
	public static final double par_gamma = 1.0;
	public static final double par_forgettingRate_search = 0; // 1.0 - all knowledge from the previous search will be
																// forgotten, 0.0 - all knowledge will be retained
	public static final double par_playoutNonUniform = 0; // from 0.0 to 1.0, value of 0.0 equals to completely uniform
															// action selection in playout

	public static final int par_UCBboundMetric = 0;
	// 0 - local children Qvalues
	// 1 - global non-discounted rewards (as in original sampleMCTS implementation)

	public static final double par_rollout_depth_start = 10;
	public static final double par_rollout_depth_max = 10;
	public static final int par_rollout_depth_increase_interval = 5; // how often to increase the rollout depth (in
																		// episodes)
	public static final double par_rollout_depth_increase_rate = par_rollout_depth_increase_interval; // how much to
																										// increase
																										// rollout depth
																										// after each
																										// batch of
																										// episodes

	private static final double HUGE_NEGATIVE = -10000000.0;
	private static final double HUGE_POSITIVE = 10000000.0;
	public static final double epsilon = 1e-6;

	public static final int remainingTimeLimit = 5; // in miliseconds

	// --- global variables ---//

	public static StateObservation rootState;
	public static SingleTreeNode rootNode = null;
	public static SingleTreeNode nextRootNode = null;
	public static int m_depth_offset = 0;
	public static int depthFromRoot = 0;
	public static int numAddedNodes = 0;

	public static double rollout_depth_current;
	public static int numAdvances;

	public static int currentSearchID = 0;
	public static double lastRealReward = 0.0;
	public static double[] globalBounds = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };
	public static double[] minMaxScore = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };

	public static EpisodeEntry[] episode = new EpisodeEntry[(int) par_rollout_depth_max + 1];

	// --- global statistics ---//
	public static long totalAdvances;
	public static long totalEpisodes;

	// --- atributes of a single node ---//

	public SingleTreeNode parent;
	public SingleTreeNode[] children;
	public double nVisits;
	public Random m_rnd;
	public int m_depth;
	public int childIdx;

	public double stateScore;
	public double Qval;
	public double normBoundMin = Double.MAX_VALUE;
	public double normBoundMax = -Double.MAX_VALUE;
	public int lastEvaluationSearch = -1;

	public int numSuccessorNodes;

	// --- constructors ---//

	public SingleTreeNode(Random rnd) {
		this(null, -1, rnd);
	}

	public SingleTreeNode(SingleTreeNode parent, int childIdx, Random rnd) {
		this.parent = parent;
		this.m_rnd = rnd;
		// totValue = 0.0;
		this.childIdx = childIdx;
		if (parent != null)
			this.m_depth = parent.m_depth + 1;
		else
			this.m_depth = 0;

		this.Qval = par_Qinit;
		this.lastEvaluationSearch = currentSearchID;
		this.nVisits = 0;

		this.children = new SingleTreeNode[Agent.NUM_ACTIONS];

		this.numSuccessorNodes = -1;
	}

	public static void InitStatics() {
		rootNode = null;
		nextRootNode = null;
		m_depth_offset = 0;
		depthFromRoot = 0;
		numAddedNodes = 0;

		currentSearchID = 0;
		lastRealReward = 0.0;
		globalBounds[0] = Double.MAX_VALUE;
		globalBounds[1] = -Double.MAX_VALUE;
		minMaxScore[0] = Double.MAX_VALUE;
		minMaxScore[1] = -Double.MAX_VALUE;

		totalAdvances = 0;
		totalEpisodes = 0;
	}

	// --- classes ---//

	public static class EpisodeEntry {
		double currentScore;
		SingleTreeNode node;

		EpisodeEntry() {
			this.currentScore = 0.0;
			this.node = null;
		}

		EpisodeEntry Add(double s, SingleTreeNode n) {
			this.currentScore = s;
			this.node = n;
			return this;
		}
	}

	// --- methods ---//

	public void mctsSearch(ElapsedCpuTimer elapsedTimer) {

		double avgTimeTaken = 0;
		double acumTimeTaken = 0;
		long remaining = elapsedTimer.remainingTimeMillis();
		int numIters = 0;

		numAdvances = 0;
		rollout_depth_current = par_rollout_depth_start;

		ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

		while (remaining > 2 * avgTimeTaken && remaining > remainingTimeLimit) {
			// while(numIters < MCTS_ITERATIONS){

			StateObservation state = rootState.copy();

			state = GenerateEpisode(state);
			BackUp(state);

			numIters++;
			acumTimeTaken = (elapsedTimerIteration.elapsedMillis());
			// System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " +
			// acumTimeTaken + " (" + remaining + ")");
			avgTimeTaken = acumTimeTaken / numIters;
			remaining = elapsedTimer.remainingTimeMillis();

			// increase the simulation rollout length
			if ((numIters % par_rollout_depth_increase_interval) == 0) {
				rollout_depth_current += par_rollout_depth_increase_rate;
				if (rollout_depth_current > par_rollout_depth_max)
					rollout_depth_current = par_rollout_depth_max;
			}
		}

		// required for forgetting
		currentSearchID++;

		// update global statistics
		totalAdvances += numAdvances;
		totalEpisodes += numIters;
	}

	public StateObservation GenerateEpisode(StateObservation initState) {

		// init
		depthFromRoot = 0; // reset "episode" index
		numAddedNodes = 0; // count of nodes added in this episode
		SingleTreeNode node = this;
		StateObservation state = initState;
		boolean isPlayout = false;

		episode[0].Add(SingleTreeNode.value(initState), rootNode);

		// prepare non-uniform random playout policy
		double[] playoutPolicy = new double[Agent.NUM_ACTIONS];
		double sumProb = 0.0;
		for (int a = 0; a < Agent.NUM_ACTIONS; a++) {
			playoutPolicy[a] = (this.m_rnd.nextDouble() * par_playoutNonUniform + (1.0 - par_playoutNonUniform));
			sumProb += playoutPolicy[a];
		}
		playoutPolicy[0] = playoutPolicy[0] / sumProb;
		for (int a = 1; a < Agent.NUM_ACTIONS; a++) {
			playoutPolicy[a] = playoutPolicy[a - 1] + playoutPolicy[a] / sumProb;
		}

		// simulate an episode
		SingleTreeNode nextNode;
		while (!state.isGameOver() && (depthFromRoot < (int) rollout_depth_current)) {

			// locals
			nextNode = null;

			// control policy
			int ownAction = 0;
			if (!isPlayout) { // tree policy

				// UCB1
				if (node.notFullyExpanded()) { // Select random unvisited children
					double bestValue = -Double.MAX_VALUE;
					for (int i = 0; i < node.children.length; i++) {
						if ((node.children[i] == null) || (node.children[i].nVisits < epsilon)) {
							double x = m_rnd.nextDouble();
							if (x > bestValue) {
								ownAction = i;
								bestValue = x;
								nextNode = node.children[i];
							}
						}
					}
					isPlayout = true;
				} else {
					double bestValue = -Double.MAX_VALUE;
					for (SingleTreeNode child : node.children) {

						double childValue = child.Qval;

						// forgetting: decay the values in the tree
						if (child.lastEvaluationSearch < currentSearchID) {
							int power = currentSearchID - child.lastEvaluationSearch;
							child.nVisits = child.nVisits * Math.pow(1.0 - par_forgettingRate_search, power);
							child.lastEvaluationSearch = currentSearchID;
						}

						// value normalization
						double boundMin = node.normBoundMin;
						double boundMax = node.normBoundMax;
						if (boundMin >= boundMax) {
							boundMin = globalBounds[0];
							boundMax = globalBounds[1];
						}
						childValue = Utils.normalise(childValue, boundMin, boundMax);

						// the UCB1 equation
						double uctValue = childValue
								+ UCB_Cp * Math.sqrt(Math.log(node.nVisits + 1) / (child.nVisits + epsilon));

						// break ties randomly
						uctValue = Utils.noise(uctValue, epsilon, m_rnd.nextDouble());
						if (uctValue > bestValue) {
							nextNode = child;
							bestValue = uctValue;
							ownAction = child.childIdx;
						}
					}
					if (nextNode == null) {
						throw new RuntimeException("Warning! returning null: " + bestValue + " : "
								+ node.children.length + " " + +globalBounds[0] + " " + globalBounds[1]);
					}
				}

			} else { // playout policy

				ownAction = Agent.NUM_ACTIONS - 1;
				double roll = m_rnd.nextDouble();
				for (int a = 0; a < Agent.NUM_ACTIONS - 1; a++) {
					if (roll < playoutPolicy[a]) {
						ownAction = a;
						break;
					}
				}

			}

			// simulate transition
			state.advance(Agent.actions[ownAction]);
			double stateScore = SingleTreeNode.value(state);

			// expand tree (add node)
			if (nextNode == null) {
				if ((par_numMemorizedNodes < 0) || (numAddedNodes < par_numMemorizedNodes)) {
					numAddedNodes++;
					nextNode = new SingleTreeNode(node, ownAction, this.m_rnd);
					nextNode.stateScore = stateScore;
					if (node != null)
						node.children[ownAction] = nextNode;
				}
			}

			// update locals
			node = nextNode;

			// update globals
			depthFromRoot++;
			episode[depthFromRoot].Add(stateScore, node);
			numAdvances++;

		}

		return state;
	}

	public void BackUp(StateObservation state) {
		// prepare locals
		int countNewlyAddedNodes = 0;
		double TDsum = 0;
		double Qnext, Qcurr, reward;
		if (state.isGameOver())
			Qnext = 0;
		else
			Qnext = par_Qasum;

		// analyse the last episode, update all estimates except root
		for (int j = depthFromRoot; j >= 0; j--) {

			// global normalziation bounds
			if (episode[j].currentScore < minMaxScore[0])
				minMaxScore[0] = episode[j].currentScore;
			if (episode[j].currentScore > minMaxScore[1])
				minMaxScore[1] = episode[j].currentScore;
			if (par_onlyFinalScoring == false) {
				globalBounds[0] = minMaxScore[0] - minMaxScore[1];
				globalBounds[1] = -globalBounds[0];
			} else {
				globalBounds[0] = minMaxScore[0];
				globalBounds[1] = minMaxScore[1];
			}

			// node update
			SingleTreeNode node = episode[j].node;

			if (par_onlyFinalScoring == false) {
				if (j > 0)
					reward = episode[j].currentScore - episode[j - 1].currentScore;
				else
					reward = lastRealReward; // to update the root node
			} else {
				if (j == depthFromRoot)
					reward = episode[j].currentScore;
				else
					reward = 0;
			}

			if (node != null)
				Qcurr = node.Qval;
			else
				Qcurr = par_Qasum;

			double TDerror = reward + par_gamma * Qnext - Qcurr;
			TDsum = par_gamma * par_lambda * TDsum + TDerror;

			if (node != null) {
				// Q-value update
				node.nVisits += 1.0;
				double alpha = 1.0 / node.nVisits;
				// if(par_onlyFinalScoring == false)
				node.Qval = node.Qval + alpha * TDsum;
				// else
				// node.Qval = node.Qval + alpha*(episode[depthFromRoot].currentScore);

				// node-local
				if (node.numSuccessorNodes == -1) // node that was added in last episode
					countNewlyAddedNodes++;
				node.numSuccessorNodes += countNewlyAddedNodes;

				// local normalization bounds
				if (node.parent != null) {
					if (par_UCBboundMetric == 1) { // global bounds
						node.parent.normBoundMin = globalBounds[0];
						node.parent.normBoundMax = globalBounds[1];
					} else { // local bounds
						double boundMetric = node.Qval;
						if (boundMetric < node.parent.normBoundMin)
							node.parent.normBoundMin = boundMetric;
						if (boundMetric > node.parent.normBoundMax)
							node.parent.normBoundMax = boundMetric;
					}
				}
			}

			Qnext = Qcurr;
		}

	}

	public int mostVisitedActionEnhanced() {

		double highestVisits = -Double.MAX_VALUE; // main criteria

		// find highest visit count and define the lower bound considering the specified
		// threshold
		for (int i = 0; i < this.children.length; i++) {
			if (this.children[i] != null) {

				// decay the values in the tree (safety check, although these nodes will very
				// likely be up to date, unless the algorithm computed very little episodes)
				if (this.children[i].lastEvaluationSearch < (currentSearchID - 1)) {
					int power = (currentSearchID - 1) - this.children[i].lastEvaluationSearch;
					this.children[i].nVisits = this.children[i].nVisits
							* Math.pow(1.0 - par_forgettingRate_search, power);
					this.children[i].lastEvaluationSearch = (currentSearchID - 1);
				}

				if (this.children[i].nVisits > highestVisits)
					highestVisits = this.children[i].nVisits;
			}
		}
		double visitsLowerBound = highestVisits * (1 - par_bestAction_visitsTolerance) - epsilon;

		// find node with highest value in the given visits bounds
		int selected = -1;
		double bestValue = -Double.MAX_VALUE; // tie-breaker
		for (int i = 0; i < this.children.length; i++) {
			if (this.children[i] != null) {
				if (this.children[i].nVisits >= visitsLowerBound) {
					double childValue = this.children[i].Qval;
					childValue = Utils.noise(childValue, epsilon * epsilon, m_rnd.nextDouble()); // break ties randomly
					if (childValue >= bestValue) {
						bestValue = childValue;
						selected = i;
					}
				}
			}
		}

		// safety check
		if (selected == -1) {
			System.out.println("Unexpected selection!");
			selected = 0;
		}

		// remember next root node (for next search)
		nextRootNode = this.children[selected];
		return selected;
	}

	static public double value(StateObservation a_gameState) {

		boolean gameOver = a_gameState.isGameOver();
		Types.WINNER win = a_gameState.getGameWinner();
		double rawScore = a_gameState.getGameScore();

		if (gameOver && win == Types.WINNER.PLAYER_LOSES)
			rawScore += (HUGE_NEGATIVE);

		if (gameOver && win == Types.WINNER.PLAYER_WINS)
			rawScore += (HUGE_POSITIVE);

		return rawScore;
	}

	public boolean notFullyExpanded() {
		for (SingleTreeNode tn : this.children) {
			if (tn == null) {
				return true;
			} else if (tn.nVisits < epsilon) {
				return true;
			}
		}

		return false;
	}
}
