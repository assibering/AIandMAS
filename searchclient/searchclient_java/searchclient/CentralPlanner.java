package searchclient;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

public class CentralPlanner {

    public LinkedList<Action[][]> individualplans;
    public State initialState;

    public CentralPlanner(State initialState) {
        this.individualplans = new LinkedList<Action[][]>();
        this.initialState = initialState;
    }

    public void addPlan(Action[][] plan) {
        this.individualplans.addLast(plan);
        System.err.println("PLANS: " + this.individualplans.size());
    }

    public Action[] getJointAction(int stepnumber) {
        Action[] jointAction = new Action[this.individualplans.size()];
        for (int i = 0; i < this.individualplans.size(); i++) {
            if (stepnumber < this.individualplans.get(i).length) {
                jointAction[i] = this.individualplans.get(i)[stepnumber][0];
            } else {
                jointAction[i] = Action.NoOp;
            }
        }
        return jointAction;
    }

    public Action[][] getFullPlan() {
        ArrayList<Action[]> result = new ArrayList<>();
        int step = 0;
        while (true) {
            Action[] jointAction = getJointAction(step);
            // If there are no actions to be made, we found the full plan
            if (Arrays.stream(jointAction).allMatch(action -> action == Action.NoOp))
                break;
            result.add(jointAction);
            step++;
        }
        return result.toArray(new Action[0][]);
    }

    public PlanningResult test(State initialState, Action[] jointAction, int step) {
        // If there are no actions to be made, we found the full plan
        if (Arrays.stream(jointAction).allMatch(action -> action == Action.NoOp)) {
            System.err.printf("Found full solution at step %d\n", step);
            return new PlanningResult();
        }
        // If there are actions to be made, we check them for applicability and conflict
        for (int agent = 0; agent < initialState.agentRows.length; agent++) {
            ApplicabilityResult applicabilityResult = initialState.isApplicable(agent, jointAction[agent]);
            if (!applicabilityResult.isApplicable()) {
                System.err.printf("Found applicability issue with action %s by agent %d at step %d due to %s: %c\n",
                        jointAction[agent],
                        agent, step, applicabilityResult.type, applicabilityResult.getCause());
                PlanningResult result = new PlanningResult();
                result.agent = agent;
                result.step = step;
                result.type = PlanningResult.PlanningResultType.WITH_CONFLICT;
                result.cause = applicabilityResult.getCause();
                return result;
            }
        }
        // Check if joint action is conflicting at this point
        ConflictResult conflictResult = initialState.isConflicting(jointAction);
        if (conflictResult.isConflict()) {
            System.err.printf("Found conflict issue with agent %d at step %d\n", conflictResult.agent1, step);
            PlanningResult result = new PlanningResult();
            result.agent = conflictResult.agent1;
            result.step = step;
            result.type = PlanningResult.PlanningResultType.WITH_CONFLICT;
            result.cause = (char) (conflictResult.agent2 + '0');
            return result;
        }

        return null;
    }

    public PlanningResult delve(State initialState, int step) {
        Action[] jointAction = getJointAction(step);
        PlanningResult result = test(initialState, jointAction, step);
        if (result != null) {
            return result;
        }
        return delve(new State(copyState(initialState), jointAction), step + 1);
    }

