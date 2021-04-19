package searchclient;

import java.util.ArrayList;
import java.util.HashSet;

public class GraphSearch {

    public static State search(State initialState, Frontier frontier) {

        //Part 2:
        //Now try to implement the Graph-Search algorithm from R&N figure 3.7
        //In the case of "failure to find a solution" you should return null.
        //Some useful methods on the state class which you will need to use are:
        //state.isGoalState() - Returns true if the state is a goal state.
        //state.extractPlan() - Returns the Array of actions used to reach this state.
        //state.getExpandedStates() - Returns an ArrayList<State> containing the states reachable from the current state.
        //You should also take a look at Frontier.java to see which methods the Frontier interface exposes
        //
        //printSearchStates(explored, frontier): As you can see below, the code will print out status
        //(#explored states, size of the frontier, #generated states, total time used) for every 10000th node generated.
        //You might also find it helpful to print out these stats when a solution has been found, so you can keep
        //track of the exact total number of states generated.


        int iterations = 0;

        frontier.add(initialState);
        HashSet<State> explored = new HashSet<>();

        while (true) {

            //Print a status message every 10000 iteration
            if (++iterations % 10000 == 0) {
                printSearchStatus(explored, frontier);
            }

            if (frontier.isEmpty()) {
                printSearchStatus(explored, frontier);
                return null;
            }

            State s = frontier.pop();
            if (s.isGoalState()) {
                printSearchStatus(explored, frontier);
                return s; //TODO changed
            }

            explored.add(s);


            ArrayList<State> States = s.getExpandedStates();
            for (int i = 0; i < States.size(); ++i) {
                State expanded = States.get(i);
                if (!explored.contains(expanded) && !frontier.contains(expanded)) {
                    frontier.add(expanded);
                }
            }
        }
    }

    private static long startTime = System.nanoTime();

    private static void printSearchStatus(HashSet<State> explored, Frontier frontier) {
        String statusTemplate = "#Expanded: %,8d, #Frontier: %,8d, #Generated: %,8d, Time: %3.3f s\n%s\n";
        double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000d;
        System.err.format(statusTemplate, explored.size(), frontier.size(), explored.size() + frontier.size(),
                elapsedTime, Memory.stringRep());
    }
}
