package liquibase.database.structure;

public class Sequence implements DatabaseObject, Comparable<Sequence> {
    private String name;
    private String schema;


    public DatabaseObject[] getContainingObjects() {
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public int compareTo(Sequence o) {
        return this.getName().compareTo(o.getName());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sequence sequence = (Sequence) o;

        return !(name != null ? !name.equalsIgnoreCase(sequence.name) : sequence.name != null);

    }

    @Override
    public int hashCode() {
        return (name != null ? name.toUpperCase().hashCode() : 0);
    }


    @Override
    public String toString() {
        return getName();
    }

	/**
	 * @return Returns the schema.
	 */
	public String getSchema () {
		return schema;
	}

	/**
	 * @param schema The schema to set.
	 */
	public void setSchema (String schema) {
		this.schema = schema;
	}
}