    //TODO: step-back mechanism for boxes not led by agents
    public PlanningResult plan(State initialState, int step) {
        System.err.printf("Start planning at step %d\n", step);
        //Can we make a step now?
        Action[] jointAction = getJointAction(step);
        PlanningResult result = test(initialState, jointAction, step);
        //If not (or we NoOp), then return
        if (result != null) {
            return result;
        }
        //If no issue now, then go to next step
        PlanningResult nextAction = plan(new State(copyState(initialState), jointAction), step + 1);

        // Issue resolution phase
        PlanningResult resolveAttempt;
        Action[][] temporaryPlan;
        Action[][] originalPlan;
        int agentToBlame;
        // If there is no conflict, we propagate that back
        if (nextAction.type == PlanningResult.PlanningResultType.NO_CONFLICT) {
            System.err.printf("Found full solution at step %d\n", step);
            return nextAction;
        }
        // TODO: move away from box
        // TODO: when to do it? every time
        // TODO: and how do we move out of step? How to check possible directions? Just use test
        // TODO: we could check from which side the box is blocked (meaning on the other side there's an agent)
        // TODO: then, we just pick opposite dimension and figure out if we can go there
        // TODO: waiting time at that spot? depending on step we're in
        else if (nextAction.type == PlanningResult.PlanningResultType.WITH_CONFLICT
                && nextAction.cause >= 'A' && nextAction.cause <= 'Z') {
            // Get action where issue happens
            // Note that if agent is blocked by a box and another agent is blocked by this agent
            // Then they are blocking each other in same dimension
            agentToBlame = nextAction.agent;
            Action[] stepBackActions = generateStepBack(agentToBlame, initialState, step);
            // Create list of steps for moving around
            originalPlan = Arrays.copyOf(this.individualplans.get(agentToBlame),
                    this.individualplans.get(agentToBlame).length);
            temporaryPlan = Arrays.copyOf(originalPlan,
                    originalPlan.length);
            ArrayList<Action[]> temporaryList = new ArrayList<>();
            Collections.addAll(temporaryList, temporaryPlan);
            // TODO: define direction we can go in
            // TODO: define number of NoOps (possibly 3 + reach time for other agent to pick up this box)
            // TODO: get minimum moment for other agent to appear
            for (int i = 0; i < stepBackActions.length; i++) {
                temporaryList.add(step + i, new Action[]{stepBackActions[i]});
            }
            for (int noOps = step + stepBackActions.length; noOps < nextAction.step + stepBackActions.length + 15; noOps++) {
                temporaryList.add(noOps, new Action[]{Action.NoOp});
            }
            for (int i = stepBackActions.length - 1; i > -1; i--) {
                temporaryList.add(nextAction.step + stepBackActions.length + 15, new Action[]{stepBackActions[i].opposite()});
            }
            temporaryPlan = temporaryList.toArray(new Action[0][]);
            this.individualplans.set(agentToBlame, temporaryPlan);
            // TODO check if this works
            resolveAttempt = delve(initialState, step);
        }
        // If we found some conflict, we try to check if delay works at this step
        // TODO: apply delays only when agent (or box pushed by an agent) is involved
        else {
            agentToBlame = nextAction.agent;
            // Create temporary list of actions for delaying
            originalPlan = Arrays.copyOf(this.individualplans.get(agentToBlame),
                    this.individualplans.get(agentToBlame).length);
            temporaryPlan = Arrays.copyOf(originalPlan,
                    originalPlan.length);
            ArrayList<Action[]> temporaryList = new ArrayList<>();
            Collections.addAll(temporaryList, temporaryPlan);
            // TODO how many NoOps should be added there - maybe difference between current step and error step?
            for (int noOps = step; noOps < nextAction.step; noOps++) {
                temporaryList.add(step, new Action[]{Action.NoOp});
            }
            temporaryPlan = temporaryList.toArray(new Action[0][]);
            this.individualplans.set(agentToBlame, temporaryPlan);
            // TODO check if this works
            resolveAttempt = delve(initialState, step);
        }

        // If the same conflict occurs, we need to add more
        // We return who is conflicting and revert changes
        // TODO: how to detect if this given conflict is fully resolved and we don't try to solve another one
        if (resolveAttempt.type == PlanningResult.PlanningResultType.WITH_CONFLICT
                && resolveAttempt.agent == nextAction.agent
                && resolveAttempt.cause == nextAction.cause) {
            // TODO revert to previous plan
            this.individualplans.set(agentToBlame, originalPlan);
            return resolveAttempt;
        }
        // If the conflict occurs not at all or with other agent or it's still a box
        // this means we delayed enough to avoid this problem
        // And we just call next step on this action
        else {
            return plan(new State(copyState(initialState), getJointAction(step)), step + 1);
        }
    }

