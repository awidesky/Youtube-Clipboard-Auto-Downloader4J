package io.github.awidesky.YoutubeAudioAutoDownloader.gui;

import java.awt.Component;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ProgressRenderer extends JProgressBar implements TableCellRenderer {

	private static final long serialVersionUID = -7507572782263584532L;

	public ProgressRenderer() {
		super(0, 100);
		this.setStringPainted(true);
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		this.setValue((Integer)value); 
		this.setString((Integer)value + "%");
		return this;
	}

}
