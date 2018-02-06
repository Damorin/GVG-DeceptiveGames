package tracks.singlePlayer.agentsForDeceptiveGames.Rooot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

public class Observer {
	public ArrayList<Types.ACTIONS> actions;
	public HashMap<Integer, Types.ACTIONS> action_mapping;
	public HashMap<Types.ACTIONS, Integer> r_action_mapping;
	public ArrayList<Observation>[] npcPositions;
	public ArrayList<Observation>[] resourcePositions;
	public ArrayList<Observation>[] portalPositions;
	public ArrayList<Observation>[] immPositions;
	public ArrayList<Observation>[][] map;
	public int blockSize;

	public LinkedList<Integer> harmfulNPCs;

	public int mapX;
	public int mapY;
	public int[][] visitedRecord;
	boolean hasNPC;
	boolean manyNPC;
	boolean hasResource;
	boolean hasPortal;
	boolean canAttack;
	Vector2d exitPosition;

	public LinkedList<Integer> npcTypes;
	public LinkedList<Integer> portalTypes;
	public LinkedList<Integer> srcTypes;
	public LinkedList<Integer> immTypes;

	public Observer(StateObservation stateObs) {
		harmfulNPCs = new LinkedList<Integer>();
		npcTypes = new LinkedList<Integer>();
		portalTypes = new LinkedList<Integer>();
		srcTypes = new LinkedList<Integer>();
		immTypes = new LinkedList<Integer>();

		actions = stateObs.getAvailableActions();
		action_mapping = new HashMap<Integer, Types.ACTIONS>();
		r_action_mapping = new HashMap<Types.ACTIONS, Integer>();
		for (int i = 0; i < actions.size(); i++) {
			action_mapping.put(i, actions.get(i));
			r_action_mapping.put(actions.get(i), i);
		}

		npcPositions = stateObs.getNPCPositions();
		hasNPC = (npcPositions == null) ? false : true;
		if (hasNPC) {
			manyNPC = (npcPositions.length == 1) ? false : true;
			canAttack = (actions.contains(Types.ACTIONS.ACTION_USE)) ? true
					: false;
		}

		resourcePositions = stateObs.getResourcesPositions();
		hasResource = (resourcePositions == null) ? false : true;

		portalPositions = stateObs.getPortalsPositions();
		hasPortal = (portalPositions == null) ? false : true;

		map = stateObs.getObservationGrid();
		blockSize = stateObs.getBlockSize();
		mapX = map.length;
		mapY = map[0].length;
		visitedRecord = new int[mapY][mapX];
		exitPosition = null;
	}

	public double closestFriend(StateObservation stateObs) {
		ArrayList<Observation>[] npcToAvatar;
		Vector2d avatarPosition = stateObs.getAvatarPosition();
		npcToAvatar = stateObs.getNPCPositions(avatarPosition);
		double chaseDistance = 0;
		if (npcToAvatar != null) {
			for (ArrayList<Observation> npcs : npcToAvatar) {
				if (npcs.size() > 0
						&& !harmfulNPCs.contains(npcs.get(0).itype)) {
					chaseDistance = npcs.get(0).sqDist;
					break;
				}
			}
		}
		return chaseDistance;
	}

	public double closestEnemy(StateObservation stateObs) {
		ArrayList<Observation>[] npcToAvatar;
		Vector2d avatarPosition = stateObs.getAvatarPosition();
		npcToAvatar = stateObs.getNPCPositions(avatarPosition);
		double awayDistance = 0;
		if (npcToAvatar != null) {
			for (ArrayList<Observation> npcs : npcToAvatar) {
				if (npcs.size() > 0 && harmfulNPCs.contains(npcs.get(0).itype)) {
					awayDistance = npcs.get(0).sqDist;
					break;
				}
			}
		}
		return awayDistance;
	}

	public double closestSRC(StateObservation stateObs) {
		ArrayList<Observation>[] srcToAvatar;
		Vector2d avatarPosition = stateObs.getAvatarPosition();
		srcToAvatar = stateObs.getResourcesPositions(avatarPosition);
		double distance = 0;
		if (srcToAvatar != null) {
			for (ArrayList<Observation> srcs : srcToAvatar) {
				if (srcs.size() > 0) {
					distance = srcs.get(0).sqDist;
					break;
				}
			}
		}
		return distance;
	}

	public double closestPortal(StateObservation stateObs) {
		ArrayList<Observation>[] portalToAvatar;
		Vector2d avatarPosition = stateObs.getAvatarPosition();
		portalToAvatar = stateObs.getPortalsPositions(avatarPosition);
		double distance = 0;
		if (portalToAvatar != null) {
			for (ArrayList<Observation> portals : portalToAvatar) {
				if (portals.size() > 0) {
					distance = portals.get(0).sqDist;
					break;
				}
			}
		}
		if(exitPosition != null){
			distance += 2*avatarPosition.sqDist(exitPosition);
			distance /= 3;
		}
		return distance;
	}

	public double rawScore(StateObservation stateObs) {
		return stateObs.getGameScore();
	}

	public boolean gameover(StateObservation stateObs) {
		return stateObs.isGameOver();
	}

	public Types.WINNER winner(StateObservation stateObs) {
		return stateObs.getGameWinner();
	}

	public int visitedCount(StateObservation stateObs) {
		Vector2d avatarPosition = stateObs.getAvatarPosition();
		int avatarX = (int) avatarPosition.x / blockSize;
		int avatarY = (int) avatarPosition.y / blockSize;
		if(avatarX < 0) avatarX = 0;
		if(avatarY < 0) avatarY = 0;
		if(avatarX > mapX - 1) avatarX = mapX - 1;
		if(avatarY > mapY - 1) avatarY = mapY - 1;
		return visitedRecord[avatarY][avatarX];
	}