    static State copyState(State initialState) {
        char[][] initialStateBoxes = new char[initialState.boxes.length][];
        for (int boxRow = 0; boxRow < initialStateBoxes.length; boxRow++) {
            initialStateBoxes[boxRow] =
                    Arrays.copyOf(initialState.boxes[boxRow], initialState.boxes[boxRow].length);
        }
        char[][] initialStateGoals = new char[initialState.goals.length][];
        for (int boxRow = 0; boxRow < initialStateGoals.length; boxRow++) {
            initialStateGoals[boxRow] =
                    Arrays.copyOf(initialState.goals[boxRow], initialState.goals[boxRow].length);
        }
        boolean[][] initialStateWalls = new boolean[initialState.walls.length][];
        for (int boxRow = 0; boxRow < initialStateWalls.length; boxRow++) {
            initialStateWalls[boxRow] =
                    Arrays.copyOf(initialState.walls[boxRow], initialState.walls[boxRow].length);
        }
        int[][] initialStateDistances = new int[initialState.distancegrid.length][];
        for (int boxRow = 0; boxRow < initialStateDistances.length; boxRow++) {
            initialStateDistances[boxRow] =
                    Arrays.copyOf(initialState.distancegrid[boxRow], initialState.distancegrid[boxRow].length);
        }
        return new State(Arrays.copyOf(initialState.agentRows, initialState.agentRows.length),
                Arrays.copyOf(initialState.agentCols, initialState.agentCols.length),
                Arrays.copyOf(initialState.agentColors, initialState.agentColors.length),
                initialStateWalls,
                initialStateBoxes,
                Arrays.copyOf(initialState.boxColors, initialState.boxColors.length),
                initialStateGoals,
                initialStateDistances
        );
    }

