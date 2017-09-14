package tracks.singlePlayer.phillipAgents.jaydee.TreeSearch;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import core.game.StateObservation;
import tracks.singlePlayer.phillipAgents.jaydee.Agent;
import tracks.singlePlayer.phillipAgents.jaydee.Node;
import tracks.singlePlayer.phillipAgents.jaydee.Utils;

public class DBS extends TreeSearch {
    public DBS(Node origin) {
        super(origin);
    }

    @Override
    public void search() {
        Queue<Node> queue = new LinkedList<Node>();
        queue.add(this.origin);

        //System.out.println(origin.depth);
        int maxD = -1;
        while (!Agent.anyTime.isTimeOver()  && !queue.isEmpty()) {
            Agent.anyTime.updatePerLoop();

            Node current = queue.remove();
            if(current.state.isGameOver()){
                continue;
            }
            if(current.children.isEmpty()) {
                current.expand();
            } else {
                StateObservation obs = current.state;
                for(Node child: current.children.values()) {
                    current.state = obs.copy();
                    child.update();
                    child.updateAverageReward();
                }
            }
            List<Node> children = new LinkedList<Node>();
            children.addAll(current.children.values());
            Collections.sort(children, Utils.heuristicComparator);
            queue.addAll(children);
            if(current.depth > maxD) {
                maxD = current.depth;
            }
        }
        System.out.println(maxD);
        System.out.println(queue.size());
    }

    @Override
    public void roll(Node origin) {
        this.origin = origin;
    }
}
