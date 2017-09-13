package controllers.AIJim;

import ontology.Types;

public class KnowledgeEvent {
	
	public double occurances = 0.0;
	public double totalScore = 0;
	public int activeType; // avatar triggered the event or object created by avatar
	public int passiveType; // sprite type that is collided with
	
//	public Types.ACTIONS action; // action associated with this event
	
//	public Map<Types.ACTIONS, Double> avgScoreChangePerAction; // for influencing weightmatrix evolution
//	public Map<Types.ACTIONS, Integer> occurancesPerAction;
//	public Map<Types.ACTIONS, Integer> totalScoreChangePerAction;
	
	public KnowledgeEvent()
	{
		this.occurances = 0.0;
		this.totalScore = 0.0;
	}

	public KnowledgeEvent(/*Types.ACTIONS action, */int activeType, int passiveType)
	{
		this.occurances = 0.0;
		this.totalScore = 0.0;
		this.activeType = activeType;
		this.passiveType = passiveType;
		
//		this.action = action;
		
//		this.avgScoreChangePerAction = new HashMap<Types.ACTIONS, Double>();
//		this.occurancesPerAction = new HashMap<Types.ACTIONS, Integer>();
//		this.totalScoreChangePerAction = new HashMap<Types.ACTIONS, Integer>();
//		
//		this.avgScoreChangePerAction.put(action, (double) scoreChange);
//		this.occurancesPerAction.put(action, 1);
//		this.totalScoreChangePerAction.put(action, scoreChange);
	}
	
	//used to make a copy
	public KnowledgeEvent(double occurances, double totalScore, int activeType, int passiveType)
	{
		this.occurances = new Double(occurances);
		this.totalScore = new Double(totalScore);
		this.activeType = new Integer(activeType);
		this.passiveType = new Integer(passiveType);
	}
	
	public void update(/*Types.ACTIONS action, */double scoreChange)
	{
		this.occurances++;
		this.totalScore += scoreChange;
		
//		if(occurancesPerAction.get(action) == null)
//		{
//			occurancesPerAction.put(action, 1);
//			totalScoreChangePerAction.put(action, scoreChange);
//			avgScoreChangePerAction.put(action, (double) scoreChange);
//		}
//		else
//		{
//			occurancesPerAction.put(action, occurancesPerAction.get(action) + 1);
//			totalScoreChangePerAction.put(action, totalScoreChangePerAction.get(action) + scoreChange);
//			
//			double avg = occurancesPerAction.get(action) / totalScoreChangePerAction.get(action);
//			avgScoreChangePerAction.put(action, avg);
//		}
	}
	
//	public KnowledgeEvent copy()
//	{
//		return new KnowledgeEvent(occurances, totalScore, activeType, passiveType);
//	}
	
	
	
	public void print()
	{
		System.out.println("Actype: " + activeType + ", psvType: " + passiveType + ", Occurances: " + occurances + ", avgScoreChange: " + getAverageScoreChange() + ", total: " + totalScore);
	}
	
	// not actual score change, because of multiple events in same timestep
	public double getAverageScoreChange()
	{
		if(occurances == 0)
			return 0;
		
		return totalScore / occurances;
	}
}
