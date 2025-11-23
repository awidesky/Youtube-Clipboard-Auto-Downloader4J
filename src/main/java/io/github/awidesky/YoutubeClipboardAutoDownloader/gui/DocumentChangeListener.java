package io.github.awidesky.YoutubeClipboardAutoDownloader.gui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DocumentChangeListener implements DocumentListener {
	private final Runnable changed;
	
	public DocumentChangeListener(Runnable changed) { this.changed = changed; }
	
	public void changedUpdate(DocumentEvent e) { changed.run(); }
	public void removeUpdate(DocumentEvent e) {	changed.run(); }
	public void insertUpdate(DocumentEvent e) { changed.run(); }
}
