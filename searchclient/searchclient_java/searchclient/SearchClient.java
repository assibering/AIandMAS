package searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class SearchClient {
    public static State parseLevel(BufferedReader serverMessages)
            throws IOException {
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
        char[][] boxes = new char[numRows][numCols];
        for (int row = 0; row < numRows; ++row) {
            line = levelLines.get(row);
            for (int col = 0; col < line.length(); ++col) {
                char c = line.charAt(col);

                if ('0' <= c && c <= '9') {
                    agentRows[c - '0'] = row;
                    agentCols[c - '0'] = col;
                    ++numAgents;
                } else if ('A' <= c && c <= 'Z') {
                    boxes[row][col] = c;
                } else if (c == '+') {
                    walls[row][col] = true;
                }
            }
        }
        agentRows = Arrays.copyOf(agentRows, numAgents);
        agentCols = Arrays.copyOf(agentCols, numAgents);

        // Read goal state
        // line is currently "#goal"
        char[][] goals = new char[numRows][numCols];
        line = serverMessages.readLine();
        int row = 0;
        while (!line.startsWith("#")) {
            for (int col = 0; col < line.length(); ++col) {
                char c = line.charAt(col);

                if (('0' <= c && c <= '9') || ('A' <= c && c <= 'Z')) {
                    goals[row][col] = c;
                }
            }

            ++row;
            line = serverMessages.readLine();
        }

        // End
        // line is currently "#end"

        return new State(agentRows, agentCols, agentColors, walls, boxes, boxColors, goals, 0);
    }

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
                    if(('A' <= c)) {
                        Color agentColour = Arrays.stream(agentColors)
                                .filter(colour -> colour.equals(boxColors[c - 'A']))
                                .findFirst().get();
                        int agentIndex = Arrays.asList(agentColors).indexOf(agentColour);
                        agentResult.get(agentIndex)
                                .add(new State(agentRows, agentCols, agentColors, walls, boxes[c - 'A'], boxColors, goals, agentIndex));
                    } else {{
                        agentResult.get(c - '0')
                            .add(new State(agentRows, agentCols, agentColors, walls, new char[numRows][numCols], boxColors, goals, c - '0'));
                    }}
                }
            }
            agentResult.get(10).add(new State(agentRows, agentCols, agentColors, walls, globalBoxes, boxColors, globalGoals, 0));

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
            throws IOException {
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
        List<Action[]> plan = new ArrayList<>();
        Heuristic heuristic = new HeuristicAStar(initialSubgoalsState.get(10).get(0));
        initialSubgoalsState = initialSubgoalsState.subList(0, 10)
                .stream()
                .filter(list -> list.size() > 0)
                .collect(Collectors.toList());
        initialSubgoalsState.forEach(states -> states.sort(heuristic));
        initialSubgoalsState.sort((s1, s2) -> heuristic.compare(s1.get(0), s2.get(0)));
        State lastState = initialSubgoalsState.get(0).get(0);
        char[][] remainingBoxes = lastState.boxes;

        for(List<State> agentStates: initialSubgoalsState) {
            System.out.println("#Number of subgoals: " + agentStates.size());
            for (State s : agentStates) {
                if(remainingBoxes == null) {
                    remainingBoxes = s.boxes;
                }
                lastState = new State(lastState.agentRows, lastState.agentCols, State.agentColors,
                        State.walls, remainingBoxes, State.boxColors, s.goals, s.currentAgent);
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

                    plan.addAll(Arrays.asList(lastState.extractPlan()));
                }

                //Preparing for next subgoal
                //TODO: lead to better resolution of that issue than "oh, it's just a wall"
                for (int i = 0; i < lastState.goals.length; i++) {
                    for (int j = 0; j < lastState.goals[i].length; j++) {
                        if (lastState.goals[i][j] != 0) {
                            State.walls[i][j] = true;
                            lastState.boxes[i][j] = 0;
                        }
                    }
                }
                if(boxesCount > 1) {
                    remainingBoxes = lastState.boxes;
                } else {
                    remainingBoxes = null;
                }
            }
        }
        for (Action[] jointAction : plan) {
            System.out.println("#Action to be taken: " + Arrays.toString(jointAction));
        }
        for (Action[] jointAction : plan) {
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
