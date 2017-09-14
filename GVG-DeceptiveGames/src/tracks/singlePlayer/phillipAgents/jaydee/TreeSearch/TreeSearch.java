package tracks.singlePlayer.phillipAgents.jaydee.TreeSearch;

import tracks.singlePlayer.phillipAgents.jaydee.Node;

public abstract class TreeSearch {
	public Node origin = null;

	public TreeSearch(Node origin) {
		this.origin = origin;
	}

	public abstract void search();

	public abstract void roll(Node origin);
}
