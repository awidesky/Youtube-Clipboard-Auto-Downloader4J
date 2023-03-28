package com.awidesky.YoutubeClipboardAutoDownloader.enums;

import java.util.HashMap;
import java.util.Map;

public enum TableColumnEnum {

	CHECKBOX(0, ""),
	VIDEO_NAME(1, "Video Name"),
	DESTINATION(2, "Destination"),
	PROGRESS(3, "Progress"),
	STATUS(4, "Status");
	
	private final int index;
	private final String name;
    private static final Map<Integer, TableColumnEnum> BY_INDEX = new HashMap<>();
    private static final Map<String, TableColumnEnum> BY_NAME = new HashMap<>();
    
    static {
        for (TableColumnEnum e: values()) {
            BY_INDEX.put(e.index, e);
            BY_NAME.put(e.name, e);
        }
    }

	private TableColumnEnum(int index, String name) {
		this.index = index;
		this.name = name;
	}
	
	public int getIndex() { return index; }
	public String getName() { return name; }
	
	public static TableColumnEnum valueOfIndex(int index) { return BY_INDEX.get(index); }
	public static TableColumnEnum valueOfName(String name) { return BY_NAME.get(name); }
	
}
