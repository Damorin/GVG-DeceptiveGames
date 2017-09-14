package tracks.singlePlayer.phillipAgents.AIJim;

import java.util.ArrayList;
import java.util.Collections;

import ontology.Types;
import ontology.Types.ACTIONS;

import tools.Vector2d;

import core.game.Observation;
import core.game.StateObservation;

public final class ShortestPath {
	
	private static GridCell[][] grid;
	
	public static ArrayList<Integer> unreachableFeatureIds;
	public static boolean noPathFindingNeeded;
	
	private static ArrayList<Vector2d> path;
	
	private ShortestPath () {}
	
	public static void initialize(StateObservation state) {
		
		noPathFindingNeeded = true;
		unreachableFeatureIds = new ArrayList<Integer>();
		path = new ArrayList<Vector2d>();
		
		ArrayList<ACTIONS> actions = state.getAvailableActions();
		int nrOfMoveActions = 0;
		for(ACTIONS action : actions) {
			if(Agent.isMoveAction(action)) {
				nrOfMoveActions++;
			}
		}
		if(nrOfMoveActions <= 2) {
			System.out.println("Number of move actions: " + nrOfMoveActions + ", no pathfinding needed");
			return;
		}
		
		ArrayList<Observation>[][] obsGrid = state.getObservationGrid();
		grid = new GridCell[obsGrid.length][obsGrid[0].length];
		
		for(int x=0; x<obsGrid.length; x++) {
			for(int y=0; y<obsGrid[x].length; y++) {
				GridCell cell = new GridCell(new Vector2d(x,y), obsGrid, grid);
				grid[x][y] = cell;
			}
		}
	}
	
	// finds the length of the shortestPath from avatar position to goal feature
	public static double shortestPathDistance(StateObservation state, Feature feature) 
	{
		if(noPathFindingNeeded)
			return feature.dist;
		
		Vector2d avatarPos = state.getAvatarPosition();
		if(avatarPos.x < 0 || avatarPos.y < 0) // state sometimes returns -1,-1
			return feature.dist;
		
		if(unreachableFeatureIds.contains(feature.id))
			return -1;
		
		Vector2d startPos = new Vector2d((int) avatarPos.x / state.getBlockSize(), 
			(int) avatarPos.y / state.getBlockSize());
		Vector2d endPos = new Vector2d((int) feature.position.x / state.getBlockSize(), 
			(int) feature.position.y / state.getBlockSize());
//		ArrayList<Observation>[][] obsGrid = state.getObservationGrid();
		
		if (startPos.x < 0 || startPos.y < 0 || endPos.x < 0 || endPos.y < 0 
				|| startPos.x >= grid.length || startPos.y >= grid[0].length || endPos.x >= grid.length || endPos.y >= grid[0].length)
			return feature.dist;
		
//		System.out.println("feature: " + feature.type + ", start: " + startPos.toString() + ", end: " + endPos.toString());
		
		GridCell start = grid[(int) startPos.x][(int) startPos.y];
		GridCell goal = grid[(int) endPos.x][(int) endPos.y];
		
		ArrayList<GridCell> openSet = new ArrayList<GridCell>(); // cells in queue that still need evaluation
		ArrayList<GridCell> closedSet = new ArrayList<GridCell>(); // cells already evaluated
		
		// initialize
		for(int x=0; x<grid.length; x++)
			for(int y=0; y<grid[x].length; y++)
				grid[x][y].init(goal);

		start.distanceScore = 0;
		start.totalScore = start.distanceScore + start.distanceHeuristicScore;
		openSet.add(start);
		
		// main algorithm
		while(openSet.size() > 0)
		{
			// get the cell with the lowest score
			Collections.sort(openSet);
			GridCell current = openSet.get(0);
			
			// found the goal cell
			if(current.location.equals(endPos))
			{
				if(Agent.USE_BETTEREVO) {
					feature.pathStartLocation = getPathStart(current, start).mul(state.getBlockSize());
				}
				return current.distanceScore * state.getBlockSize();
			}
			
			openSet.remove(current);
			closedSet.add(current);
			for(GridCell neighbor : current.neighbors)
			{
				if(!neighbor.traversable || closedSet.contains(neighbor))
					continue;
				
				if(openSet.contains(neighbor) && current.distanceScore + 1 >= neighbor.distanceScore)
					continue;
				
				neighbor.predecessor = current; // needed for reconstruction
				neighbor.distanceScore = current.distanceScore + 1;
				neighbor.totalScore = neighbor.distanceScore + neighbor.distanceHeuristicScore;
				if(!openSet.contains(neighbor))
					openSet.add(neighbor);
			}
		}
		
		// can't be found
		unreachableFeatureIds.add(feature.id);
		return -1;
	}
	
	// reconstructs the actual path
//	private static ArrayList<Vector2d> reconstructShortestPath(GridCell current, GridCell start)
//	{
//		path.clear();
//		path.add(current.location);
//		GridCell predecessor = current.predecessor;
//		while(!predecessor.equals(start))
//		{
//			path.add(predecessor.location);
//			predecessor = predecessor.predecessor;
//		}
////		path.add(start.location);
//		return path;
//	}
	
	private static Vector2d getPathStart(GridCell current, GridCell start)
	{
		GridCell predecessor = current.predecessor;
		if(predecessor == null)
			return start.location.copy();

		while(!predecessor.equals(start))
			predecessor = predecessor.predecessor;

		return predecessor.location.copy();
	}
}