    private Action[] generateStepBack(int blockedAgent, State initialState, int step) {
        Action[] currentAction = getJointAction(step);
        Action[] nextAction = getJointAction(step + 1);
        String[] directions, directions1, directions2;
        //We assume this doesn't happen
        switch (currentAction[blockedAgent]) {
            case NoOp:
                return new Action[]{Action.NoOp};
            case MoveN:
            case MoveS:
                currentAction[blockedAgent] = Action.MoveE;
                if (test(initialState, currentAction, step) == null) {
                    return new Action[]{Action.MoveE};
                }
                currentAction[blockedAgent] = Action.MoveW;
                if (test(initialState, currentAction, step) == null) {
                    return new Action[]{Action.MoveW};
                }
                break;
            case MoveE:
            case MoveW:
                currentAction[blockedAgent] = Action.MoveN;
                if (test(initialState, currentAction, step) == null) {
                    return new Action[]{Action.MoveN};
                }
                currentAction[blockedAgent] = Action.MoveS;
                if (test(initialState, currentAction, step) == null) {
                    return new Action[]{Action.MoveS};
                }
                break;
            //First step: pull agent either to E or W, box to S
            //Second step: pull agent in any direction, box to E or W
            case PushNN:
                directions1 = new String[]{"E", "W"};
                directions2 = new String[]{"N", "E", "S", "W"};
                Collections.shuffle(Arrays.asList(directions1));
                Collections.shuffle(Arrays.asList(directions2));
                for (String firstDirection : directions1) {
                    currentAction[blockedAgent] = Action.valueOf("Pull" + firstDirection + "S");
                    PlanningResult testResult = test(initialState, currentAction, step);
                    if (testResult != null && testResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
                        continue;
                    }
                    for (String secondDirection : directions2) {
                        if ((firstDirection + secondDirection).equals("EW") ||
                                (firstDirection + secondDirection).equals("WE")) {
                            continue;
                        }
                        nextAction[blockedAgent] = Action.valueOf("Pull" + secondDirection + firstDirection);
                        if (test(new State(copyState(initialState), currentAction), nextAction, step + 1) == null) {
                            return new Action[]{
                                    Action.valueOf("Pull" + firstDirection + "S"),
                                    Action.valueOf("Pull" + secondDirection + firstDirection)
                            };
                        }
                    }
                }
                break;
            //One step: pull agent to any direction, box to S
            case PushNE:
            case PushNW:
                directions = new String[]{"S", "E", "W"};
                Collections.shuffle(Arrays.asList(directions));
                for (String direction : directions) {
                    currentAction[blockedAgent] = Action.valueOf("Pull" + direction + "S");
                    if (test(initialState, currentAction, step) == null) {
                        return new Action[]{Action.valueOf("Pull" + direction + "S")};
                    }
                }
                break;
            //First step: pull agent either to E or W, box to S
            //Second step: pull agent in any direction, box to E or W
            case PushSS:
                directions1 = new String[]{"E", "W"};
                directions2 = new String[]{"N", "E", "S", "W"};
                Collections.shuffle(Arrays.asList(directions1));
                Collections.shuffle(Arrays.asList(directions2));
                for (String firstDirection : directions1) {
                    currentAction[blockedAgent] = Action.valueOf("Pull" + firstDirection + "N");
                    PlanningResult testResult = test(initialState, currentAction, step);
                    if (testResult != null && testResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
                        continue;
                    }
                    for (String secondDirection : directions2) {
                        if ((firstDirection + secondDirection).equals("EW") ||
                                (firstDirection + secondDirection).equals("WE")) {
                            continue;
                        }
                        nextAction[blockedAgent] = Action.valueOf("Pull" + secondDirection + firstDirection);
                        if (test(new State(copyState(initialState), currentAction), nextAction, step + 1) == null) {
                            return new Action[]{
                                    Action.valueOf("Pull" + firstDirection + "N"),
                                    Action.valueOf("Pull" + secondDirection + firstDirection)
                            };
                        }
                    }
                }
                break;
            //One step: pull agent to any direction, box to N
            case PushSE:
            case PushSW:
                directions = new String[]{"N", "E", "W"};
                Collections.shuffle(Arrays.asList(directions));
                for (String direction : directions) {
                    currentAction[blockedAgent] = Action.valueOf("Pull" + direction + "N");
                    if (test(initialState, currentAction, step) == null) {
                        return new Action[]{Action.valueOf("Pull" + direction + "N")};
                    }
                }
                break;
            //One step: pull agent to any direction, box to W
            case PushEN:
            case PushES:
                directions = new String[]{"S", "N", "W"};
                Collections.shuffle(Arrays.asList(directions));
                for (String direction : directions) {
                    currentAction[blockedAgent] = Action.valueOf("Pull" + direction + "W");
                    if (test(initialState, currentAction, step) == null) {
                        return new Action[]{Action.valueOf("Pull" + direction + "W")};
                    }
                }
                break;
            //First step: pull agent either to E or W, box to S
            //Second step: pull agent in any direction, box to E or W
            case PushEE:
                directions1 = new String[]{"N", "S"};
                directions2 = new String[]{"N", "E", "S", "W"};
                Collections.shuffle(Arrays.asList(directions1));
                Collections.shuffle(Arrays.asList(directions2));
                for (String firstDirection : directions1) {
                    currentAction[blockedAgent] = Action.valueOf("Pull" + firstDirection + "W");
                    PlanningResult testResult = test(initialState, currentAction, step);
                    if (testResult != null && testResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
                        continue;
                    }
                    for (String secondDirection : directions2) {
                        if ((firstDirection + secondDirection).equals("NS") ||
                                (firstDirection + secondDirection).equals("SN")) {
                            continue;
                        }
                        nextAction[blockedAgent] = Action.valueOf("Pull" + secondDirection + firstDirection);
                        if (test(new State(copyState(initialState), currentAction), nextAction, step + 1) == null) {
                            return new Action[]{
                                    Action.valueOf("Pull" + firstDirection + "W"),
                                    Action.valueOf("Pull" + secondDirection + firstDirection)
                            };
                        }
                    }
                }
                break;
            //One step: pull agent to any direction, box to E
            case PushWN:
            case PushWS:
                directions = new String[]{"S", "N", "E"};
                Collections.shuffle(Arrays.asList(directions));
                for (String direction : directions) {
                    currentAction[blockedAgent] = Action.valueOf("Pull" + direction + "E");
                    if (test(initialState, currentAction, step) == null) {
                        return new Action[]{Action.valueOf("Pull" + direction + "E")};
                    }
                }
                break;
            //First step: pull agent either to N or S, box to E
            //Second step: pull agent in any direction, box to N or S
            case PushWW:
                directions1 = new String[]{"N", "S"};
                directions2 = new String[]{"N", "E", "S", "W"};
                Collections.shuffle(Arrays.asList(directions1));
                Collections.shuffle(Arrays.asList(directions2));
                for (String firstDirection : directions1) {
                    currentAction[blockedAgent] = Action.valueOf("Pull" + firstDirection + "E");
                    PlanningResult testResult = test(initialState, currentAction, step);
                    if (testResult != null && testResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
                        continue;
                    }
                    for (String secondDirection : directions2) {
                        if ((firstDirection + secondDirection).equals("NS") ||
                                (firstDirection + secondDirection).equals("SN")) {
                            continue;
                        }
                        nextAction[blockedAgent] = Action.valueOf("Pull" + secondDirection + firstDirection);
                        if (test(new State(copyState(initialState), currentAction), nextAction, step + 1) == null) {
                            return new Action[]{
                                    Action.valueOf("Pull" + firstDirection + "E"),
                                    Action.valueOf("Pull" + secondDirection + firstDirection)
                            };
                        }
                    }
                }
                break;
            //First step: pull box either to E or W, agent to S
            //Second step: pull box in any direction, agent to E or W
            case PullNN:
                directions1 = new String[]{"E", "W"};
                directions2 = new String[]{"N", "E", "S", "W"};
                Collections.shuffle(Arrays.asList(directions1));
                Collections.shuffle(Arrays.asList(directions2));
                for (String firstDirection : directions1) {
                    currentAction[blockedAgent] = Action.valueOf("Push" + "S" + firstDirection);
                    PlanningResult testResult = test(initialState, currentAction, step);
                    if (testResult != null && testResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
                        continue;
                    }
                    for (String secondDirection : directions2) {
                        if ((firstDirection + secondDirection).equals("EW") ||
                                (firstDirection + secondDirection).equals("WE")) {
                            continue;
                        }
                        nextAction[blockedAgent] = Action.valueOf("Push" + firstDirection + secondDirection);
                        if (test(new State(copyState(initialState), currentAction), nextAction, step + 1) == null) {
                            return new Action[]{
                                    Action.valueOf("Push" + "S" + firstDirection),
                                    Action.valueOf("Push" + firstDirection + secondDirection)
                            };
                        }
                    }
                }
                break;
            //One step: pull box to any direction, agent to S
            case PullNE:
            case PullNW:
                directions = new String[]{"S", "E", "W"};
                Collections.shuffle(Arrays.asList(directions));
                for (String direction : directions) {
                    currentAction[blockedAgent] = Action.valueOf("Push" + "S" + direction);
                    if (test(initialState, currentAction, step) == null) {
                        return new Action[]{Action.valueOf("Push" + "S" + direction)};
                    }
                }
                break;
            //First step: pull box either to E or W, agent to S
            //Second step: pull box in any direction, agent to E or W
            case PullSS:
                directions1 = new String[]{"E", "W"};
                directions2 = new String[]{"N", "E", "S", "W"};
                Collections.shuffle(Arrays.asList(directions1));
                Collections.shuffle(Arrays.asList(directions2));
                for (String firstDirection : directions1) {
                    currentAction[blockedAgent] = Action.valueOf("Push" + "N" + firstDirection);
                    PlanningResult testResult = test(initialState, currentAction, step);
                    if (testResult != null && testResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
                        continue;
                    }
                    for (String secondDirection : directions2) {
                        if ((firstDirection + secondDirection).equals("EW") ||
                                (firstDirection + secondDirection).equals("WE")) {
                            continue;
                        }
                        nextAction[blockedAgent] = Action.valueOf("Push" + firstDirection + secondDirection);
                        if (test(new State(copyState(initialState), currentAction), nextAction, step + 1) == null) {
                            return new Action[]{
                                    Action.valueOf("Push" + "N" + firstDirection),
                                    Action.valueOf("Push" + firstDirection + secondDirection)
                            };
                        }
                    }
                }
                break;
            //One step: pull box to any direction, agent to N
            case PullSE:
            case PullSW:
                directions = new String[]{"N", "E", "W"};
                Collections.shuffle(Arrays.asList(directions));
                for (String direction : directions) {
                    currentAction[blockedAgent] = Action.valueOf("Push" + "N" + direction);
                    if (test(initialState, currentAction, step) == null) {
                        return new Action[]{Action.valueOf("Push" + "N" + direction)};
                    }
                }
                break;
            //One step: pull box to any direction, agent to W
            case PullEN:
            case PullES:
                directions = new String[]{"N", "S", "W"};
                Collections.shuffle(Arrays.asList(directions));
                for (String direction : directions) {
                    currentAction[blockedAgent] = Action.valueOf("Push" + "W" + direction);
                    if (test(initialState, currentAction, step) == null) {
                        return new Action[]{Action.valueOf("Push" + "W" + direction)};
                    }
                }
                break;
            //First step: pull box either to N or S, agent to W
            //Second step: pull box in any direction, agent to E or W
            case PullEE:
                directions1 = new String[]{"N", "S"};
                directions2 = new String[]{"N", "E", "S", "W"};
                Collections.shuffle(Arrays.asList(directions1));
                Collections.shuffle(Arrays.asList(directions2));
                for (String firstDirection : directions1) {
                    currentAction[blockedAgent] = Action.valueOf("Push" + "W" + firstDirection);
                    PlanningResult testResult = test(initialState, currentAction, step);
                    if (testResult != null && testResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
                        continue;
                    }
                    for (String secondDirection : directions2) {
                        if ((firstDirection + secondDirection).equals("NS") ||
                                (firstDirection + secondDirection).equals("SN")) {
                            continue;
                        }
                        nextAction[blockedAgent] = Action.valueOf("Push" + firstDirection + secondDirection);
                        if (test(new State(copyState(initialState), currentAction), nextAction, step + 1) == null) {
                            return new Action[]{
                                    Action.valueOf("Push" + "W" + firstDirection),
                                    Action.valueOf("Push" + firstDirection + secondDirection)
                            };
                        }
                    }
                }
                break;
            //One step: pull box to any direction, agent to E
            case PullWN:
            case PullWS:
                directions = new String[]{"N", "S", "E"};
                Collections.shuffle(Arrays.asList(directions));
                for (String direction : directions) {
                    currentAction[blockedAgent] = Action.valueOf("Push" + "E" + direction);
                    if (test(initialState, currentAction, step) == null) {
                        return new Action[]{Action.valueOf("Push" + "E" + direction)};
                    }
                }
                break;
            //First step: pull box either to N or S, agent to W
            //Second step: pull box in any direction, agent to E or W
            case PullWW:
                directions1 = new String[]{"N", "S"};
                directions2 = new String[]{"N", "E", "S", "W"};
                Collections.shuffle(Arrays.asList(directions1));
                Collections.shuffle(Arrays.asList(directions2));
                for (String firstDirection : directions1) {
                    currentAction[blockedAgent] = Action.valueOf("Push" + "E" + firstDirection);
                    PlanningResult testResult = test(initialState, currentAction, step);
                    if (testResult != null && testResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
                        continue;
                    }
                    for (String secondDirection : directions2) {
                        if ((firstDirection + secondDirection).equals("NS") ||
                                (firstDirection + secondDirection).equals("SN")) {
                            continue;
                        }
                        nextAction[blockedAgent] = Action.valueOf("Push" + firstDirection + secondDirection);
                        if (test(new State(copyState(initialState), currentAction), nextAction, step + 1) == null) {
                            return new Action[]{
                                    Action.valueOf("Push" + "E" + firstDirection),
                                    Action.valueOf("Push" + firstDirection + secondDirection)
                            };
                        }
                    }
                }
                break;
            default:
                throw new InvalidParameterException("Unknown Action");
        }
        return new Action[]{Action.NoOp}; // TODO: this means we can't in any way step back
    }
}