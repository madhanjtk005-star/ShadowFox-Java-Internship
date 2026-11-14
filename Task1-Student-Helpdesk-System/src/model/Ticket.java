package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** A support ticket. State changes are validated by HelpdeskService. */
public class Ticket {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final String id;
    private String title;
    private String description;
    private Category category;
    private Priority priority;
    private TicketStatus status;
    private final User createdBy;
    private User assignedTo;
    private String resolutionNote;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Ticket(String id, String title, String description,
                  Category category, Priority priority, User createdBy) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.createdBy = createdBy;
        this.status = TicketStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Priority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public User getAssignedTo() { return assignedTo; }
    public String getResolutionNote() { return resolutionNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; touch(); }
    public void setDescription(String description) { this.description = description; touch(); }
    public void setCategory(Category category) { this.category = category; touch(); }
    public void setPriority(Priority priority) { this.priority = priority; touch(); }
    public void setStatus(TicketStatus status) { this.status = status; touch(); }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; touch(); }
    public void setResolutionNote(String note) { this.resolutionNote = note; touch(); }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /** One-line summary used in list views. */
    @Override
    public String toString() {
        return String.format("%-9s | %-28s | %-19s | %-7s | %-11s | %s",
                id,
                title.length() > 28 ? title.substring(0, 25) + "..." : title,
                category, priority, status,
                assignedTo == null ? "Unassigned" : assignedTo.getName());
    }

    /** Multi-line view used for ticket details. */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("----------------------------------------------\n");
        sb.append("Ticket ID   : ").append(id).append('\n');
        sb.append("Title       : ").append(title).append('\n');
        sb.append("Description : ").append(description).append('\n');
        sb.append("Category    : ").append(category).append('\n');
        sb.append("Priority    : ").append(priority).append('\n');
        sb.append("Status      : ").append(status).append('\n');
        sb.append("Created by  : ").append(createdBy).append('\n');
        sb.append("Assigned to : ").append(assignedTo == null ? "Unassigned" : assignedTo).append('\n');
        if (resolutionNote != null) {
            sb.append("Resolution  : ").append(resolutionNote).append('\n');
        }
        sb.append("Created     : ").append(createdAt.format(FMT)).append('\n');
        sb.append("Last updated: ").append(updatedAt.format(FMT)).append('\n');
        sb.append("----------------------------------------------");
        return sb.toString();
    }
}