	// FOR DEBUG
	public void printVisitedRecord() {
		System.out.println(mapX + " " + mapY);
		for (int i = 0; i < visitedRecord.length; i++) {
			for (int j = 0; j < visitedRecord[0].length; j++) {
				System.out.print(visitedRecord[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println();
		System.out.println();
	}

	public void avatarVisited(StateObservation stateObs, Types.ACTIONS action) {
		Vector2d avatarPosition = stateObs.getAvatarPosition();
		int avatarX = (int) avatarPosition.x / blockSize;
		int avatarY = (int) avatarPosition.y / blockSize;
		/*
		 * if(action == Types.ACTIONS.ACTION_DOWN){ avatarY += 1; } else
		 * if(action == Types.ACTIONS.ACTION_LEFT){ avatarX -= 1; } else
		 * if(action == Types.ACTIONS.ACTION_UP){ avatarY -= 1; } else if(action
		 * == Types.ACTIONS.ACTION_RIGHT){ avatarX += 1; }
		 */
		if (action != Types.ACTIONS.ACTION_USE) {
			if(avatarX < 0) avatarX = 0;
			if(avatarY < 0) avatarY = 0;
			if(avatarX > mapX - 1) avatarX = mapX - 1;
			if(avatarY > mapY - 1) avatarY = mapY - 1;
			visitedRecord[avatarY][avatarX] += 1;
		}
	}

	public void onLearning(StateObservation curState, StateObservation advState) {
		hasNPC = false;
		ArrayList<Observation>[] curNPC = curState.getNPCPositions();
		if(curNPC != null){
			for(ArrayList<Observation> npcs : curNPC){
				if(npcs.size() > 0){
					hasNPC = true;
					break;
				}
			}
		}

		//if(!canAttack && hasNPC && !hasPortal && !hasResource){
		//	harmfulNPCs.clear();
		//}
		
		if (advState.getGameWinner() == Types.WINNER.PLAYER_LOSES || avatarLive(advState) < avatarLive(curState)) {
			TreeSet<Event> eventHistory = advState.getEventsHistory();
			if (eventHistory.size() > 0) {
				Event event = eventHistory.last();
				if (event.fromAvatar == false && !harmfulNPCs.contains(event.passiveTypeId)) {
					harmfulNPCs.add(event.passiveTypeId);
				}
			}
		} else if(!canAttack && advState.getGameScore() > curState.getGameScore() && avatarLive(advState) >= avatarLive(curState)){
			TreeSet<Event> eventHistory = advState.getEventsHistory();
			if (eventHistory.size() > 0) {
				Event event = eventHistory.last();
				if (event.fromAvatar == false && npcTypes.contains(event.passiveTypeId)) {
					Iterator<Integer> iter = harmfulNPCs.iterator();
					while(iter.hasNext()){
						if(iter.next() == event.passiveTypeId){
							iter.remove();
						}
					}
				}
			}
		}
		
		if(advState.getGameWinner() != Types.WINNER.PLAYER_LOSES){
			Vector2d avatarPos = advState.getAvatarPosition();
			ArrayList<Observation>[] npcPos = advState.getNPCPositions(avatarPos);
			Vector2d closestNPC;
			if(npcPos != null){
				for(ArrayList<Observation> npcs : npcPos){
					if(npcs.size() > 0){
						closestNPC = npcs.get(0).position;
						if(closestNPC.x == avatarPos.x && closestNPC.y == avatarPos.y){
							Iterator<Integer> iter = harmfulNPCs.iterator();
							while(iter.hasNext()){
								if(iter.next() == npcs.get(0).itype){
									iter.remove();
								}
							}
							break;
						}
					}
				}
			}
			
		}
		
		
	}

	public void offLearning(StateObservation stateObs) {
		if (npcPositions != null) {
			for (ArrayList<Observation> npcs : npcPositions) {
				if (npcs.size() > 0) {
					npcTypes.offer(npcs.get(0).itype);
				}
			}
			for(Integer type : npcTypes){
				harmfulNPCs.offer(type);
			}
		}
		if (portalPositions != null) {
			for (ArrayList<Observation> portals : portalPositions) {
				if (portals.size() > 0) {
					portalTypes.offer(portals.get(0).itype);
					if(portals.size() == 1){
						exitPosition = portals.get(0).position;
					}
				}
			}
		}
		if (resourcePositions != null) {
			for (ArrayList<Observation> srcs : resourcePositions) {
				if (srcs.size() > 0) {
					srcTypes.offer(srcs.get(0).itype);
				}
			}
		}
		if(immPositions != null){
			for(ArrayList<Observation> imms : immPositions){
				if(imms.size() > 0){
					immTypes.offer(imms.get(0).itype);
				}
			}
		}
		if(!canAttack && hasNPC && !hasPortal && !hasResource){
			harmfulNPCs.clear();
		}
	}
	
	public int numNPCs(StateObservation stateObs){
		ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions();
		int counter = 0;
		if(npcPositions != null){
			for(ArrayList<Observation> npcs : npcPositions){
				counter += npcs.size();
			}
		}
		return counter;
	}
	
	public int numImms(StateObservation stateObs){
		ArrayList<Observation>[] immPos = stateObs.getImmovablePositions();
		int counter = 0;
		if(immPos != null){
			for(ArrayList<Observation> imms : immPos){
				counter += imms.size();
			}
		}
		return counter;
	}
	
	public int avatarLive(StateObservation stateObs){
		HashMap<Integer, Integer> avatarResource = stateObs.getAvatarResources();
		int live = 0;
		for(Object src : avatarResource.keySet()){
			live += avatarResource.get(src);
		}
		return live;
	}
}
