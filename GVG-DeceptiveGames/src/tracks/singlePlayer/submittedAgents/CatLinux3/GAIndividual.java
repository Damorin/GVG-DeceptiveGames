package tracks.singlePlayer.submittedAgents.CatLinux3;

import core.game.StateObservation;
import ontology.Types;

import java.util.Random;

/**
 * Created by jliu on 23/05/16.
 */
public class GAIndividual {
    public int genome[];
    private double fitness;

    public GAIndividual(int genome_length, int NUM_ACTIONS, Random rdm_generator) {
        this.genome = new int[genome_length];
        for(int i=0; i<genome.length;i++) {
            genome[i] = rdm_generator.nextInt(NUM_ACTIONS);
        }
        this.fitness = Double.NEGATIVE_INFINITY;
    }

    public GAIndividual(int[] _genome) {
        this.genome = new int[_genome.length];
        for(int i=0; i<genome.length;i++) {
            genome[i] = _genome[i];
        }
        this.fitness = Double.NEGATIVE_INFINITY;
    }

    public void evaluate(StateObservation stateObs) {
        for (int i=0; i<genome.length; i++) {
            Types.ACTIONS action = Agent.legal_actions.get(this.genome[i]);
            stateObs.advance(action);
            if (stateObs.isGameOver()) {
                break;
            }
        }
        this.fitness = Agent.evaluateState(stateObs);
    }

    public double getFitness() {
        return this.fitness;
    }
}
