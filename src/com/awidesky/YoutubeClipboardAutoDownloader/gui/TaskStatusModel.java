package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.awidesky.YoutubeClipboardAutoDownloader.TaskData;

public class TaskStatusModel extends AbstractTableModel {

	/**
	 * Default serialVersionUID
	 */
	private static final long serialVersionUID = -7803447765391487650L;

	private static TaskStatusModel instance = new TaskStatusModel();
	private List<TaskData> rows = new ArrayList<>();

	private TaskStatusModel() {
	}

	public static TaskStatusModel getinstance() {
		return instance;
	}

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
		switch (columnIndex) {
		case 0: // name
			return rows.get(rowIndex).getVideoName();
		case 1: // path
			return rows.get(rowIndex).getDest();
		case 2: // progress
			return rows.get(rowIndex).getProgress();
		case 3: // status
			return rows.get(rowIndex).getStatus();
		}
		GUI.error("Invalid column index!", "Invalid column index : " + columnIndex, null, false);
		return null; // this should not happen!
	}

	@Override
	public String getColumnName(int column) {

		switch (column) {
		case 0: // name
			return "Video Name";
		case 1: // path
			return "Destination";
		case 2: // progress
			return "Progress";
		case 3: // status
			return "Status";
		}
		GUI.error("Invalid column index!", "Invalid column index : " + column, null, false);
		return "null"; // this should not happen!
	}

	public void clearDone() {

		rows.removeIf((t) -> t.getStatus().equals("Done!"));
		fireTableDataChanged();

	}

	public void clearAll() {

		if (rows.stream().anyMatch(TaskData::isNotDone)) 
			if (!GUI.confirm("Before clearing!", "Some task(s) are not done!\nCancel all task(s) and clear list?"))
				return;
		
		rows.stream().filter(TaskData::isNotDone).forEach(TaskData::kill);
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

	public String getProgressToolTip(int row) {
		return rows.get(row).getProgressToolTip();
	}

}
