package sagex.jetty.util;

import java.io.Serializable;

public class PrefItem extends Property<String> implements Serializable {
	private static final long serialVersionUID = 1L;
	private String label = null;
    private String description = null;
    private String key = null;
    private boolean isGroup = false;
    private String defaultValue = null;
    private String resetValue = null;
    private String type = null;
    private Hints hints = new Hints();
	private PrefItem[] children = null;
	private String listSeparator;
    
    public Hints getHints() {
		return hints;
	}

	public void setHints(Hints hints) {
		this.hints = hints;
	}


    public PrefItem() {
        super(null);
    }

    public PrefItem[] getChildren() {
        return children;
    }

    public void setChildren(PrefItem[] children) {
        this.children = children;
    }
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String text) {
        this.label = text;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean isGroup) {
        this.isGroup = isGroup;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return get();
    }

    public void setValue(String value) {
        set(value);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getResetValue() {
        return resetValue;
    }

    public void setResetValue(String resetValue) {
        this.resetValue = resetValue;
    }
    
    public boolean hasChanged() {
        return (get()!=null && !get().equals(resetValue));
    }

	public void setListSeparator(String listSeparator) {
		if (listSeparator==null||listSeparator.trim().length()==0) listSeparator=null;
		this.listSeparator = listSeparator;
	}
	
	public String getListSeparator() {
		return listSeparator;
	}
}
