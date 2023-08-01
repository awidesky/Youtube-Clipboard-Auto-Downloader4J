package io.github.awidesky.YoutubeAudioAutoDownloader.gui;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import io.github.awidesky.YoutubeAudioAutoDownloader.Main;
import io.github.awidesky.YoutubeAudioAutoDownloader.TaskData;
import io.github.awidesky.YoutubeAudioAutoDownloader.enums.TableColumnEnum;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;

public class TaskStatusModel extends AbstractTableModel {

	private static final long serialVersionUID = 8199693587883074204L;

	private static TaskStatusModel instance = new TaskStatusModel();
	private static Logger log = Main.getLogger("[TaskStatusModel] ");
	private List<TaskData> rows = new Vector<>();

	private Consumer<Boolean> checkBoxSelectedCalback = null;
	
	private TaskStatusModel() {}
	public static TaskStatusModel getinstance() { return instance; }

	public void setCheckBoxSelectedCallback(Consumer<Boolean> checkBoxSelectedCalback) { this.checkBoxSelectedCalback = checkBoxSelectedCalback; }
	
	@Override
	public int getRowCount() { return rows.size(); }
	@Override
	public int getColumnCount() { return 5; }

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
		log.log("invalid index - row : " + rowIndex + ", col :" + columnIndex);
		SwingDialogs.error("Invalid column index!", "Invalid column index : " + columnIndex, null, false);
		return null; // this should not happen!
	}

	@Override
	public Class<?> getColumnClass(int column) {  return (column == TableColumnEnum.CHECKBOX.getIndex()) ? Boolean.class : (getValueAt(0, column).getClass()); }
	
	@Override
	public String getColumnName(int column) {
		TableColumnEnum result = TableColumnEnum.valueOfIndex(column);
		if(result == null) {
			SwingDialogs.error("Invalid column index!", "Invalid column index : " + column, null, false);
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
	public boolean removeSelected() {
		List<TaskData> selected = rows.stream().filter(TaskData::isChecked).toList();
		
		if (selected.stream().anyMatch(TaskData::isRunning)) 
			if (!SwingDialogs.confirm("Before removing!", "Some task(s) you chose are not done yet!\nCancel those task(s)?"))
				return false;
		
		selected.stream().filter(TaskData::isRunning).forEach(TaskData::kill);
		rows.removeAll(selected);
		if(rows.isEmpty()) checkBoxSelectedCalback.accept(false);
		fireTableDataChanged();
		return true;
	}
	
	public void clearAll() {
		if (rows.stream().anyMatch(TaskData::isRunning)) 
			if (!SwingDialogs.confirm("Before clearing!", "Some task(s) are not done yet!\nCancel all task(s) and clear all?"))
				return;
		
		rows.stream().filter(TaskData::isRunning).forEach(TaskData::kill);
		rows.clear();
		fireTableDataChanged();
	}

	public void updated(TaskData t) { fireTableRowsUpdated(rows.indexOf(t), rows.indexOf(t)); }

	public void addTask(TaskData t) {
		Runnable r = () -> {
			rows.add(t);
			fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
		};
		if(EventQueue.isDispatchThread()) {
			SwingUtilities.invokeLater(r);
		} else {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch (InvocationTargetException | InterruptedException e) {
				log.log("Exception when waitng EDT for add task to the Table!");
				log.log(e);
				SwingUtilities.invokeLater(r);
			}
		}
	}

	public String getProgressToolTip(int row) { return rows.get(row).getProgressToolTip(); }

	public boolean isTaskExists(TaskData t) {
		if (EventQueue.isDispatchThread()) {
			return rows.contains(t);
		} else {
			final AtomicReference<Boolean> result = new AtomicReference<>();
			try {
				SwingUtilities.invokeAndWait(() -> { result.set(rows.contains(t)); });
				return result.get();
			} catch (Exception e) {
				log.log("Exception when checking existing Task(s)");
				log.log(e);
			}
			return false;
		}
	}
	
	/**
	 * Is same task exists in the table, and the status Strings are identical?
	 * */
	public boolean isTaskExistsSameStatus(TaskData t) {
		if(!isTaskExists(t)) { return false; }
		if (EventQueue.isDispatchThread()) {
			return rows.get(rows.indexOf(t)).getStatus().equals(t.getStatus());
		} else {
			final AtomicReference<Boolean> result = new AtomicReference<>();
			try {
				SwingUtilities.invokeAndWait(() -> { result.set(rows.get(rows.indexOf(t)).getStatus().equals(t.getStatus())); });
				return result.get();
			} catch (Exception e) {
				log.log("Exception when checking existing Task(s) is done");
				log.log(e);
			}
			return false;
		}
	}

}
