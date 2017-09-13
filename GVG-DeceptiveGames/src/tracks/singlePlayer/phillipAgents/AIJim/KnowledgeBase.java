package controllers.AIJim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tools.Vector2d;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;

public class KnowledgeBase {
	
	public StateObservation state;
	public HashMap<Integer, Feature> featureSet;
	public HashMap<Integer, KnowledgeEvent> eventSet;
	public double SCORE_ALPHA = (2/3);
	public double SCORE_BETA = 1 - SCORE_ALPHA;

	public KnowledgeBase()
	{
		this.featureSet = new HashMap<Integer, Feature>();
		this.eventSet = new HashMap<Integer, KnowledgeEvent>();
		// eventset voor action->collisionwithspritetype, opslaan wat scorechange is (gehele game), gebruiken voor weightmatrix evo bias.
	}
	
	// copies this knowledgebase from given one
	public KnowledgeBase(KnowledgeBase kb)
	{
		this.featureSet = new HashMap<Integer, Feature>();
		for(Map.Entry<Integer, Feature> entry : kb.featureSet.entrySet())
		{
			Feature f = entry.getValue();
//			Feature fCopy = new Feature(f.type, f.dist);
			Feature fCopy = new Feature(f.type, f.dist, f.position, f.id); //temp
			this.featureSet.put(entry.getKey(), fCopy);
		}
		this.eventSet = new HashMap<Integer, KnowledgeEvent>();
		for(Map.Entry<Integer, KnowledgeEvent> entry : kb.eventSet.entrySet())
		{
			KnowledgeEvent ke = entry.getValue();
			KnowledgeEvent keCopy = new KnowledgeEvent(ke.occurances, ke.totalScore, ke.activeType, ke.passiveType);
			this.eventSet.put(entry.getKey(), keCopy);
		}
	}
	
	public void update(StateObservation state, boolean usePathFinding)
	{
		this.state = state;
		updateFeatures(usePathFinding);
	}
	
	public void updateFeatures(boolean usePathFinding)
	{
		Vector2d avatarPos = state.getAvatarPosition();
		updateFeatureSet(state.getNPCPositions(avatarPos), false, usePathFinding);
		updateFeatureSet(state.getImmovablePositions(avatarPos), false, usePathFinding);
		updateFeatureSet(state.getMovablePositions(avatarPos), false, usePathFinding);
		updateFeatureSet(state.getPortalsPositions(avatarPos), false, usePathFinding);
		updateFeatureSet(state.getResourcesPositions(avatarPos), false, usePathFinding);

//		updateFeatureSet(so.getFromAvatarSpritesPositions(avatarPos)); zou voor sommige spellen nuttig kunnen zijn, voor de meeste alleen maar onhandig
		
//		for(Observation obs : featureSet) {
//			if(obs != null)
//				System.out.println("type: " + obs.itype + " value: " + obs.sqDist);
//		}
		
//		if (so.getEventsHistory().size() > 0) {
//			Event event = so.getEventsHistory().last();
//		
//			System.out.println("step:" + event.gameStep + " fromavt:" + event.fromAvatar + " actype:" + event.activeTypeId + " pastype:" + event.passiveTypeId);
//		}
		
//		if (so.getGameTick() == 100) {
//			for (KnowledgeEvent know : eventSet.values()) {
//				know.print();
//			}
//		}
	}
	
	private void updateFeatureSet(ArrayList<Observation>[] observations, boolean printTypes, boolean usePathFinding) 
	{
		if (observations != null && observations.length > 0)
		{
			for(int i=0; i<observations.length; i++) // loop over observation types
			{
				if (observations[i].size() > 0)
				{
					Observation obs = observations[i].get(0); // get closest observation
					
					if(obs.itype == 0) // wall
						continue;
					
					Feature feature = new Feature(obs);
					
					if(Agent.USE_SHORTESTPATH && usePathFinding) {
						feature.dist = ShortestPath.shortestPathDistance(state, feature);
//						System.out.println("dist: " + feature.dist);
					}
					
					if(printTypes) {
						System.out.println("cat " + obs.category + ", itype " + obs.itype + ", id " + obs.obsID);
					}
					
					featureSet.put(obs.itype, feature);
				}
			}
		}
	}
	
	public void updateEventSet(Event event, double scoreChange)
	{
		int index = event.passiveTypeId;
		if (index != 0) // 0 is a wall
		{
			if (event.fromAvatar == true) // not caused by the collision with avatar sprite
				index += 100;
			
			KnowledgeEvent ke = eventSet.get(index);
			if (ke == null) {
				ke = new KnowledgeEvent(event.activeTypeId, event.passiveTypeId);
				ke.update(scoreChange);
				eventSet.put(index, ke);
			} else {
				ke.update(scoreChange);
			}
		}
	}

