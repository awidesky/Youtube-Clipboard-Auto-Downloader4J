package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.EventQueue;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.TaskData;

public class TaskStatusModel extends AbstractTableModel {

	/**
	 * Default serialVersionUID
	 */
	private static final long serialVersionUID = -7803447765391487650L;

	private static TaskStatusModel instance = new TaskStatusModel();
	private List<TaskData> rows = new Vector<>();

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

	public void removeSelected(int[] selected) {

		
		if (Arrays.stream(selected).mapToObj(rows::get).anyMatch(TaskData::isNotDone)) 
			if (!GUI.confirm("Before clearing!", "Some task(s) you chose are not done!\nCancel those task(s)?"))
				return;
		
		Arrays.stream(selected).mapToObj(rows::get).filter(TaskData::isNotDone).forEach(TaskData::kill);
		Arrays.stream(selected).forEach(rows::remove);
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

	public void updated(TaskData t) {  //use Fireupdate, and add checkbox

		setValueAt(t.getVideoName(), rows.indexOf(t), 0);
		setValueAt(t.getDest(), rows.indexOf(t), 0);
		setValueAt(t.getProgress(), rows.indexOf(t), 0);
		setValueAt(t.getStatus(), rows.indexOf(t), 0);

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

	public boolean isTaskExists(TaskData t) {
		
		if (EventQueue.isDispatchThread()) {

			return rows.contains(t);
			
		} else {
			
			final AtomicReference<Boolean> result = new AtomicReference<>();

			try {

				SwingUtilities.invokeAndWait(() -> {
					result.set(rows.contains(t));
				});
				return result.get();

			} catch (Exception e) {
				
				Main.log("Exception when checking existing Task(s)");
				Main.log(e);
				
			}
			
			return false;

		}
		
	}
	
	public boolean isTaskDone(TaskData t) {
		
		if(!isTaskExists(t)) {
			return false;
		}
		
		if (EventQueue.isDispatchThread()) {

			return "Done!".equals(rows.get(rows.indexOf(t)).getStatus());
			
		} else {
			
			final AtomicReference<Boolean> result = new AtomicReference<>();

			try {

				SwingUtilities.invokeAndWait(() -> {
					result.set("Done!".equals(rows.get(rows.indexOf(t)).getStatus()));
				});
				return result.get();

			} catch (Exception e) {
				
				Main.log("Exception when checking existing Task(s) is done");
				Main.log(e);
				
			}
			
			return false;

		}
	}

}
