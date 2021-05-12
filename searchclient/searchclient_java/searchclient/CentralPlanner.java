package searchclient;

import java.util.LinkedList;

public class CentralPlanner {

    public LinkedList<Action[][]> individualplans;
    public State initialState;

    public CentralPlanner(State initialState) {
        this.individualplans = new LinkedList<>();
        this.initialState = initialState;
    }

    public void addPlan(Action[][] plan) {
        this.individualplans.addLast(plan);
        System.err.println("PLANS: " + this.individualplans.size());
    }

    public Action[] getJointAction(int step) {
        Action[] jointAction = new Action[this.individualplans.size()];
        for (int i = 0; i < this.individualplans.size(); i++) {
            if (step < this.individualplans.get(i).length) {
                jointAction[i] = this.individualplans.get(i)[step][0];
            } else {
                jointAction[i] = Action.NoOp;
            }
        }
        return jointAction;
    }

}