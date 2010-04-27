package org.getalp.blexisma.wiktionary;

import java.io.Serializable;

public class OffsetValue implements Serializable {
	/**
     * 
     */
    private static final long serialVersionUID = -570466055812338894L;
    public int start;
	public int length;

	public OffsetValue(int start, int length) {
		this.start = start;
		this.length = length;
	}

    public String toString() {
        return "" + start + "/" + length;
	}
}
