package controllers.AIJim;

import java.util.ArrayList;
import java.util.HashMap;

import core.game.Observation;

import tools.Vector2d;

public class GridCell implements Comparable<GridCell> {
	
	public ArrayList<GridCell> neighbors;
	public double distanceHeuristicScore;
	public boolean traversable;
//	public ArrayList<Observation> observations;
	public Vector2d location;
	public GridCell predecessor;
	public int distanceScore;
	public double totalScore;

	public GridCell(Vector2d location, ArrayList<Observation>[][] obsGrid, GridCell[][] grid) 
	{
		this.location = location;
		this.traversable = true;
		this.neighbors = new ArrayList<GridCell>();
		
		int x = (int) location.x;
		int y = (int) location.y;
		
		ArrayList<Observation> obs = obsGrid[x][y];
		for(Observation ob : obs) {
			if(ob.itype == 0) { //wall
				this.traversable = false;
				if(ShortestPath.noPathFindingNeeded && 
						x > 0 && x < obsGrid.length - 1 && y > 0 && y < obsGrid[0].length - 1) // wall objects inside level
				{
					ShortestPath.noPathFindingNeeded = false;
					System.out.println("Objects detected");
				}
			}
		}
		
		if(location.x > 0)
			addNeighbor(grid[(int) location.x - 1][(int) location.y]);
		if(location.y > 0)
			addNeighbor(grid[(int) location.x][(int) location.y - 1]);
	}
	
	public void init(GridCell goal)
	{
//		this.observations = obsGrid[(int) location.x][(int) location.y];
		this.distanceScore = Integer.MAX_VALUE;
		this.totalScore = Double.MAX_VALUE;
		this.predecessor = null;
		
		// distance heuristic is euclidean distance from this cell to goal cell
		this.distanceHeuristicScore = this.location.dist(goal.location);
	}
	
	private void addNeighbor(GridCell neighbor)
	{
		this.neighbors.add(neighbor);
		neighbor.neighbors.add(this);
	}
	
	// sorts ascending to totalScore
	@Override
    public int compareTo(GridCell other) {
        if(this.totalScore < other.totalScore)        return -1;
        else if(this.totalScore < other.totalScore)   return 1;
        return 0;
    }
}
