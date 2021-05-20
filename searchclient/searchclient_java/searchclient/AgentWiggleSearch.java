package searchclient;

import java.util.*;

import static searchclient.CentralPlanner.copyState;

public class AgentWiggleSearch {
    public static Action[][] search(State initialState, FrontierBestFirst frontier,
                                    int wiggledAgent) {
        return search(initialState, frontier, wiggledAgent, null);
    }

    public static Action[][] search(State initialState, FrontierBestFirst frontier,
                                    int wiggledAgent, Integer culprit) {

        int iterations = 0;

        System.err.printf("Starting wiggle search for agent %d\n", wiggledAgent);

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

            // Reachability check
            List<Integer> currentBlockers = new ArrayList<>();
            if (culprit != null) {
                List<Character> blockedAgents = checkAgentBlocks(s, culprit);
                if (blockedAgents.stream().anyMatch(c -> c.equals((char) ('0' + wiggledAgent)))) {
                    currentBlockers.add(culprit);
                }
            } else {
                for (int i = 0; i < s.agentRows.length; i++) {
                    List<Character> blockedAgents = checkAgentBlocks(s, i);
                    if (blockedAgents.stream().anyMatch(c -> c.equals((char) ('0' + wiggledAgent)))) {
                        currentBlockers.add(i);
                    }
                }
            }

            boolean isOnGoal = false;
            if (culprit != null) {
                //Agent row, col, goal letter, goal color, agent color
                int agentRow = s.agentRows[culprit];
                int agentCol = s.agentCols[culprit];
                char goalLetter = s.goals[agentRow][agentCol];
                if (goalLetter == 0)
                    continue;
                Color goalColor = s.boxColors[goalLetter - 'A'];
                Color agentColor = s.agentColors[culprit];
                if (!agentColor.equals(goalColor)) {
                    isOnGoal = true;
                }
            } else {
                for (int agent = 0; agent < s.agentRows.length; agent++) {
                    if (agent == wiggledAgent)
                        continue;
                    //Agent row, col, goal letter, goal color, agent color
                    int agentRow = s.agentRows[agent - '0'];
                    int agentCol = s.agentCols[agent - '0'];
                    char goalLetter = s.goals[agentRow][agentCol];
                    if (goalLetter == 0)
                        continue;
                    Color goalColor = s.boxColors[goalLetter - 'A'];
                    Color agentColor = s.agentColors[agent - '0'];
                    if (!agentColor.equals(goalColor)) {
                        isOnGoal = true;
                        break;
                    }
                }
            }

            if (currentBlockers.isEmpty() && !isOnGoal) {
                printSearchStatus(explored, frontier);
                return s.extractPlan();
            }

            explored.add(s);

            ArrayList<State> States = s.getExpandedStates(currentBlockers);
            for (State expanded : States) {
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

    public static List<Character> checkAgentBlocks(State currentState, int agent) {

        List<Character> result = new ArrayList<>();
        int agentRow = currentState.agentRows[agent];
        int agentCol = currentState.agentCols[agent];

        State testState = copyState(currentState);

        testState.walls[agentRow][agentCol] = true;
        testState.agentRows[agent] = 0;
        testState.agentCols[agent] = 0;

        boolean[] wallNeighbours = new boolean[4];
        wallNeighbours[0] = testState.walls[agentRow - 1][agentCol];
        wallNeighbours[1] = testState.walls[agentRow + 1][agentCol];
        wallNeighbours[2] = testState.walls[agentRow][agentCol - 1];
        wallNeighbours[3] = testState.walls[agentRow][agentCol + 1];

        LinkedList<int[]> agentNeighbours = new LinkedList<int[]>();
        for (int i = 0; i < wallNeighbours.length; i++) {
            if (!wallNeighbours[i]) {
                if (i == 0) {
                    agentNeighbours.add(new int[]{agentRow - 1, agentCol});
                } else if (i == 1) {
                    agentNeighbours.add(new int[]{agentRow + 1, agentCol});
                } else if (i == 2) {
                    agentNeighbours.add(new int[]{agentRow, agentCol - 1});
                } else {
                    agentNeighbours.add(new int[]{agentRow, agentCol + 1});
                }
            }
        }

        int[] neighbourCoordinate = agentNeighbours.poll();
        if (neighbourCoordinate == null) {
            throw new IndexOutOfBoundsException("Agent has no neighbours.");
        }
        int[][] region = testState.getdistance(neighbourCoordinate[0], neighbourCoordinate[1]);
        for (int i = 0; i < region.length; i++) {
            for (int j = 0; j < region[i].length; j++) {
                if (region[i][j] != 0) {
                    region[i][j] = 1;
                }
            }
        }
        System.err.println(Arrays.toString(neighbourCoordinate));

        int currRegion = 2;
        for (int[] agentNeighbour : agentNeighbours) {
            neighbourCoordinate = agentNeighbour;
            if (region[neighbourCoordinate[0]][neighbourCoordinate[1]] == 0) {
                System.err.println(Arrays.toString(neighbourCoordinate));
                int[][] region_temp = testState.getdistance(neighbourCoordinate[0], neighbourCoordinate[1]);
                for (int j = 0; j < region_temp.length; j++) {
                    for (int k = 0; k < region_temp[j].length; k++) {
                        if (region_temp[j][k] != 0) {
                            region[j][k] = currRegion;
                        }
                    }
                }
            }
            currRegion += 1;
        }

        currRegion -= 1;

        LinkedList<Character>[] agentsPerRegion = new LinkedList[currRegion];
        LinkedList<Character>[] boxesPerRegion = new LinkedList[currRegion];
        LinkedList<Character>[] goalsPerRegion = new LinkedList[currRegion];
        for (int i = 0; i < currRegion; i++) {
            agentsPerRegion[i] = new LinkedList<>();
            boxesPerRegion[i] = new LinkedList<>();
            goalsPerRegion[i] = new LinkedList<>();
        }

        //Adding boxes and goals in region
        for (int i = 0; i < region.length; i++) {
            for (int j = 0; j < region[i].length; j++) {
                int thisRegion = region[i][j];
                if (thisRegion != 0) {
                    if (testState.boxes[i][j] != 0) {
                        boxesPerRegion[thisRegion - 1].add(testState.boxes[i][j]);
                    } else if (testState.goals[i][j] != 0) {
                        goalsPerRegion[thisRegion - 1].add(testState.goals[i][j]);
                    }
                }
            }
        }

        //Adding agents in region
        for (int i = 0; i < testState.agentRows.length; i++) {
            int agent_row = testState.agentRows[i];
            int agent_col = testState.agentCols[i];

            int agentregion = region[agent_row][agent_col];

            if (agentregion != 0) {
                agentsPerRegion[agentregion - 1].add((char) (i + '0'));
            }
        }


        for (int i = 0; i < agentsPerRegion.length; i++) {
            for (char agentFromRegion : agentsPerRegion[i]) {
                // Check if agent has a box to handle
                // Agent has box check
                Color agentColor = testState.agentColors[agentFromRegion - '0'];
                List<Character> boxChars = new ArrayList<>();
                boolean boxFound = false;
                for (int j = 0; j < boxesPerRegion[i].size(); j++) {
                    if (testState.boxColors[boxesPerRegion[i].get(j) - 'A'].equals(agentColor)) {
                        boxFound = true;
                        boxChars.add(boxesPerRegion[i].get(j));
                    }
                }
                if (!boxFound) {
                    result.add(agentFromRegion);
                    continue;
                }
                // Box has goal check
                boolean goalFound;
                for (int j = 0; j < boxChars.size(); j++) {
                    goalFound = false;
                    for (int k = 0; k < boxesPerRegion[i].size(); k++) {
                        if (boxChars.get(j).equals(boxesPerRegion[i].get(k))) {
                            goalFound = true;
                            break;
                        }
                    }
                    if (!goalFound) {
                        result.add(agentFromRegion);
                        continue;
                    }
                }
            }
        }

        return result;
    }

}