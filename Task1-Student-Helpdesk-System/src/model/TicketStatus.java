package model;

/** Lifecycle states of a ticket and the allowed transitions between them. */
public enum TicketStatus {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    /**
     * Workflow rules:
     * OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
     * RESOLVED / CLOSED -> OPEN (reopen)
     */
    public boolean canTransitionTo(TicketStatus next) {
        switch (this) {
            case OPEN:        return next == IN_PROGRESS;
            case IN_PROGRESS: return next == RESOLVED;
            case RESOLVED:    return next == CLOSED || next == OPEN;
            case CLOSED:      return next == OPEN;
            default:          return false;
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
