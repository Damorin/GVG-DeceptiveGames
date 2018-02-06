 package tracks.singlePlayer.agentsForDeceptiveGames.ICELab.OpenLoopRLBiasMCTS.TreeSearch;

import tracks.singlePlayer.agentsForDeceptiveGames.ICELab.OpenLoopRLBiasMCTS.Node;

public abstract class TreeSearch {
    public Node origin = null;
    public TreeSearch(Node origin) {
        this.origin = origin;
    }
    public abstract void search();
    public abstract void roll(Node origin);
}
