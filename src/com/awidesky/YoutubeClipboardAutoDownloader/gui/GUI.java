package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.awidesky.YoutubeClipboardAutoDownloader.Config;
import com.awidesky.YoutubeClipboardAutoDownloader.LoadingStatus;
import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;

public class GUI {
	
	private Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	private static final JDialog confirmDialogParent = new JDialog();
	
	private JFrame loadingFrame;
	private JLabel loadingStatus;
	private JProgressBar initProgress;
	

	private JFrame mainFrame;
	private JButton browse, cleanCompleted, cleanAll, nameFormatHelp, openConfig;
	private JLabel format, quality, path, nameFormat, playList;
	private JTextField pathField, nameFormatField;
	private JComboBox<String> cb_format, cb_quality, cb_playList, cb_clipboardOption;
	private JFileChooser jfc = new JFileChooser();
	private JTable table;
	private JScrollPane scrollPane;
	
	
	public GUI() {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			error("Error while setting window look&feel", "%e%", e);
		}
		
	}
	
	
	/**
	 * 
	 * Make <code>loadingFrame</code>> and show <code>loadingFrame</code>> before making <code>mainFrame</code>
	 * This does not show <code>mainFrame</code>.
	 * To show <code>mainFrame</code>, use <code>showmMainFrame</code>
	 * @see GUI#showmMainFrame() showmMainFrame
	 * 
	 * */
	public void initLoadingFrame() {
		
		/** make <code>loadingFrame</code> */
		loadingFrame = new JFrame();
		loadingFrame.setTitle("loading...");
		loadingFrame.setIconImage(new ImageIcon(
				YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\icon.jpg")
						.getImage());
		loadingFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		loadingFrame.setSize(450, 100); //add more height than fxml because it does not think about title length
		loadingFrame.setLocation(dim.width/2-loadingFrame.getSize().width/2, dim.height/2-loadingFrame.getSize().height/2);
		loadingFrame.setLayout(null);
		loadingFrame.setResizable(false);
		loadingFrame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				
				e.getWindow().dispose();
				confirmDialogParent.dispose();
				Main.log("LoadingFrame was closed");
				Main.kill(0);

			}

		});
		
		loadingStatus = new JLabel("");
		loadingStatus.setBounds(14, 8, 370, 18);
		
		initProgress = new JProgressBar();
		initProgress.setBounds(15, 27, 370, 18);
		
		loadingFrame.add(loadingStatus);
		loadingFrame.add(initProgress);
		loadingFrame.setVisible(true);
		
	}
	
	public void initMainFrame() { 
		
		/** make <code>mainFrame</code> */
		mainFrame = new JFrame();
		mainFrame.setTitle("Youtube Audio Auto Downloader " + Main.version);
		mainFrame.setIconImage(new ImageIcon(
				YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\icon.jpg")
						.getImage());
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.setSize(630, 495);
		mainFrame.setLocation(dim.width/2-mainFrame.getSize().width/2, dim.height/2-mainFrame.getSize().height/2);
		mainFrame.setLayout(null);
		mainFrame.setResizable(false);
		mainFrame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				
				e.getWindow().dispose();
				Main.kill(0);

			}

		});

		addFileChooser();
		addLabels();
		addTextFields();
		addButtons();
		addComboBoxes();
		addTable();
		disposeLoadingFrame();
		mainFrame.setVisible(true);
		
	}
	

	private void addFileChooser() {
		
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setDialogTitle("Choose directory to save music!");
		jfc.setCurrentDirectory(new File(Config.getSaveto()));
	}
	
	private void addLabels() {
		
		format = new JLabel("Format :");
		quality = new JLabel("Audio Quality :");
		path = new JLabel("Save to :");
		nameFormat = new JLabel("Filename Format : ");
		playList = new JLabel("Download Playlist? : ");
		
		format.setBounds(26, 23, format.getPreferredSize().width, format.getPreferredSize().height);
		quality.setBounds(273, 23, quality.getPreferredSize().width, quality.getPreferredSize().height);
		path.setBounds(12, 80, path.getPreferredSize().width, path.getPreferredSize().height);
		nameFormat.setBounds(10, 126, nameFormat.getPreferredSize().width, nameFormat.getPreferredSize().height);
		playList.setBounds(395, 126, playList.getPreferredSize().width, playList.getPreferredSize().height);
		
		mainFrame.add(format);
		mainFrame.add(path);
		mainFrame.add(quality);
		mainFrame.add(nameFormat);
		mainFrame.add(playList);
		
	}
	
	private void addTextFields() {
		
		pathField = new JTextField(Config.getSaveto());
		nameFormatField =  new JTextField(Config.getFileNameFormat());
		
		pathField.addActionListener((e) -> { Config.setSaveto(pathField.getText()); });
		nameFormatField.addActionListener((e) -> { Config.setFileNameFormat(nameFormatField.getText()); });
		
		pathField.setBounds(65, 76, 456, 22); 
		nameFormatField.setBounds(115, 122, 172, 22);

		mainFrame.add(pathField);
		mainFrame.add(nameFormatField);

	}
	
	private void addButtons() {
		
		browse = new JButton("Browse...");
		cleanCompleted = new JButton("clean completed");
		cleanAll = new JButton("clean all");
		nameFormatHelp = new JButton("<= help?");
		openConfig = new JButton("open config.txt");
		
		browse.addActionListener((e) -> {

			if (jfc.showDialog(new JFrame(), null) != JFileChooser.APPROVE_OPTION) {
				JOptionPane.showMessageDialog(null, "Please choose a directory!", "ERROR!",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			String path = jfc.getSelectedFile().getAbsolutePath();
			Config.setSaveto(path);
			pathField.setText(path);
			jfc.setCurrentDirectory(new File(path));

		});
		cleanCompleted.addActionListener((e) -> { TaskStatusModel.getinstance().clearDone(); });
		cleanAll.addActionListener((e) -> { TaskStatusModel.getinstance().clearAll(); });
		nameFormatHelp.addActionListener((e) -> { Main.webBrowse("https://github.com/ytdl-org/youtube-dl#output-template"); });
		openConfig.addActionListener((e) -> {Main.openConfig();});
		
		browse.setBounds(523, 75, browse.getPreferredSize().width, browse.getPreferredSize().height);
		cleanCompleted.setBounds(14, 418, cleanCompleted.getPreferredSize().width, cleanCompleted.getPreferredSize().height);
		cleanAll.setBounds(160, 418, cleanAll.getPreferredSize().width, cleanAll.getPreferredSize().height);
		nameFormatHelp.setBounds(298, 121, nameFormatHelp.getPreferredSize().width, nameFormatHelp.getPreferredSize().height);
		openConfig.setBounds(490, 418, openConfig.getPreferredSize().width, openConfig.getPreferredSize().height);
		
		mainFrame.add(browse);
		mainFrame.add(cleanCompleted);
		mainFrame.add(cleanAll);
		mainFrame.add(nameFormatHelp);
		mainFrame.add(openConfig);
		
	}
	
	private void addComboBoxes() {
		
		cb_format = new JComboBox<>(new String[] { "mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav" });
		cb_quality = new JComboBox<>(new String[] { "0(best)", "1", "2", "3", "4", "5", "6", "7", "8", "9(worst)" });
		cb_playList = new JComboBox<>(new String[] { "yes", "no", "ask" });
		cb_clipboardOption = new JComboBox<>(new String[] { "Download link automatically",
															"Ask when a link is found",
															"Stop listening clipboard" });
		
		cb_format.setSelectedItem(Config.getFormat());
		cb_quality.setSelectedIndex(Integer.parseInt(Config.getQuality()));
		cb_playList.setSelectedItem(Config.getPlaylistOption().toComboBox());
		cb_clipboardOption.setSelectedItem(Config.getClipboardListenOption());

		cb_format.addActionListener((e) -> { Config.setFormat(cb_format.getSelectedItem().toString()); });
		cb_quality.addActionListener((e) -> { Config.setQuality(String.valueOf(cb_quality.getSelectedIndex())); });
		cb_playList.addActionListener((e) -> { Config.setPlaylistOption(cb_playList.getSelectedItem().toString()); });
		cb_clipboardOption.addActionListener((e) -> {Config.setClipboardListenOption(cb_clipboardOption.getSelectedItem().toString());});
		
		cb_format.setBounds(83, 19, 96, 22);
		cb_quality.setBounds(365, 19, 150, 22);
		cb_playList.setBounds(518, 122, 90, 22);
		cb_clipboardOption.setBounds(270, 418, 200, 22);

		mainFrame.add(cb_format);
		mainFrame.add(cb_quality);
		mainFrame.add(cb_playList);
		mainFrame.add(cb_clipboardOption);
		
	}
	
	private void addTable() {
		
		table = new JTable() {
			
			/**
			 * serialVersionUID
			 */
			private static final long serialVersionUID = 6021131657635813356L;

			@Override
			public String getToolTipText(MouseEvent e) { 
				int row = rowAtPoint(e.getPoint());
				int column = columnAtPoint(e.getPoint());
				if (row == -1) return "";
				if (column == 2) return TaskStatusModel.getinstance().getProgressToolTip(row);
				return String.valueOf(TaskStatusModel.getinstance().getValueAt(row, column));
			}
			
		};
		
		table.setModel(TaskStatusModel.getinstance());
		table.setAutoCreateColumnsFromModel(false);
		table.getColumn("Progress").setCellRenderer(new ProgressRenderer());
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(120);
		table.getColumnModel().getColumn(1).setPreferredWidth(324);
		table.getColumnModel().getColumn(2).setPreferredWidth(73);
		table.getColumnModel().getColumn(3).setPreferredWidth(82);
		
		scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(8, 168, 600, 240);

		mainFrame.add(scrollPane);

	}

	public void disposeAll() {
		
		disposeLoadingFrame();
		if (mainFrame != null) mainFrame.dispose();
		confirmDialogParent.dispose();
		
		mainFrame = null;
		
	}
	
	private void disposeLoadingFrame() {

		if (loadingFrame != null) {
			loadingFrame.setVisible(false);
			loadingFrame.dispose();
		}
		
		loadingFrame = null;
		loadingStatus = null;
		initProgress = null;
		
	}
	
	public void setLoadingStat(LoadingStatus stat) {
		
		loadingStatus.setText(stat.getStatus());
		initProgress.setValue(stat.getProgress());
		
	}
	

	/**
	 * show error dialog.
	 * String <code>"%e%"</code> in <code>content</code> will replaced by error message of given <code>Exception</code> if it's not <code>null</code>
	 * */
	public static void error(String title, String content, Exception e) {

		Main.log("\n");
		String co = content.replace("%e%", (e == null) ? "null" : e.getMessage());
		SwingUtilities.invokeLater(() -> {
			
			final JDialog dialog = new JDialog();
			dialog.setAlwaysOnTop(true);  
			JOptionPane.showMessageDialog(dialog, co, title, JOptionPane.ERROR_MESSAGE);
			
		});
		
		Main.log("[GUI.error] " + title + "\n\t" + co);
		if(e != null) Main.log(e);
		
	}

	/**
	 * show warning dialog.
	 * String <code>"%e%"</code> in <code>content</code> will replaced by warning message of given <code>Exception</code> if it's not <code>null</code>
	 * 
	 * */
	public static void warning(String title, String content, Exception e) {

		Main.log("\n");
		String co = content.replace("%e%", (e == null) ? "null" : e.getMessage());
		SwingUtilities.invokeLater(() -> {

			final JDialog dialog = new JDialog();
			dialog.setAlwaysOnTop(true);  
			JOptionPane.showMessageDialog(dialog, co, title, JOptionPane.WARNING_MESSAGE);
			
		});
		
		Main.log("[GUI.warning] " + title + "\n\t" + co);
		if(e != null) Main.log(e);
		
	}

	public static boolean confirm(String title, String message) {

		confirmDialogParent.setAlwaysOnTop(true);
		
		if (EventQueue.isDispatchThread()) {

			return showConfirmDialog(title, message, confirmDialogParent);
			
		} else {
			
			final AtomicReference<Boolean> result = new AtomicReference<>();

			try {

				SwingUtilities.invokeAndWait(() -> {
					result.set(showConfirmDialog(title, message, confirmDialogParent));
				});
				
				return result.get();

			} catch (InterruptedException | InvocationTargetException e) {
				GUI.error("Exception in Thread working(SwingWorker)",
						e.getClass().getName() + "-%e%\nI'll consider you chose \"no\"", e);
			}

			return false;

		}

	}
	
	private static boolean showConfirmDialog(String title, String message, JDialog dialog) {
		return JOptionPane.showConfirmDialog(dialog, message, title,JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	} 

}
