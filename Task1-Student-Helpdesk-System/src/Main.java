import model.Category;
import model.Priority;
import model.Ticket;
import model.TicketStatus;
import model.User;
import service.HelpdeskService;
import util.InputValidator;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console UI: menu loop and user interaction only. All rules live in HelpdeskService. */
public class Main {

    private final HelpdeskService service = new HelpdeskService();
    private final InputValidator in = new InputValidator(new Scanner(System.in));
    private User currentUser;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        seedUsers();
        System.out.println("==============================================");
        System.out.println("      STUDENT HELPDESK - SUPPORT TICKET SYSTEM");
        System.out.println("==============================================");
        try {
            login();
            boolean running = true;
            while (running) {
                printMenu();
                int choice = in.readInt("Enter choice: ", 0, 12);
                try {
                    running = handle(choice);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("  [ERROR] " + e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Exiting.");
        }
        System.out.println("Goodbye!");
    }

    private void seedUsers() {
        service.addUser(new User("S101", "Asha", User.Role.STUDENT));
        service.addUser(new User("S102", "Ravi", User.Role.STUDENT));
        service.addUser(new User("A201", "Meena", User.Role.SUPPORT_AGENT));
        service.addUser(new User("A202", "Karthik", User.Role.SUPPORT_AGENT));
    }

    // ------------------------------------------------------------------ menu

    private void login() {
        System.out.println("\nSelect a user to log in as:");
        List<User> users = service.getAllUsers();
        for (int i = 0; i < users.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, users.get(i));
        }
        int choice = in.readInt("Choose (1-" + users.size() + "): ", 1, users.size());
        currentUser = users.get(choice - 1);
        System.out.println("Logged in as " + currentUser);
    }

    private void printMenu() {
        System.out.println("\n---------------- MAIN MENU ----------------");
        System.out.println("User: " + currentUser);
        System.out.println(" 1. Create new ticket");
        System.out.println(" 2. View all tickets");
        System.out.println(" 3. View ticket details");
        System.out.println(" 4. Search / filter tickets");
        System.out.println(" 5. Update ticket details");
        System.out.println(" 6. Assign ticket          (agents)");
        System.out.println(" 7. Start work on ticket   (agents)  OPEN -> IN_PROGRESS");
        System.out.println(" 8. Resolve ticket         (agents)  IN_PROGRESS -> RESOLVED");
        System.out.println(" 9. Close ticket           (agents)  RESOLVED -> CLOSED");
        System.out.println("10. Reopen ticket");
        System.out.println("11. Summary by status");
        System.out.println("12. Switch user");
        System.out.println(" 0. Exit");
    }

    /** @return false when the user chose to exit */
    private boolean handle(int choice) {
        switch (choice) {
            case 1:  createTicket(); break;
            case 2:  printTickets(service.getAllTickets()); break;
            case 3:  viewDetails(); break;
            case 4:  searchMenu(); break;
            case 5:  updateTicket(); break;
            case 6:  assignTicket(); break;
            case 7:  startWork(); break;
            case 8:  resolveTicket(); break;
            case 9:  closeTicket(); break;
            case 10: reopenTicket(); break;
            case 11: showSummary(); break;
            case 12: login(); break;
            case 0:  return false;
            default: break;
        }
        return true;
    }

    // -------------------------------------------------------------- actions

    private void createTicket() {
        System.out.println("\n--- Create Ticket ---");
        String title = in.readNonEmpty("Title: ");
        String description = in.readNonEmpty("Description: ");
        Category category = in.readEnum("Category", Category.class);
        Priority priority = in.readEnum("Priority", Priority.class);
        Ticket t = service.createTicket(title, description, category, priority, currentUser);
        System.out.println("  [OK] Ticket created with ID " + t.getId());
    }

    private void viewDetails() {
        String id = in.readNonEmpty("Ticket ID (e.g. TKT-1001 or 1001): ");
        System.out.println(service.getTicketOrThrow(id).toDetailedString());
    }

    private void searchMenu() {
        System.out.println("\n--- Search / Filter ---");
        System.out.println("1. By ticket ID");
        System.out.println("2. By status");
        System.out.println("3. By category");
        System.out.println("4. By priority");
        System.out.println("5. By keyword (title/description)");
        System.out.println("6. My tickets");
        int choice = in.readInt("Choose (1-6): ", 1, 6);
        switch (choice) {
            case 1:
                String id = in.readNonEmpty("Ticket ID: ");
                System.out.println(service.getTicketOrThrow(id).toDetailedString());
                return;
            case 2:
                printTickets(service.filterByStatus(in.readEnum("Status", TicketStatus.class)));
                break;
            case 3:
                printTickets(service.filterByCategory(in.readEnum("Category", Category.class)));
                break;
            case 4:
                printTickets(service.filterByPriority(in.readEnum("Priority", Priority.class)));
                break;
            case 5:
                printTickets(service.searchByKeyword(in.readNonEmpty("Keyword: ")));
                break;
            default:
                printTickets(service.getTicketsCreatedBy(currentUser));
                break;
        }
    }

    private void updateTicket() {
        String id = in.readNonEmpty("Ticket ID to update: ");
        Ticket t = service.getTicketOrThrow(id);
        System.out.println(t.toDetailedString());
        System.out.println("Leave a field blank to keep its current value.");

        String title = in.readOptional("New title: ");
        String desc = in.readOptional("New description: ");
        Category category = in.readEnumOrSkip("New category", Category.class);
        Priority priority = in.readEnumOrSkip("New priority", Priority.class);

        service.updateTicket(id, currentUser,
                title.isEmpty() ? null : title,
                desc.isEmpty() ? null : desc,
                category, priority);
        System.out.println("  [OK] Ticket " + t.getId() + " updated.");
    }

    private void assignTicket() {
        String id = in.readNonEmpty("Ticket ID to assign: ");
        List<User> agents = service.getAgents();
        System.out.println("Available agents:");
        for (int i = 0; i < agents.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, agents.get(i));
        }
        int choice = in.readInt("Choose (1-" + agents.size() + "): ", 1, agents.size());
        Ticket t = service.assignTicket(id, currentUser, agents.get(choice - 1));
        System.out.println("  [OK] " + t.getId() + " assigned to " + t.getAssignedTo().getName());
    }

