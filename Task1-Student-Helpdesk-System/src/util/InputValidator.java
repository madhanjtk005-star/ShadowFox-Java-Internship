package util;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Safe wrappers around Scanner. Every read uses nextLine() and parses manually,
 * so bad input (letters instead of numbers, blank lines) never crashes the
 * program and never leaves stray characters in the buffer.
 */
public class InputValidator {

    private final Scanner scanner;

    public InputValidator(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Reads a raw line. Throws NoSuchElementException if the input stream ended. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("Input stream closed.");
        }
        return scanner.nextLine();
    }

    /** Re-prompts until the user enters a whole number between min and max (inclusive). */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt).trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  ! Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  ! Invalid input. Please enter a number.");
            }
        }
    }

    /** Re-prompts until the user enters something other than blank/whitespace. */
    public String readNonEmpty(String prompt) {
        while (true) {
            String line = readLine(prompt).trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("  ! This field cannot be empty.");
        }
    }

    /** Returns the trimmed line, which may be empty (used for "press Enter to keep"). */
    public String readOptional(String prompt) {
        return readLine(prompt).trim();
    }

    /** Shows a numbered list of enum values and returns the chosen one. */
    public <E extends Enum<E>> E readEnum(String label, Class<E> enumClass) {
        E[] values = enumClass.getEnumConstants();
        System.out.println(label + ":");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, values[i]);
        }
        int choice = readInt("Choose (1-" + values.length + "): ", 1, values.length);
        return values[choice - 1];
    }

    /** Like readEnum, but pressing Enter returns null (meaning "keep current"). */
    public <E extends Enum<E>> E readEnumOrSkip(String label, Class<E> enumClass) {
        E[] values = enumClass.getEnumConstants();
        System.out.println(label + " (press Enter to keep current):");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, values[i]);
        }
        while (true) {
            String line = readLine("Choose (1-" + values.length + ") or Enter to skip: ").trim();
            if (line.isEmpty()) {
                return null;
            }
            try {
                int choice = Integer.parseInt(line);
                if (choice >= 1 && choice <= values.length) {
                    return values[choice - 1];
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("  ! Invalid choice.");
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String line = readLine(prompt + " (y/n): ").trim().toLowerCase();
            if (line.equals("y") || line.equals("yes")) return true;
            if (line.equals("n") || line.equals("no")) return false;
            System.out.println("  ! Please type y or n.");
        }
    }
}
