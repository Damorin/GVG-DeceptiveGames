package tracks.singlePlayer.phillipAgents.AIJim;

import tools.Vector2d;
import core.game.Observation;

public class Feature {
	
	public int type;
	public double dist;
	Vector2d position; //temp
	int id; //temp
	Vector2d pathStartLocation;

	public Feature(Observation obs) 
	{
		this.type = obs.itype;
		this.dist = obs.sqDist;
		position = obs.position;
		id = obs.obsID;
	}
	
	// used to make a copy
	public Feature(int type, double dist)
	{
		this.type = new Integer(type);
		this.dist = new Double(dist);
	}
	
	// used to make a copy TEMP
	public Feature(int type, double dist, Vector2d pos, int id)
	{
		this.type = new Integer(type);
		this.dist = new Double(dist);
		position = new Vector2d(pos);
		this.id = id;
	}
	
	public double getValue()
	{
		if(dist <= 0)
			return 0;
		
//		return 1 / dist; // paper uses just dist?
		return dist;
	}
}