    private void startWork() {
        Ticket t = service.startWork(in.readNonEmpty("Ticket ID: "), currentUser);
        System.out.println("  [OK] " + t.getId() + " is now " + t.getStatus());
    }

    private void resolveTicket() {
        String id = in.readNonEmpty("Ticket ID: ");
        String note = in.readNonEmpty("Resolution note: ");
        Ticket t = service.resolveTicket(id, currentUser, note);
        System.out.println("  [OK] " + t.getId() + " is now " + t.getStatus());
    }

    private void closeTicket() {
        String id = in.readNonEmpty("Ticket ID: ");
        if (!in.confirm("Close " + id.toUpperCase() + "?")) {
            System.out.println("  Cancelled.");
            return;
        }
        Ticket t = service.closeTicket(id, currentUser);
        System.out.println("  [OK] " + t.getId() + " is now " + t.getStatus());
    }

    private void reopenTicket() {
        Ticket t = service.reopenTicket(in.readNonEmpty("Ticket ID: "), currentUser);
        System.out.println("  [OK] " + t.getId() + " reopened (" + t.getStatus() + ")");
    }

    private void showSummary() {
        System.out.println("\n--- Tickets by Status ---");
        for (Map.Entry<TicketStatus, Long> e : service.countByStatus().entrySet()) {
            System.out.printf("  %-12s %d%n", e.getKey(), e.getValue());
        }
    }

    private void printTickets(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println("  No tickets found.");
            return;
        }
        System.out.println();
        System.out.printf("%-9s | %-28s | %-19s | %-7s | %-11s | %s%n",
                "ID", "Title", "Category", "Priority", "Status", "Assigned To");
        System.out.println("-".repeat(100));
        tickets.forEach(System.out::println);
        System.out.println(tickets.size() + " ticket(s).");
    }
}
