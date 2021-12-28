package sagex.jetty.util;

import java.io.Serializable;

public class Property<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	private T value;
    private boolean visible=true;
    private boolean readOnly=false;
    
    public Property() {
    }
    
    public Property(T value) {
        this.value=value;
    }
    
    public T get() {
        return value;
    }
    
    public void set(T value) {
        this.value=value;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean vis) {
        this.visible=vis;
    }

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
}
