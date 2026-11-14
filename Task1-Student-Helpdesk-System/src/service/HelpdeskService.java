package service;

import model.Category;
import model.Priority;
import model.Ticket;
import model.TicketStatus;
import model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core business logic: creating, updating, searching and transitioning tickets.
 * Throws IllegalArgumentException for invalid input and IllegalStateException
 * for rule/permission violations; the UI layer decides how to display them.
 */
public class HelpdeskService {

    private static final int FIRST_TICKET_NUMBER = 1001;
    private static final int MAX_TITLE_LENGTH = 100;

    private final Map<String, Ticket> tickets = new LinkedHashMap<>();
    private final Map<String, User> users = new LinkedHashMap<>();
    private int nextTicketNumber = FIRST_TICKET_NUMBER;

    // ---------------------------------------------------------------- users

    public void addUser(User user) {
        if (users.containsKey(user.getId())) {
            throw new IllegalArgumentException("User ID already exists: " + user.getId());
        }
        users.put(user.getId(), user);
    }

    public User findUser(String id) {
        return users.get(id.trim().toUpperCase());
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public List<User> getAgents() {
        return users.values().stream().filter(User::isAgent).collect(Collectors.toList());
    }

    // -------------------------------------------------------------- tickets

    /** Creates a ticket with an auto-generated unique ID such as TKT-1001. */
    public Ticket createTicket(String title, String description,
                               Category category, Priority priority, User creator) {
        String cleanTitle = requireText(title, "Title");
        String cleanDesc = requireText(description, "Description");
        if (cleanTitle.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title must be at most " + MAX_TITLE_LENGTH + " characters.");
        }
        if (category == null || priority == null || creator == null) {
            throw new IllegalArgumentException("Category, priority and creator are required.");
        }
        String id = "TKT-" + nextTicketNumber++;
        Ticket ticket = new Ticket(id, cleanTitle, cleanDesc, category, priority, creator);
        tickets.put(id, ticket);
        return ticket;
    }

    /** Updates details. Pass null for any field that should stay unchanged. */
    public Ticket updateTicket(String ticketId, User actor, String newTitle, String newDescription,
                               Category newCategory, Priority newPriority) {
        Ticket t = getTicketOrThrow(ticketId);
        requireOwnerOrAgent(t, actor);
        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException("Ticket " + t.getId() + " is CLOSED. Reopen it before updating.");
        }
        if (newTitle != null) {
            String clean = requireText(newTitle, "Title");
            if (clean.length() > MAX_TITLE_LENGTH) {
                throw new IllegalArgumentException("Title must be at most " + MAX_TITLE_LENGTH + " characters.");
            }
            t.setTitle(clean);
        }
        if (newDescription != null) t.setDescription(requireText(newDescription, "Description"));
        if (newCategory != null) t.setCategory(newCategory);
        if (newPriority != null) t.setPriority(newPriority);
        return t;
    }

    public Ticket assignTicket(String ticketId, User actor, User agent) {
        requireAgent(actor);
        if (agent == null || !agent.isAgent()) {
            throw new IllegalArgumentException("Tickets can only be assigned to a support agent.");
        }
        Ticket t = getTicketOrThrow(ticketId);
        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException("Cannot assign a CLOSED ticket. Reopen it first.");
        }
        t.setAssignedTo(agent);
        return t;
    }

    // ----------------------------------------------------- status transitions

    public Ticket startWork(String ticketId, User actor) {
        requireAgent(actor);
        Ticket t = getTicketOrThrow(ticketId);
        transition(t, TicketStatus.IN_PROGRESS);
        if (t.getAssignedTo() == null) {
            t.setAssignedTo(actor); // the agent who starts work owns the ticket
        }
        return t;
    }

    public Ticket resolveTicket(String ticketId, User actor, String resolutionNote) {
        requireAgent(actor);
        Ticket t = getTicketOrThrow(ticketId);
        String note = requireText(resolutionNote, "Resolution note");
        transition(t, TicketStatus.RESOLVED);
        t.setResolutionNote(note);
        return t;
    }

    public Ticket closeTicket(String ticketId, User actor) {
        requireAgent(actor);
        Ticket t = getTicketOrThrow(ticketId);
        transition(t, TicketStatus.CLOSED);
        return t;
    }

    public Ticket reopenTicket(String ticketId, User actor) {
        Ticket t = getTicketOrThrow(ticketId);
        requireOwnerOrAgent(t, actor);
        transition(t, TicketStatus.OPEN);
        t.setResolutionNote(null);
        return t;
    }

    private void transition(Ticket t, TicketStatus next) {
        if (!t.getStatus().canTransitionTo(next)) {
            throw new IllegalStateException(String.format(
                    "Invalid status change for %s: %s -> %s is not allowed.",
                    t.getId(), t.getStatus(), next));
        }
        t.setStatus(next);
    }

    // ------------------------------------------------------- view and search

    public Ticket getTicketOrThrow(String ticketId) {
        Ticket t = ticketId == null ? null : tickets.get(normaliseId(ticketId));
        if (t == null) {
            throw new IllegalArgumentException("No ticket found with ID: " + ticketId);
        }
        return t;
    }

    public List<Ticket> getAllTickets() {
        return new ArrayList<>(tickets.values());
    }

    public List<Ticket> filterByStatus(TicketStatus status) {
        return tickets.values().stream().filter(t -> t.getStatus() == status).collect(Collectors.toList());
    }

    public List<Ticket> filterByCategory(Category category) {
        return tickets.values().stream().filter(t -> t.getCategory() == category).collect(Collectors.toList());
    }

    public List<Ticket> filterByPriority(Priority priority) {
        return tickets.values().stream().filter(t -> t.getPriority() == priority).collect(Collectors.toList());
    }

    public List<Ticket> searchByKeyword(String keyword) {
        String k = requireText(keyword, "Keyword").toLowerCase();
        return tickets.values().stream()
                .filter(t -> t.getTitle().toLowerCase().contains(k)
                        || t.getDescription().toLowerCase().contains(k))
                .collect(Collectors.toList());
    }

    public List<Ticket> getTicketsCreatedBy(User user) {
        return tickets.values().stream()
                .filter(t -> t.getCreatedBy().getId().equals(user.getId()))
                .collect(Collectors.toList());
    }

    /** Counts tickets per status for the summary view. */
    public Map<TicketStatus, Long> countByStatus() {
        Map<TicketStatus, Long> counts = new LinkedHashMap<>();
        for (TicketStatus s : TicketStatus.values()) {
            counts.put(s, tickets.values().stream().filter(t -> t.getStatus() == s).count());
        }
        return counts;
    }

    // --------------------------------------------------------------- helpers

    private String normaliseId(String id) {
        String s = id.trim().toUpperCase();
        // allow typing just "1001"
        return s.matches("\\d+") ? "TKT-" + s : s;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty or blank.");
        }
        return value.trim();
    }

    private void requireAgent(User actor) {
        if (actor == null || !actor.isAgent()) {
            throw new IllegalStateException("Only support agents can perform this action.");
        }
    }

    private void requireOwnerOrAgent(Ticket t, User actor) {
        boolean owner = actor != null && t.getCreatedBy().getId().equals(actor.getId());
        if (!owner && (actor == null || !actor.isAgent())) {
            throw new IllegalStateException("You can only modify tickets that you created.");
        }
    }
}