	// returns score change if it's not 0 and otherwise a combination of knowledge and distance change
	public double[] getRolloutScore(KnowledgeBase rollerKb)
	{
		double[] scores = new double[4];
//		System.out.println(rollerKb.state.toString());
//		System.out.println(this.state.toString());
		double scoreChange = rollerKb.state.getGameScore() - this.state.getGameScore();
		if(scoreChange != 0.0) {
//		if (true) {
			

//			return scoreChange * 10;
			if(Agent.USE_COURAGEOVERTIME && scoreChange < 0) {
				double gameTick = this.state.getGameTick();
				scoreChange = ((2000 - gameTick) / 2000) * scoreChange;
			}
			
			scores[0] = scoreChange;
			scores[1] = 0.0;
			scores[2] = 0.0;
			scores[3] = 0.0;
			return scores;
		}
		
//		System.out.println("");
		
		// calculate knowledge change and distance change
		double knowledgeChange = 0;
		double distanceChange = 0;
		int totalNrOfEvents = 0;
		if(Agent.USE_KNOWLEDGEFACTOR) {
			for(KnowledgeEvent event : rollerKb.eventSet.values()) {
				totalNrOfEvents += event.occurances;
			}
		}
		
		for(Map.Entry<Integer, KnowledgeEvent> entry : rollerKb.eventSet.entrySet())
		{	
			KnowledgeEvent rollerEvent = entry.getValue();
			KnowledgeEvent origEvent = this.eventSet.get(entry.getKey());
			
//				System.out.print("----roller ");
//				rollerEvent.print();
			if(origEvent == null)
				origEvent = new KnowledgeEvent();
			
			if(origEvent.occurances == 0)
				knowledgeChange += rollerEvent.occurances;
			else
				knowledgeChange += (rollerEvent.occurances / origEvent.occurances) - 1;
		}
		
			
		for(Map.Entry<Integer, Feature> entry : rollerKb.featureSet.entrySet())
		{
			Feature rollerFeature = entry.getValue();
			Feature origFeature = this.featureSet.get(entry.getKey());
			if(origFeature != null) // when in rollout a feature is discovered that hasn't been encountered in the real game
			{
				double occurances = 0; // occurances of collision with avatar and fromAvatar are taken together
				KnowledgeEvent origEventAvatar = eventSet.get(entry.getKey());
				KnowledgeEvent origEventFromAvatar = eventSet.get(entry.getKey() + 100);
				if(origEventAvatar != null)
					occurances += origEventAvatar.occurances;
				if(origEventFromAvatar != null)
					occurances += origEventFromAvatar.occurances;
				
				double avgScoreChange = 0.0; // average scorechange from collision with avatar and fromAvatar are added (good?)
				KnowledgeEvent rollerEventAvatar = rollerKb.eventSet.get(entry.getKey());
				KnowledgeEvent rollerEventFromAvatar = rollerKb.eventSet.get(entry.getKey() + 100);
				if(rollerEventAvatar != null)
					avgScoreChange += rollerEventAvatar.getAverageScoreChange();
				if(rollerEventFromAvatar != null)
					avgScoreChange += rollerEventFromAvatar.getAverageScoreChange();
				
//				System.out.println("TYPE " + origFeature.type + ", occ " + occurances + ", scchg " + avgScoreChange + ", ENDDIST " + rollerFeature.dist + ", STARTDIST " + origFeature.dist + ", ENDPOS " + rollerFeature.position.toString() + ", STARTPOS " + origFeature.position.toString() + ", ENDID " + origFeature.id + ", STARTID " + rollerFeature.id + ", DIST " + rollerFeature.position.dist(origFeature.position));
//				System.out.println("TYPE " + origFeature.type + ", occ " + occurances + ", scchg " + avgScoreChange + ", ENDDIST " + rollerFeature.dist + ", STARTDIST " + origFeature.dist);
				
				if(occurances == 0 || (origFeature.dist > 0.0 && avgScoreChange > 0.2)) // 0.2 instead of 0 because we don't know score change for sure
				{
					// knowledge factor
					double knowledgeFactor = 1;
					if(Agent.USE_KNOWLEDGEFACTOR) {
						double kOcc = 0; 
						if(totalNrOfEvents > 1 && (rollerEventAvatar != null || rollerEventFromAvatar != null)) {
							if(rollerEventAvatar != null)
								kOcc += rollerEventAvatar.occurances;
							if(rollerEventFromAvatar != null)
								kOcc += rollerEventFromAvatar.occurances;
							knowledgeFactor = 1 + (((9 * totalNrOfEvents) - (9 * kOcc)) / ((totalNrOfEvents * kOcc) - kOcc));
						}
//						System.out.println("KnowledgeFactor: " + knowledgeFactor + "    " + totalNrOfEvents + "    " + kOcc);
					}
					
					if (origFeature.dist == 0.0 || rollerFeature.dist == 0.0) { //sometimes a sprite is spawned underneath the avatar
						KnowledgeEvent ke = new KnowledgeEvent(1, rollerFeature.type); // sometimes no event is created so make a custom one
						ke.update(0.0);
						rollerKb.eventSet.put(rollerFeature.type, ke);
						continue;
					}

					double distScore = 1 - (rollerFeature.dist / origFeature.dist);
					distanceChange += knowledgeFactor * distScore;			
				}
			}
		}
//		System.out.println("                            scoreChange: " + scoreChange + ", knowledgeChange: " + knowledgeChange + ", distanceChange: " + distanceChange);

//		return 0.0;
//		return (SCORE_ALPHA * knowledgeChange) + (SCORE_BETA * distanceChange);
		scores[0] = (SCORE_ALPHA * knowledgeChange) + (SCORE_BETA * distanceChange);
		scores[1] = knowledgeChange;
		scores[2] = distanceChange;
		scores[3] = scoreChange;
		
		return scores;
	}
	
	public void resetScoreChange() {
		for(KnowledgeEvent event : this.eventSet.values()) {
			event.totalScore = 0.0;
		}
	}
	
	private void printAllObservations(ArrayList<Observation>[] observations) 
	{
		if (observations != null && observations.length > 0)
		{
			System.out.println("category: " + observations[0].get(0).category);
			for(int i=0; i<observations.length; i++)
			{
				int type = observations[i].get(0).itype;
				System.out.println("--type: " + type);
				for(Observation obs : observations[i]) 
				{
					double dist = obs.sqDist;
					System.out.println("----distance: " + dist);
				}
			}
		}
	}
}
