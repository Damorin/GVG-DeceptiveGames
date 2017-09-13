package controllers.AIJim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import core.game.Observation;

import ontology.Types;

public class WeightMatrix implements Comparable<WeightMatrix> 
{
	
	public ArrayList<HashMap<Integer, Double>> weightMatrix; //actions x features
	public double fitness;
	private Random rand;
	public int moveIndex = -1;
	
	public WeightMatrix(Random random)
	{
		this.rand = random;
		this.fitness = -1.0;
		this.weightMatrix = new ArrayList<HashMap<Integer, Double>>();
		if(Agent.USE_BETTEREVO) {
			boolean moveActionFound = false;
			for(int i=0; i<Agent.NUM_ACTIONS; i++)
	        {
				if(Agent.actions[i] == Types.ACTIONS.ACTION_USE)
					this.weightMatrix.add(new HashMap<Integer, Double>());
				else if(!moveActionFound && Agent.isMoveAction(Agent.actions[i])) {
					moveActionFound = true;
					moveIndex = i;
					this.weightMatrix.add(new HashMap<Integer, Double>());
				}
	        }
		} else {
			for(int i=0; i<Agent.NUM_ACTIONS; i++)
	        {
	            this.weightMatrix.add(new HashMap<Integer, Double>());
	        }
		}
	}
	
	public double getWeight(int index, Types.ACTIONS action, Feature feature) {
		double weight;
		if(moveIndex >= 0 && Agent.isMoveAction(action)) {
			try {
				weight = weightMatrix.get(moveIndex).get(feature.type);
			} catch(Exception e) {
				weight = 1;
			}
		} else {
			try {
				weight = weightMatrix.get(index).get(feature.type);
			} catch(Exception e) {
				weight = 1;
			}
		}
		
		return weight;
	}
	
	// fits this weightmatrix to the size of the knowledgebase
	// just makes it larger if it's too small, features that are no longer present in the
	// game will still be kept in the weightmatrix, although will no longer change
	public void fitToSize(KnowledgeBase kb)
	{
//		int nrOfFeatures = kb.featureSet.size();
//		if(weightMatrix.get(0).size() < nrOfFeatures)
//		{
//			for(ArrayList<Double> featureWeights : weightMatrix)
//	        {
//				for(int j=featureWeights.size(); j<nrOfFeatures; j++)
//					featureWeights.add(1.0);
//	        }
//		}
	}

	// value for features that are no longer in the featureSet remain the same in the matrix
	public WeightMatrix copyAndMutate(KnowledgeBase kb, double stdDev, double pMutation, double min, double max, double meanFactor)
	{
		WeightMatrix newMatrix = new WeightMatrix(rand);
		
		for(int i=0; i<this.weightMatrix.size(); i++)
        {
			HashMap<Integer, Double> newWeights = new HashMap<Integer, Double>(this.weightMatrix.get(i));
			
			for (Feature feature : kb.featureSet.values()) 
			{
				int type = feature.type;
				if (newWeights.get(type) == null) // values for this feature type aren't initialized yet
				{
					newWeights.put(type, 1.0);
				}
				else if (rand.nextDouble() < pMutation) // feature type is present in the game currently and we do mutation
				{
					double mutated, v, newWeight;
					double mean = 0;
					KnowledgeEvent ke;
					if(Agent.isMoveAction(Agent.actions[i]))
						ke = kb.eventSet.get(type);
					else
						ke = kb.eventSet.get(type + 100);
					
					// at least more than 100 occurances to approximate the real scoreChange better
					// mean changes the mean of the gaussian in the desired direction
					if(Agent.USE_BIASMUTATION && Agent.USE_BETTEREVO && ke != null && ke.occurances > 100)
					{
						double scoreChange = ke.getAverageScoreChange();
						if (scoreChange > 0.2)
							mean = rand.nextDouble() * meanFactor;
						else if (scoreChange < -0.2)
							mean = -rand.nextDouble() * meanFactor;
					}

					v = rand.nextGaussian() * stdDev + mean;
//					System.out.println(v + "  " + stdDev + "   " + mean);
					
					if(Agent.USE_BETTEREVO) { // this should work better with the way the weights are used now
						newWeight = newWeights.get(type) * (1 + v);
					} else {
						newWeight = newWeights.get(type) + v;
					}
					
					mutated = Math.max(min, newWeight);
					mutated = Math.min(max, mutated);
					newWeights.put(type, mutated);
				}
			}
			
			newMatrix.weightMatrix.set(i, newWeights);
        }
		
		return newMatrix;
	}
	
	// for sorting matrices according to their fitness value: descending
	@Override
    public int compareTo(WeightMatrix owm) {
        if(fitness < owm.fitness)        return 1;
        else if(fitness > owm.fitness)   return -1;
        return 0;
    }
	
	public void print(KnowledgeBase kb)
	{
		for(int i=-1; i<weightMatrix.size(); i++)
		{
			if(i == -1)
			{
				System.out.print("               ");
				for(Feature feature : kb.featureSet.values())
				{
					System.out.print("type" + feature.type + " ");
				}
				System.out.println("");
				continue;
			}
			Types.ACTIONS action = Agent.actions[i];
			String actionString;
			if(Agent.USE_BETTEREVO) {
				if (Agent.isMoveAction(action))
					actionString = "ACTION_MOVE ";
				else 
					actionString = action + " ";
			} else {
				actionString = action + " ";
			}
				
			int length = actionString.length();
			for(int k=0; k<15-length; k++)
				actionString = actionString.concat(" ");

			System.out.print(actionString);
			for (double featureVal : weightMatrix.get(i).values())
			{
				String value;
				value = " " + featureVal + "  ";
				value = value.substring(0, 5) + " ";
				
				System.out.print(value);
			}
			System.out.println("");
		}
	}

}
