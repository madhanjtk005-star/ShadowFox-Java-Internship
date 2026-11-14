package model;

/** A person using the helpdesk: either a student or a support agent. */
public class User {

    public enum Role {
        STUDENT("Student"),
        SUPPORT_AGENT("Support Agent");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final String id;
    private final String name;
    private final Role role;

    public User(String id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }

    public boolean isAgent() {
        return role == Role.SUPPORT_AGENT;
    }

    @Override
    public String toString() {
        return name + " (" + id + ", " + role + ")";
    }
}
