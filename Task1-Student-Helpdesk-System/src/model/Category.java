package model;

/** Classification of support issues. */
public enum Category {
    IT_SUPPORT("IT Support"),
    ACADEMICS("Academics"),
    FACILITIES("Facilities / Hostel"),
    OTHER("Other");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
