package searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class SearchClient {

    public static List<List<State>> parseLevelSubgoals(BufferedReader serverMessages)
            throws IOException {
        List<List<State>> agentResult = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            agentResult.add(new ArrayList<>());
        }
        // We can assume that the level file is conforming to specification, since the server verifies this.
        // Read domain
        serverMessages.readLine(); // #domain
        serverMessages.readLine(); // hospital

        // Read Level name
        serverMessages.readLine(); // #levelname
        serverMessages.readLine(); // <name>

        // Read colors
        serverMessages.readLine(); // #colors
        Color[] agentColors = new Color[10];
        Color[] boxColors = new Color[26];
        String line = serverMessages.readLine();
        while (!line.startsWith("#")) {
            String[] split = line.split(":");
            Color color = Color.fromString(split[0].strip());
            String[] entities = split[1].split(",");
            for (String entity : entities) {
                char c = entity.strip().charAt(0);
                if ('0' <= c && c <= '9') {
                    agentColors[c - '0'] = color;
                } else if ('A' <= c && c <= 'Z') {
                    boxColors[c - 'A'] = color;
                }
            }
            line = serverMessages.readLine();
        }

        // Read initial state
        // line is currently "#initial"
        int numRows = 0;
        int numCols = 0;
        ArrayList<String> levelLines = new ArrayList<>(64);
        line = serverMessages.readLine();
        while (!line.startsWith("#")) {
            levelLines.add(line);
            numCols = Math.max(numCols, line.length());
            ++numRows;
            line = serverMessages.readLine();
        }
        int numAgents = 0;
        int[] agentRows = new int[10];
        int[] agentCols = new int[10];
        boolean[][] walls = new boolean[numRows][numCols];
        char[][][] boxes = new char[26][numRows][numCols];
        char[][] globalBoxes = new char[numRows][numCols];
        for (int row = 0; row < numRows; ++row) {
            line = levelLines.get(row);
            for (int col = 0; col < line.length(); ++col) {
                char c = line.charAt(col);

                if ('0' <= c && c <= '9') {
                    agentRows[c - '0'] = row;
                    agentCols[c - '0'] = col;
                    ++numAgents;
                } else if ('A' <= c && c <= 'Z') {
                    boxes[c - 'A'][row][col] = c;
                    globalBoxes[row][col] = c;
                } else if (c == '+') {
                    walls[row][col] = true;
                }
            }
        }
        agentRows = Arrays.copyOf(agentRows, numAgents);
        agentCols = Arrays.copyOf(agentCols, numAgents);

        // Read goal state
        // line is currently "#goal"
        char[][] globalGoals = new char[numRows][numCols];
        line = serverMessages.readLine();
        int row = 0;
        while (!line.startsWith("#")) {
            for (int col = 0; col < line.length(); ++col) {
                char c = line.charAt(col);

                if (('0' <= c && c <= '9') || ('A' <= c && c <= 'Z')) {
                    char[][] goals = new char[numRows][numCols];
                    goals[row][col] = c;
                    globalGoals[row][col] = c;
                    //result.add(new State(agentRows, agentCols, agentColors, walls, boxes, boxColors, goals));
                    if (('A' <= c)) {
                        Color agentColour = Arrays.stream(agentColors)
                                .filter(colour -> colour.equals(boxColors[c - 'A']))
                                .findFirst().get();
                        int agentIndex = Arrays.asList(agentColors).indexOf(agentColour);
                        agentResult.get(agentIndex)
                                .add(new State(agentRows, agentCols, agentColors, walls, boxes[c - 'A'], boxColors, goals, agentIndex));
                    } else {
                        {
                            agentResult.get(c - '0')
                                    .add(new State(agentRows, agentCols, agentColors, walls, new char[numRows][numCols], boxColors, goals, c - '0'));
                        }
                    }
                }
            }
            boolean[][] wallsCopy = new boolean[walls.length][];
            for (int i = 0; i < walls.length; i++) {
                wallsCopy[i] = Arrays.copyOf(walls[i], walls[0].length);
            }
            agentResult.get(10).add(new State(agentRows, agentCols, agentColors, wallsCopy, globalBoxes, boxColors, globalGoals, 0));

            ++row;
            line = serverMessages.readLine();
        }

        // End
        // line is currently "#end"

        return agentResult;
    }

    public static State searchForLastState(State initialState, Frontier frontier) {
        System.err.format("Starting %s.\n", frontier.getName());

        return GraphSearch.search(initialState, frontier);
    }

    public static void main(String[] args)
            throws Exception {
        // Use stderr to print to the console.
        System.err.println("SearchClient initializing. I am sending this using the error output stream.");

        // Send client name to server.
        System.out.println("SearchClient");

        // We can also print comments to stdout by prefixing with a #.
        System.out.println("#This is a comment.");

        // Parse the level.
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.US_ASCII));

        List<List<State>> initialSubgoalsState = SearchClient.parseLevelSubgoals(serverMessages);

        // This is the whole state
        State initialState = initialSubgoalsState.get(10).get(0);
        // We ignore agents with no goals (most likely: agents that do not exist)
        initialSubgoalsState = initialSubgoalsState.subList(0, 10)
                .stream()
                .filter(list -> list.size() > 0)
                .collect(Collectors.toList());
        // We sort all goals by their heuristic
        Heuristic heuristic = new HeuristicAStar(initialState);
        Heuristic finalHeuristic = heuristic;
        initialSubgoalsState.forEach(states -> states.sort(finalHeuristic));
        //initialSubgoalsState.sort((s1, s2) -> finalHeuristic.compare(s1.get(0), s2.get(0)));
        // Prepare plans list
        List<LinkedList<Action[]>> plans = new ArrayList<>();
        initialSubgoalsState.forEach(states -> plans.add(new LinkedList<>()));
        // We add empty state to start of subgoal list, so that when needed we can reach it in case we need to reschedule DONE
        initialSubgoalsState.forEach(states -> states.add(1, states.get(0)));

        // Generate step list for first subgoal
        for (int i = 0; i < initialSubgoalsState.size(); i++) {
            buildActionsForFirstSubstate(args, plans, initialState, initialSubgoalsState, i);
        }

        //TODO: consider how to avoid agents being stuck on goal spots of other agents
        List<Action[]> joinedPlan = new ArrayList<>();
        while (true) {
            //Stop working if all subgoals have been considered
            if (initialSubgoalsState.stream().allMatch(subgoals -> subgoals.size() <= 1) &&
                    plans.stream().allMatch(plan -> plan.size() == 0)) {
                break;
            }

            Action[] jointAction = new Action[plans.size()];
            for (int i = 0; i < plans.size(); i++) {
                System.out.printf("#Index: %d, remaining subgoals size: %d, remaining plans size: %d\n",
                        i, initialSubgoalsState.get(i).size(), plans.get(i).size());
                if (plans.get(i).isEmpty()) {
                    jointAction[i] = Action.NoOp;
                    //If we have no more actions left, we plan for new goal
                    //If no plan has been reached (e.g., could not find a solution), we move on
                    //As next iteration will calculate for another goal or with different state
                    if (initialSubgoalsState.get(i).size() > 1) {
                        System.out.printf("#Doing replanning on agent %d\n", i);
                        buildActionsForFirstSubstate(args, plans, initialState, initialSubgoalsState, i);
                        if (!plans.get(i).isEmpty())
                            jointAction[i] = plans.get(i).poll()[i];
                        else {
                            Action[] noOpAction = new Action[plans.size()];
                            Arrays.fill(noOpAction, Action.NoOp);
                            for (int j = 0; j < 10; j++) {
                                plans.get(i).add(noOpAction);
                            }
                        }
                    }
                } else {
                    // We pull the next action
                    jointAction[i] = plans.get(i).poll()[i];
                }
            }

            //DEBUG ONLY: stop working if plan above 1000 iterations
            if (initialState.g() > 1000)
                break;
            //Check every action for being applicable
            for (int i = 0; i < plans.size(); i++) {
                //If action is no longer applicable, then replan or NoOp
                if (!initialState.isApplicable(i, jointAction[i], true)) {
                    System.out.printf("#Action %s for agent: %d in position [%d,%d] is not applicable%n",
                            jointAction[i], i, initialState.agentRows[i], initialState.agentCols[i]);
                    //Reschedule NoOping agent to current goal or NoOp
                    boolean shouldReschedule = new Random().nextBoolean();
                    if (shouldReschedule) {
                        heuristic = new HeuristicAStar(initialState);
                        var newSubgoals = initialSubgoalsState.get(i).subList(1, initialSubgoalsState.get(i).size());
                        newSubgoals.sort(heuristic);
                        newSubgoals.add(0, newSubgoals.get(0));
                        initialSubgoalsState.set(i, newSubgoals);
                        buildActionsForFirstSubstate(args, plans, initialState, initialSubgoalsState, i);
                        jointAction[i] = Action.NoOp;
                        if (!plans.get(i).isEmpty())
                            jointAction[i] = plans.get(i).poll()[i];
                    } else {
                        Action[] previousAction = new Action[plans.size()];
                        Arrays.fill(previousAction, Action.NoOp);
                        previousAction[i] = jointAction[i];
                        plans.get(i).add(0, previousAction);
                        //NoOp in order to let another agent through
                        jointAction[i] = Action.NoOp;
                    }
                }
            }
            //Check if any conflict exists now and resolve it with a NoOp
            ConflictResult conflictCheck = initialState.isConflicting(jointAction);
            while (conflictCheck.isConflict()) {
                //Select the culprit at random
                boolean randomResult = new Random().nextBoolean();
                int noOpAgent = randomResult ? conflictCheck.agent1 : conflictCheck.agent2;
                System.out.println("#Agent to blame: " + noOpAgent);
                //Retain action to be taken
                Action[] previousAction = new Action[plans.size()];
                Arrays.fill(previousAction, Action.NoOp);
                previousAction[noOpAgent] = jointAction[noOpAgent];
                plans.get(noOpAgent).add(0, previousAction);
                //NoOp in order to let another agent through
                jointAction[noOpAgent] = Action.NoOp;
                //Check if any conflict is left
                conflictCheck = initialState.isConflicting(jointAction);
            }
            // As all conflicts are resolved, we make a joint action
            System.out.println("#Action to be added: " + Arrays.toString(jointAction));
            System.out.println("#Current step: " + initialState.g());
            initialState = new State(initialState, jointAction);
            joinedPlan.add(jointAction);
        }
        //Send list of actions to server
        for (Action[] jointAction : joinedPlan) {
            System.out.println("#Action to be taken: " + Arrays.toString(jointAction));
        }
        for (Action[] jointAction : joinedPlan) {
            System.out.print(jointAction[0].name);
            for (int action = 1; action < jointAction.length; ++action) {
                System.out.print("|");
                System.out.print(jointAction[action].name);
            }
            System.out.println();
            // We must read the server's response to not fill up the stdin buffer and block the server.
            serverMessages.readLine();
        }
    }

    private static void buildActionsForFirstSubstate(String[] args, List<LinkedList<Action[]>> plans,
                                                     State originalState, List<List<State>> subgoals,
                                                     int selectedAgent) {
        Frontier frontier;
        if (subgoals.get(selectedAgent).size() < 2) {
            return;
        }

        //Grab first existing state
        State s = subgoals.get(selectedAgent).get(1);
        //This moves current goal to fallback position, in case we need to reconsider it again
        subgoals.get(selectedAgent).remove(0);
        //We create a new state, with current state's position of agents
        //And out-of-scope boxes and reached goals as walls
        //And filtered goals as not to overlap with walls
        State usedState = new State(originalState.agentRows, originalState.agentCols, State.agentColors,
                includeBoxesAsWalls(s.boxes, originalState.boxes, originalState.goals, originalState.walls),
                filter(s.boxes, originalState.boxes, originalState.goals),
                State.boxColors, removeReachedGoals(s.goals, originalState.boxes), s.currentAgent);
        int boxesCount = 0;
        for (int i = 0; i < usedState.boxes.length; i++) {
            for (int j = 0; j < usedState.boxes[i].length; j++) {
                if (usedState.boxes[i][j] != 0) {
                    System.out.println("#Existing box: " + usedState.boxes[i][j]);
                    boxesCount++;
                }
            }
        }
        System.out.println("#Boxes left: " + boxesCount);
        for (int i = 0; i < usedState.goals.length; i++) {
            usedState.goals[i] = Arrays.copyOf(s.goals[i], s.goals[i].length);
        }
        for (int i = 0; i < usedState.goals.length; i++) {
            for (int j = 0; j < usedState.goals[i].length; j++) {
                if (usedState.goals[i][j] != 0) {
                    System.out.println("#Current goal: " + usedState.goals[i][j]);
                }
            }
        }

        frontier = createFrontier(args, usedState);
        try {
            usedState = SearchClient.searchForLastState(usedState, frontier);
        } catch (OutOfMemoryError ex) {
            System.err.println("Maximum memory usage exceeded.");
            usedState = null;
        }

        // Print plan to server.
        if (usedState == null) {
            System.err.println("Unable to solve level, moving to the back of the list.");
            //TODO: add state to back of subgoal list DONE
            subgoals.get(selectedAgent).add(s);
        } else {
            System.err.format("Found solution of length %,d.\n", usedState.extractPlan().length);
            //TODO: set to new list rather than add to current one DONE
            plans.set(usedState.currentAgent, new LinkedList<>(Arrays.asList(usedState.extractPlan())));
        }
    }

    private static Frontier createFrontier(String[] args, State usedState) {
        Frontier frontier;
        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "-bfs":
                    frontier = new FrontierBFS();
                    break;
                case "-dfs":
                    frontier = new FrontierDFS();
                    break;
                case "-astar":
                    frontier = new FrontierBestFirst(new HeuristicAStar(usedState));
                    break;
                case "-wastar":
                    int w = 5;
                    if (args.length > 1) {
                        try {
                            w = Integer.parseUnsignedInt(args[1]);
                        } catch (NumberFormatException e) {
                            System.err.println("Couldn't parse weight argument to -wastar as integer, using default.");
                        }
                    }
                    frontier = new FrontierBestFirst(new HeuristicWeightedAStar(usedState, w));
                    break;
                case "-greedy":
                    frontier = new FrontierBestFirst(new HeuristicGreedy(usedState));
                    break;
                default:
                    frontier = new FrontierBFS();
                    System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or " +
                            "-greedy to set the search strategy.");
            }
        } else {
            frontier = new FrontierBFS();
            System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to " +
                    "set the search strategy.");
        }
        return frontier;
    }

    private static char[][] filter(char[][] subgoalBoxes, char[][] totalStateBoxes, char[][] totalStateGoals) {
        char[][] result = new char[subgoalBoxes.length][subgoalBoxes[0].length];
        char letter = 0;
        for (int i = 0; i < subgoalBoxes.length; i++) {
            for (int j = 0; j < subgoalBoxes[i].length; j++) {
                if (subgoalBoxes[i][j] != 0) {
                    letter = subgoalBoxes[i][j];
                    break;
                }
            }
            if (letter != 0)
                break;
        }
        for (int i = 0; i < totalStateBoxes.length; i++) {
            for (int j = 0; j < totalStateBoxes[i].length; j++) {
                if (letter == totalStateBoxes[i][j] && totalStateGoals[i][j] != totalStateBoxes[i][j]) {
                    result[i][j] = letter;
                } else {
                    result[i][j] = 0;
                }
            }
        }
        return result;
    }

    public static boolean[][] includeBoxesAsWalls(char[][] subgoalBoxes,
                                                  char[][] totalStateBoxes,
                                                  char[][] totalStateGoals,
                                                  boolean[][] totalStateWalls) {
        boolean[][] resultWalls = new boolean[totalStateWalls.length][totalStateWalls[0].length];

        char letter = 0;
        for (int i = 0; i < subgoalBoxes.length; i++) {
            for (int j = 0; j < subgoalBoxes[i].length; j++) {
                if (subgoalBoxes[i][j] != 0) {
                    letter = subgoalBoxes[i][j];
                    break;
                }
            }
            if (letter != 0)
                break;
        }

        for (int i = 0; i < totalStateWalls.length; i++) {
            for (int j = 0; j < totalStateWalls[i].length; j++) {
                //Keep existing walls
                if (totalStateWalls[i][j])
                    resultWalls[i][j] = true;
                    //Treat boxes from other subgoals as walls
                else if (totalStateBoxes[i][j] != 0 && totalStateBoxes[i][j] != letter) {
                    resultWalls[i][j] = true;
                }
                //Treat achieved goals as walls
                else if (totalStateBoxes[i][j] != 0 && totalStateBoxes[i][j] == totalStateGoals[i][j]) {
                    resultWalls[i][j] = true;
                }
            }
        }

        return resultWalls;
    }

    public static char[][] removeReachedGoals(char[][] subgoalGoals,
                                              char[][] totalStateBoxes) {
        char[][] resultGoals = new char[subgoalGoals.length][subgoalGoals[0].length];

        for (int i = 0; i < subgoalGoals.length; i++) {
            for (int j = 0; j < subgoalGoals[i].length; j++) {
                //Keep existing walls
                if (subgoalGoals[i][j] != 0 && totalStateBoxes[i][j] == subgoalGoals[i][j]) {
                    resultGoals[i][j] = subgoalGoals[i][j];
                } else {
                    resultGoals[i][j] = 0;
                }
            }
        }

        return resultGoals;
    }
}
