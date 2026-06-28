package io.atlas.modules.rank.model;

public class Rank {

    private final long id;
    private final String identifier;
    private final String displayName;
    private final String prefix;
    private final int priority;
    private final boolean staff;

    public Rank(long id,
                String identifier,
                String displayName,
                String prefix,
                int priority,
                boolean staff) {

        this.id = id;
        this.identifier = identifier;
        this.displayName = displayName;
        this.prefix = prefix;
        this.priority = priority;
        this.staff = staff;
    }

    public long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isStaff() {
        return staff;
    }
}