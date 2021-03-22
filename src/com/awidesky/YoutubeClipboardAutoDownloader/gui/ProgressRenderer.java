package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.Component;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ProgressRenderer extends JProgressBar implements TableCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7507572782263584532L;

	public ProgressRenderer() {
		super(0, 100);
		setStringPainted(true);
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		setValue((int)value);
		setString((int)value + "%");
		return this;
	}

}
