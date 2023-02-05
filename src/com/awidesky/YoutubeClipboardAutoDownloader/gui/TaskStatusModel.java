package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.EventQueue;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.TaskData;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.TableColumnEnum;

public class TaskStatusModel extends AbstractTableModel {

	/**
	 * Default serialVersionUID
	 */
	private static final long serialVersionUID = -7803447765391487650L;

	private static TaskStatusModel instance = new TaskStatusModel();
	private List<TaskData> rows = new Vector<>();

	private Consumer<Boolean> checkBoxSelectedCalback = null;
	
	private TaskStatusModel() {
	}

	public static TaskStatusModel getinstance() {
		return instance;
	}

	public void setCheckBoxSelectedCallback(Consumer<Boolean> checkBoxSelectedCalback) {
		this.checkBoxSelectedCalback = checkBoxSelectedCalback;
	}
	
	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return 5;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (TableColumnEnum.valueOfIndex(columnIndex)) {
		case CHECKBOX:
			return rows.get(rowIndex).isChecked();
		case VIDEO_NAME: // name
			return rows.get(rowIndex).getVideoName();
		case DESTINATION: // path
			return rows.get(rowIndex).getDest();
		case PROGRESS: // progress
			return rows.get(rowIndex).getProgress();
		case STATUS: // status
			return rows.get(rowIndex).getStatus();
		default:
			break;
		}
		GUI.error("Invalid column index!", "Invalid column index : " + columnIndex, null, false);
		return null; // this should not happen!
	}

	@Override
	public Class<?> getColumnClass(int column) {
        return (column == TableColumnEnum.CHECKBOX.getIndex()) ? Boolean.class : (getValueAt(0, column).getClass());
    }
	
	@Override
	public String getColumnName(int column) {

		TableColumnEnum result = TableColumnEnum.valueOfIndex(column);
		if(result == null) {
			GUI.error("Invalid column index!", "Invalid column index : " + column, null, false);
			return "null"; // this should not happen!
		} else {
			return result.getName();
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		super.setValueAt(value, row, col);
		if(col == TableColumnEnum.CHECKBOX.getIndex()) {
			boolean result = !(Boolean)this.getValueAt(row, col);
			rows.get(row).setChecked(result);
			result = rows.stream().anyMatch(TaskData::isChecked);
			checkBoxSelectedCalback.accept(result);
		}
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		if(column == TableColumnEnum.CHECKBOX.getIndex()) return true;
		else return false;
	}
	
	public void clearDone() {

		rows.removeIf((t) -> t.getStatus().equals("Done!"));
		fireTableDataChanged();

	}

	/**
	 * @return <code>true</code> if user didn't cancel the removing.
	 * */
	public boolean removeSelected(int[] selected) {
		
		if (Arrays.stream(selected).mapToObj(rows::get).anyMatch(TaskData::isNotDone)) 
			if (!GUI.confirm("Before removing!", "Some task(s) you chose are not done!\nCancel those task(s)?"))
				return false;
		
		Arrays.stream(selected).mapToObj(rows::get).filter(TaskData::isNotDone).forEach(TaskData::kill);
		Arrays.stream(selected).forEach(rows::remove);
		if(rows.isEmpty()) checkBoxSelectedCalback.accept(false);
		fireTableDataChanged();
		return true;
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
