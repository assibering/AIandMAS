package searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            for(int i = 0; i < walls.length; i++) {
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
            throws IOException, InterruptedException, Exception {
        // Use stderr to print to the console.
        System.err.println("SearchClient initializing. I am sending this using the error output stream.");

        // Send client name to server.
        System.out.println("SearchClient");

        // We can also print comments to stdout by prefixing with a #.
        System.out.println("#This is a comment.");

        // Parse the level.
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.US_ASCII));

        // State initialState = SearchClient.parseLevel(serverMessages);
        List<List<State>> initialSubgoalsState = SearchClient.parseLevelSubgoals(serverMessages);
        // Select search strategy.
        Frontier frontier;
        // Search for a plan.
        List<List<Action[]>> plans = new ArrayList<>();
        State initialState = initialSubgoalsState.get(10).get(0);
        Heuristic heuristic = new HeuristicAStar(initialState);
        initialSubgoalsState = initialSubgoalsState.subList(0, 10)
                .stream()
                .filter(list -> list.size() > 0)
                .collect(Collectors.toList());
        initialSubgoalsState.forEach(states -> states.sort(heuristic));
        initialSubgoalsState.forEach(states -> plans.add(new ArrayList<>()));
        initialSubgoalsState.sort((s1, s2) -> heuristic.compare(s1.get(0), s2.get(0)));
        State lastState = initialSubgoalsState.get(0).get(0);
        char[][] remainingBoxes = lastState.boxes;

        for (List<State> agentStates : initialSubgoalsState) {
            System.out.println("#Number of subgoals: " + agentStates.size());
            for (State s : agentStates) {
                if (remainingBoxes == null) {
                    remainingBoxes = s.boxes;
                }
                lastState = new State(lastState.agentRows, lastState.agentCols, State.agentColors,
                        lastState.walls, remainingBoxes, State.boxColors, s.goals, s.currentAgent);
                int boxesCount = 0;
                for (int i = 0; i < lastState.boxes.length; i++) {
                    for (int j = 0; j < lastState.boxes[i].length; j++) {
                        if (lastState.boxes[i][j] != 0) {
                            System.out.println("#Existing box: " + lastState.boxes[i][j]);
                            boxesCount++;
                        }
                    }
                }
                System.out.println("#Boxes left: " + boxesCount);
                for (int i = 0; i < lastState.goals.length; i++) {
                    lastState.goals[i] = Arrays.copyOf(s.goals[i], s.goals[i].length);
                }
                for (int i = 0; i < lastState.goals.length; i++) {
                    for (int j = 0; j < lastState.goals[i].length; j++) {
                        if (lastState.goals[i][j] != 0) {
                            System.out.println("#Current goal: " + lastState.goals[i][j]);
                        }
                    }
                }

                if (args.length > 0) {
                    switch (args[0].toLowerCase(Locale.ROOT)) {
                        case "-bfs":
                            frontier = new FrontierBFS();
                            break;
                        case "-dfs":
                            frontier = new FrontierDFS();
                            break;
                        case "-astar":
                            frontier = new FrontierBestFirst(new HeuristicAStar(lastState));
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
                            frontier = new FrontierBestFirst(new HeuristicWeightedAStar(lastState, w));
                            break;
                        case "-greedy":
                            frontier = new FrontierBestFirst(new HeuristicGreedy(lastState));
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
                try {
                    lastState = SearchClient.searchForLastState(lastState, frontier);
                } catch (OutOfMemoryError ex) {
                    System.err.println("Maximum memory usage exceeded.");
                    lastState = null;
                }

                // Print plan to server.
                if (lastState == null || lastState.extractPlan() == null) {
                    System.err.println("Unable to solve level.");
                    System.exit(0);
                } else {
                    System.err.format("Found solution of length %,d.\n", lastState.extractPlan().length);

                    plans.get(lastState.currentAgent).addAll(Arrays.asList(lastState.extractPlan()));
                }

                //Preparing for next subgoal
                //TODO: lead to better resolution of that issue than "oh, it's just a wall"
                for (int i = 0; i < lastState.goals.length; i++) {
                    for (int j = 0; j < lastState.goals[i].length; j++) {
                        if (lastState.goals[i][j] != 0) {
                            lastState.walls[i][j] = true;
                            lastState.boxes[i][j] = 0;
                        }
                    }
                }
                if (boxesCount > 1) {
                    remainingBoxes = lastState.boxes;
                } else {
                    remainingBoxes = null;
                }
            }
        }

        //Take pairs of actions
        //Check if they are not conflicting
        //If not, apply actions and move to next pair
        //Otherwise try to NoOp some operations until conflict is resolved
        //TODO: consider how to avoid agents being stuck on goal spots of other agents
        int[] counters = new int[plans.size()];
        List<Action[]> joinedPlan = new ArrayList<>();
        while (true) {
            Action[] jointAction = new Action[plans.size()];
            int maxxedCounters = 0;
            for (int i = 0; i < plans.size(); i++) {
                if (counters[i] >= plans.get(i).size()) {
                    jointAction[i] = Action.NoOp;
                    maxxedCounters++;
                } else {
                    jointAction[i] = plans.get(i).get(counters[i])[i];
                    counters[i] = counters[i] + 1;
                }
            }
            if(maxxedCounters == plans.size()) {
                break;
            }
            for(int i = 0; i < plans.size(); i++) {
                if(!initialState.isApplicable(i, jointAction[i], true)) {
                    System.out.printf("#Action %s for agent: %d in position [%d,%d] is not applicable%n",
                            jointAction[i], i, initialState.agentRows[i], initialState.agentCols[i]);
                    jointAction[i] = Action.NoOp;
                    counters[i] = counters[i] - 1;
                }
            }
            ConflictResult conflictCheck = initialState.isConflicting(jointAction);

            if (!conflictCheck.isConflict()) {
                //Thread.sleep(1000);
                System.out.println("#Action to be added: " + Arrays.toString(jointAction));
                System.out.println("#Current step: " + initialState.g());
                initialState = new State(initialState, jointAction);
                joinedPlan.add(jointAction);
                if(joinedPlan.size() > 300)
                    break;
            } else {
                //Select the culprit at random
                int randomResult = new Random().nextInt();
                int noOpAgent = randomResult == 0 ? conflictCheck.agent1 : conflictCheck.agent2;
                int continuingAgent = randomResult == 1 ? conflictCheck.agent1 : conflictCheck.agent2;
                System.out.println("#Agent to blame: " + noOpAgent);

                //Roll back all counters
                for(int i = 0; i < plans.size(); i++) {
                    counters[i] = counters[i] - 1;
                }
                //Add NoOp at current point
                Action[] noOpAction = new Action[plans.size()];
                Arrays.fill(noOpAction, Action.NoOp);
                if(plans.get(noOpAgent).get(counters[noOpAgent])[noOpAgent] != Action.NoOp)
                    plans.get(noOpAgent).add(counters[noOpAgent], noOpAction);
                System.out.println("#Next three actions for blamed agent: " +
                        "[" + plans.get(noOpAgent).get(counters[noOpAgent])[noOpAgent] + ", " +
                        plans.get(noOpAgent).get(counters[noOpAgent]+1)[noOpAgent] + ", " +
                        plans.get(noOpAgent).get(counters[noOpAgent]+2)[noOpAgent] + "]"
                );
                System.out.println("#Next three actions for continuing agent: " +
                        "[" + plans.get(continuingAgent).get(counters[continuingAgent])[continuingAgent] + ", " +
                        plans.get(continuingAgent).get(counters[continuingAgent]+1)[continuingAgent] + ", " +
                        plans.get(continuingAgent).get(counters[continuingAgent]+2)[continuingAgent] + "]"
                );
            }
        }
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
}
