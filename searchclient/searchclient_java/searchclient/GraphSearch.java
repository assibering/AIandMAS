package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

public class GraphSearch {

    public static Action[][] search(State initialState, Frontier frontier)
    {
        boolean outputFixedSolution = false;

        if (outputFixedSolution) {
            //Part 1:
            //The agents will perform the sequence of actions returned by this method.
            //Try to solve a few levels by hand, enter the found solutions below, and run them:

            return new Action[][] {
                {Action.PushEE}
            };
        } else {
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
            
            
//            System.err.println("GOALSTATE: " + Arrays.deepToString(initialState.goals));
            
            System.err.println(frontier.getName().contains("greedy"));
            boolean greedy = false;
            boolean wastar = false;
            boolean astar = false;
            
            if (frontier.getName().contains("greedy")) {
            	greedy = true;
            }
            
            if (frontier.getName().contains("WA*")) {
            	wastar = true;
            }
            
            if (frontier.getName().contains("A*")) {
            	astar = true;
            }
            
            LinkedList<char[][]> subgoals = new LinkedList<char[][]>();
            
            LinkedList<char[][]>[] MAsubgoals = new LinkedList[initialState.agentRows.length];
            
            for (int agent=0; agent<initialState.agentRows.length; agent++) {
            	subgoals = initialState.getAgentSubGoals(agent);
            	MAsubgoals[agent] = subgoals;
            }
            
            LinkedList<Action[]> actions = new LinkedList<Action[]>();
            
            State s = new State(initialState.agentRows, initialState.agentCols, State.agentColors,
            		initialState.walls, initialState.boxes, State.boxColors, initialState.goals);
            
            int agents = s.agentRows.length;
            
            CentralPlanner planner = new CentralPlanner(s);
            
            for (int agent=0; agent<agents; agent++) {
            	
            	subgoals = MAsubgoals[agent];
            	int[] aRows = s.getSingleAgentRow(agent);
            	int[] aCols = s.getSingleAgentCol(agent);
            	char[][] aBoxes = s.getSingleAgentBoxes(agent);
            	boolean first_subgoal = true;
            	
            	actions = new LinkedList<Action[]>(); 
            	
            	 for (char[][] subgoal : subgoals) {
            		 
            		System.err.println("NEW STATE - NEW SUBGOAL");
            		
//            		if (!first_subgoal) {
//            			for (int i=0; i<subgoal.length; i++) {
//                			for (int j=0; j<subgoal[i].length; j++) {
//                				subgoal[i][j] += s.goals[i][j];
//                			}
//                		}
//            		}
            		
                 	s = new State(aRows, aCols, State.agentColors,
                 		s.walls, aBoxes, State.boxColors, subgoal);
                 	
                 	System.err.println("SUBGOAL");
                 	for (int i=0; i<subgoal.length; i++) {
            			System.err.println(Arrays.toString(s.goals[i]));
            		}
                 	System.err.println("BOXES");
            		for (int i=0; i<aBoxes.length; i++) {
            			System.err.println(Arrays.toString(s.boxes[i]));
            		}
            		System.err.println("AGENT POSITION");
            		System.err.println("Row: " + Arrays.toString(s.agentRows) + ", Col: " + Arrays.toString(s.agentCols));
            		
                 	
                 	if (greedy) {
                 		frontier = new FrontierBestFirst(new HeuristicGreedy(s));
                 	}
                 	
                 	else if (astar) {
                 		frontier = new FrontierBestFirst(new HeuristicAStar(s));
                 	}
                 	
                 	else if (wastar) {
                 		frontier = new FrontierBestFirst(new HeuristicWeightedAStar(s, 2));
                 	}
                 	
                 	
                 	frontier.add(s);
                     HashSet<State> explored = new HashSet<>();
                 	
                     while (true) {

                         //Print a status message every 10000 iteration
                         if (++iterations % 10000 == 0) {
                             printSearchStatus(explored, frontier);
                         }

                         if(frontier.isEmpty()) {
                             printSearchStatus(explored, frontier);
                             return null;
                         }
                         
                         System.err.println(frontier.size());

                         s = frontier.pop();
                         if(s.isGoalState()){
                        	System.err.println("SUBGOAL FOUND");
                        	for (int i=0; i<subgoal.length; i++) {
                    			System.err.println(Arrays.toString(s.goals[i]));
                    		}
                          	for (int i=0; i<subgoal.length; i++) {
                     			System.err.println(Arrays.toString(s.boxes[i]));
                     		}
                        	 
                        	 
                             for (int row=1; row<s.goals.length-1; row++) {
                             	for (int col=1; col<s.goals[row].length-1; col++) {
                             		
                             		char goal = s.goals[row][col];
                             		if ('A' <= goal && goal <= 'Z') {
                             			s.boxes[row][col] = 0;
                             			s.walls[row][col] = true;
                             		}
                             	}
                             }
                             
                             Action[][] subactions =  s.extractPlan();
                             for (Action[] subaction : subactions) {
                            	System.err.println(Arrays.toString(subaction));
                             	actions.addLast(subaction);
                             }
                             aRows = s.getSingleAgentRow(agent);
                         	 aCols = s.getSingleAgentCol(agent);
                         	 aBoxes = s.getSingleAgentBoxes(agent);
                         	 
                         	 while (!frontier.isEmpty()) {
                         		 frontier.pop();
                         	 }
                            
                         	first_subgoal = false;
                         	System.err.println();
                             break;
                         }
                         
                         
                         
                         explored.add(s);



                         ArrayList<State> States = s.getExpandedStates();
                         for(int i = 0; i < States.size(); ++i){
                             State expanded = States.get(i);
                             if(!explored.contains(expanded) && !frontier.contains(expanded)) {
                                 frontier.add(expanded);
                             }
                         }
                     }
                 }
            	 
            	 planner.addPlan(actions.toArray(new Action[0][0]));
            	 System.err.println(Arrays.toString(planner.getJointAction(1)));
            	 
            	 s = new State(initialState.agentRows, initialState.agentCols, State.agentColors,
                 		initialState.walls, initialState.boxes, State.boxColors, initialState.goals);
            }
            System.err.println("DONE");
            return actions.toArray(new Action[0][0]);
        }
    }
    

    private static long startTime = System.nanoTime();

    private static void printSearchStatus(HashSet<State> explored, Frontier frontier)
    {
        String statusTemplate = "#Expanded: %,8d, #Frontier: %,8d, #Generated: %,8d, Time: %3.3f s\n%s\n";
        double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000d;
        System.err.format(statusTemplate, explored.size(), frontier.size(), explored.size() + frontier.size(),
                          elapsedTime, Memory.stringRep());
    }
}
