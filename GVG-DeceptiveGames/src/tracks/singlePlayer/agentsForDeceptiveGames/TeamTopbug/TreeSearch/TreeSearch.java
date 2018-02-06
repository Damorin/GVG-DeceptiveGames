package tracks.singlePlayer.agentsForDeceptiveGames.TeamTopbug.TreeSearch;

import tracks.singlePlayer.agentsForDeceptiveGames.TeamTopbug.Node;

public abstract class TreeSearch {
	public Node origin = null;

	public TreeSearch(Node origin) {
		this.origin = origin;
	}

	public abstract void search();

	public abstract void roll(Node origin);
}
