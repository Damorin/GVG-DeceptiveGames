package tracks.singlePlayer.agentsForDeceptiveGames.jaydee.TreeSearch;

import tracks.singlePlayer.agentsForDeceptiveGames.jaydee.Node;

public abstract class TreeSearch {
	public Node origin = null;

	public TreeSearch(Node origin) {
		this.origin = origin;
	}

	public abstract void search();

	public abstract void roll(Node origin);
}
