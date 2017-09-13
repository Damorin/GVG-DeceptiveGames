package controllers.AIJim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Random;

import ontology.Types;

public class Evo {
	
	private KnowledgeBase kb;
	
	private Random rand;
	
	// evolution parameters
	private double STDDEV_MUTATION = 0.1; // standard deviation in normal distribution for mutation
	private double P_MUTATION = 0.3; // mutation probability
	private double MIN_VALUE = 0; // minimum value in weight matrix
	private double MAX_VALUE = 10; // maximum value in weight matrix
	private double MEAN_FACTOR = 0.2; // changes how much the mean of the gaussian is influenced by knowledgeEvents
	private int POP_SIZE = 1;
	private int GROW_SIZE = 1;
	public ArrayList<WeightMatrix> population;
	public WeightMatrix currentBest;
	
	public Evo(KnowledgeBase kb, Random random)
	{
		this.kb = kb;
		this.population = new ArrayList<WeightMatrix>();
		this.rand = random;
		
		this.population.add(new WeightMatrix(rand));
		population.get(0).fitness = 0.0;
	}
	
	public void doNewCycle()
	{		
		// selection
		Collections.sort(population);
		currentBest = null;
		
		if(Agent.USE_BETTEREVO) { 
			// gibbs sampling, doesn't really work
//			double gibb = 0;
//			double r = Math.random();
//	    	double acum = 0;
//	    	
//			for(WeightMatrix wm : population) {
//				System.out.print(wm.fitness + ", ");
//				gibb += Math.pow(wm.fitness, Math.E);
//			}
//			for(WeightMatrix wm : population) {
//				acum += (Math.pow(wm.fitness, Math.E)) / (gibb);
//	    		if (acum > r) {
//	    			currentBest = wm;
//	    			break;
//	    		}
//	    		currentBest = wm;
//			}
//			System.out.println("");
			
			double[] pickChances = {0.9, 0.05, 0.03, 0.01, 0.01};
			double acum = 0;
			double r = Math.random();
			for(int i=0; i<population.size(); i++) {
				acum += pickChances[i];
				if(acum > r) {
					currentBest = population.get(i);
					population.set(0, currentBest);
					break;
				}
			}
		}
		
		if(currentBest == null) // just get the matrix with the highest fitness
			currentBest = population.get(0);
		
		population.subList(POP_SIZE, population.size()).clear();
//		System.out.println("best: " + currentBest.fitness);
		
		// expand
		if (POP_SIZE == 1)
			growPopulationFromSingle(currentBest);
		else
			growPopulationFromMultiple();
	}
	
	// mutate X amount of new matrices from given matrix
	public void growPopulationFromSingle(WeightMatrix parent)
	{
//		System.out.println("populationsize: " + population.size());
		for(int i=0; i<GROW_SIZE; i++)
		{
			WeightMatrix newMatrix = parent.copyAndMutate(kb, STDDEV_MUTATION, P_MUTATION, MIN_VALUE, MAX_VALUE, MEAN_FACTOR);
			population.add(newMatrix);
//			newMatrix.fitness = parent.fitness + 1;
//			newMatrix.print(kb);
		}
	}
	
	// using crossover and/or mutation
	public void growPopulationFromMultiple()
	{
		
	}
	
//	public ArrayList<WeightMatrix> getMatricesForEval()
//	{
//		ArrayList<WeightMatrix> unevaluatedMatrices = new ArrayList<WeightMatrix>();
//		for (WeightMatrix wm : population)
//			if (wm.fitness < 0)
//				unevaluatedMatrices.add(wm);
//		
//		return unevaluatedMatrices;
//	}
	
	public WeightMatrix getUnevaluatedMatrix()
	{
		if (POP_SIZE == 1) {
			WeightMatrix wm = population.get(population.size() - 1);
			if (wm.fitness < 0)
				return wm;
		} else {
			ListIterator<WeightMatrix> li = population.listIterator(population.size());
			while(li.hasPrevious()) {
				WeightMatrix wm = li.previous();
				if (wm.fitness < 0)
					return wm;
			}
		}
		return null;
	}
}
