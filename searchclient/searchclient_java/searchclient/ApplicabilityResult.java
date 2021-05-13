package searchclient;

public class ApplicabilityResult {
    ApplicabilityType type = ApplicabilityType.NO_ISSUE;
    char box;
    int agent;

    enum ApplicabilityType {NO_ISSUE, WALL, BOX, AGENT, CONDITIONS_NOT_MET}

    public boolean isApplicable() {
        return this.type == ApplicabilityType.NO_ISSUE;
    }

    public char getCause() {
        char result;
        switch (this.type) {
            case BOX:
                result = this.box;
                break;
            case AGENT:
                result = (char) (this.agent + '0');
                break;
            default:
                result = '\0';
                break;
        }
        return result;
    }
}
