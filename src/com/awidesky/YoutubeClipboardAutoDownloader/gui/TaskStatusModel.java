package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class TaskStatusModel extends AbstractTableModel {

	/**
	 * Default serialVersionUID
	 */
	private static final long serialVersionUID = -7803447765391487650L;

	private static TaskStatusModel instance = new TaskStatusModel();
	private List<TaskData> rows = Collections.synchronizedList(new ArrayList<>());
	
	private TaskStatusModel() {}
	
	public static TaskStatusModel getinstance() { return instance; }

	@Override
	public int getRowCount() {
		return rows.size();
	}


	@Override
	public int getColumnCount() {
		return 4;
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
		case 0: //name
			return rows.get(rowIndex).getVideoName();
		case 1: //path
			return rows.get(rowIndex).getDest();
		case 2: //progress
			return rows.get(rowIndex).getProgress();
		case 3: //status
			return rows.get(rowIndex).getStatus();
		}
		GUI.error("Invalid column index!", "Invalid column index : " + columnIndex);
		return null; //this should not happen!
	}

	
	@Override
	public String getColumnName(int column) {

		switch(column) {
		case 0: //name
			return "Video Name";
		case 1: //path
			return "Destination";
		case 2: //progress
			return "Progress";
		case 3: //status
			return "Status";
		}
		GUI.error("Invalid column index!", "Invalid column index : " + column);
		return "null"; //this should not happen!
	}

	public void clearDone() {
		
		rows.removeIf((t) -> t.getStatus().equals("Done!"));
		fireTableDataChanged();
		
	}
	
	public void clearAll() {
		
		rows.clear();
		fireTableDataChanged();
		
	}
	
	public void updated(TaskData t) {
		
		fireTableRowsUpdated(rows.indexOf(t), rows.indexOf(t));
		
	}

	public void addTask(TaskData t) {

		SwingUtilities.invokeLater(() -> {
			rows.add(t);
			fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
		});
		
	}

	public String getUrlOf(int row) {
		
		return rows.get(row).getUrl();
		
	}
	
	
}
