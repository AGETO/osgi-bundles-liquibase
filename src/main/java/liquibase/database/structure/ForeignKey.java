package liquibase.database.structure;

import java.util.List;

public class ForeignKey implements DatabaseObject, Comparable<ForeignKey> {
    private Table primaryKeyTable;
    private String primaryKeyColumns;

    private Table foreignKeyTable;
    private String foreignKeyColumns;

    private String name;

    private boolean deferrable;
    private boolean initiallyDeferred;

	// Some databases supports creation of FK with referention to column marked as unique, not primary
	// If FK referenced to such unique column this option should be set to false
	private boolean referencesUniqueColumn = false;

    private ForeignKeyConstraintType updateRule;
    private ForeignKeyConstraintType deleteRule;

    public DatabaseObject[] getContainingObjects() {
        return new DatabaseObject[] {
                new liquibase.database.structure.Column()
                        .setName(getPrimaryKeyColumns())
                        .setTable(getPrimaryKeyTable()),
                new liquibase.database.structure.Column()
                        .setName(getForeignKeyColumns())
                        .setTable(getForeignKeyTable())

        };
    }

    public Table getPrimaryKeyTable() {
        return primaryKeyTable;
    }

    public void setPrimaryKeyTable(Table primaryKeyTable) {
        this.primaryKeyTable = primaryKeyTable;
    }

    public String getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    public void addPrimaryKeyColumn(String primaryKeyColumn) {
        if ((this.primaryKeyColumns == null)
                || (this.primaryKeyColumns.length() == 0)) {
            this.primaryKeyColumns = primaryKeyColumn;
        } else {
            this.primaryKeyColumns = this.primaryKeyColumns + ", "
                    + primaryKeyColumn;
        }
    }

    public void setPrimaryKeyColumns(String primaryKeyColumns) {
        this.primaryKeyColumns = primaryKeyColumns;
    }

    public Table getForeignKeyTable() {
        return foreignKeyTable;
    }

    public void setForeignKeyTable(Table foreignKeyTable) {
        this.foreignKeyTable = foreignKeyTable;
    }

    public String getForeignKeyColumns() {
        return foreignKeyColumns;
    }

    public void addForeignKeyColumn(String foreignKeyColumn) {
        if ((this.foreignKeyColumns == null)
                || (this.foreignKeyColumns.length() == 0)) {
            this.foreignKeyColumns = foreignKeyColumn;
        } else {
            this.foreignKeyColumns = this.foreignKeyColumns + ", "
                    + foreignKeyColumn;
        }
    }

    public void setForeignKeyColumns(String foreignKeyColumns) {
        this.foreignKeyColumns = foreignKeyColumns;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return getName() + "(" + getForeignKeyTable() + "." + getForeignKeyColumns() + " ->" + getPrimaryKeyTable() + "." + getPrimaryKeyColumns() + ")";
    }


    public boolean isDeferrable() {
        return deferrable;
    }

    public void setDeferrable(boolean deferrable) {
        this.deferrable = deferrable;
    }


    public boolean isInitiallyDeferred() {
        return initiallyDeferred;
    }

    public void setInitiallyDeferred(boolean initiallyDeferred) {
        this.initiallyDeferred = initiallyDeferred;
    }

    public void setUpdateRule(ForeignKeyConstraintType rule) {
        this.updateRule = rule;
    }

    public ForeignKeyConstraintType getUpdateRule() {
        return this.updateRule;
    }

    public void setDeleteRule(ForeignKeyConstraintType rule) {
        this.deleteRule = rule;
    }

    public ForeignKeyConstraintType getDeleteRule() {
        return this.deleteRule;
    }

	public boolean getReferencesUniqueColumn() {
		return referencesUniqueColumn;
	}

	public void setReferencesUniqueColumn(boolean referencesUniqueColumn) {
		this.referencesUniqueColumn = referencesUniqueColumn;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ForeignKey that = (ForeignKey) o;

        if (getForeignKeyColumns() == null) {
            return this.getName().equalsIgnoreCase(that.getName());
        }

        return getForeignKeyColumns().equalsIgnoreCase(that.getForeignKeyColumns())
                && foreignKeyTable.equals(that.foreignKeyTable)
                && getPrimaryKeyColumns().equalsIgnoreCase(that.getPrimaryKeyColumns())
                && primaryKeyTable.equals(that.primaryKeyTable)
		        && referencesUniqueColumn == that.getReferencesUniqueColumn();
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (primaryKeyTable != null) {
            result = primaryKeyTable.hashCode();
        }
        if (primaryKeyColumns != null) {
            result = 31 * result + primaryKeyColumns.toUpperCase().hashCode();
        }

        if (foreignKeyTable != null) {
            result = 31 * result + foreignKeyTable.hashCode();
        }

        if (foreignKeyColumns != null) {
            result = 31 * result + foreignKeyColumns.toUpperCase().hashCode();
        }

        return result;
    }


    public int compareTo(ForeignKey o) {
        int returnValue = 0;
        if (this.getForeignKeyTable() != null && o.getForeignKeyTable() != null) {
            returnValue = this.getForeignKeyTable().compareTo(o.getForeignKeyTable());
        }
        if (returnValue == 0 && this.getForeignKeyColumns() != null && o.getForeignKeyColumns() != null) {
            returnValue = this.getForeignKeyColumns().compareToIgnoreCase(o.getForeignKeyColumns());
        }
        if (returnValue == 0 && this.getName() != null && o.getName() != null) {
            returnValue = this.getName().compareToIgnoreCase(o.getName());
        }
        if (returnValue == 0 && this.getPrimaryKeyTable() != null && o.getPrimaryKeyTable() != null) {
            returnValue = this.getPrimaryKeyTable().compareTo(o.getPrimaryKeyTable());
        }

        if (returnValue == 0 && this.getPrimaryKeyColumns() != null && o.getPrimaryKeyColumns() != null) {
            returnValue = this.getPrimaryKeyColumns().compareToIgnoreCase(o.getPrimaryKeyColumns());
        }
        if (returnValue == 0 && this.updateRule != null && o.getUpdateRule() != null)
            returnValue = this.updateRule.compareTo(o.getUpdateRule());
        if (returnValue == 0 && this.deleteRule != null && o.getDeleteRule() != null)
            returnValue = this.deleteRule.compareTo(o.getDeleteRule());
        return returnValue;
    }

    private String toDisplayString(List<String> columnsNames) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String columnName : columnsNames) {
            i++;
            sb.append(columnName);
            if (i < columnsNames.size()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
