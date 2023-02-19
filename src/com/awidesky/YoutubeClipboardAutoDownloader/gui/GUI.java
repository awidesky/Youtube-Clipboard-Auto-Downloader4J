package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import com.awidesky.YoutubeClipboardAutoDownloader.Config;
import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.ClipBoardOption;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.LoadingStatus;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.TableColumnEnum;
import com.awidesky.YoutubeClipboardAutoDownloader.util.SwingDialogs;

public class GUI {
	
	private Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	
	private JFrame loadingFrame;
	private JLabel loadingStatus;
	private JProgressBar initProgress;
	
	public static final Image ICON = new ImageIcon(YoutubeAudioDownloader.getProjectpath().replace(File.separator, "/") + "/YoutubeAudioAutoDownloader-resources/icon.jpg").getImage();

	private JFrame mainFrame;
	private JButton browse, cleanCompleted, removeSwitch, nameFormatHelp, openConfig, modeSwitch, openSaveDir;
	private JLabel format, quality, path, nameFormat, playList;
	private JTextField pathField, nameFormatField;
	private JComboBox<String> cb_format, cb_quality, cb_playList, cb_clipboardOption;
	private DefaultComboBoxModel<String> audioFormatCBoxModel = new DefaultComboBoxModel<>(new String[] { "mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav" });
	private DefaultComboBoxModel<String> videoFormatCBoxModel = new DefaultComboBoxModel<>(new String[] { "mp4", "webm", "3gp", "flv" });
	private DefaultComboBoxModel<String> audioQualityCBoxModel = new DefaultComboBoxModel<>(new String[] { "0(best)", "1", "2", "3", "4", "5", "6", "7", "8", "9(worst)" });
	private DefaultComboBoxModel<String> videoQualityCBoxModel = new DefaultComboBoxModel<>(new String[] { "best", "240p", "360p", "360p", "480p", "720p", "1080p", "1440p", "2160p" });
	private JFileChooser jfc = new JFileChooser();
	private JTable table;
	private JScrollPane scrollPane;
	
	
	public GUI() {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			SwingDialogs.error("Error while setting window look&feel", "%e%", e, false);
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
		loadingFrame.setIconImage(ICON);
		loadingFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		loadingFrame.setSize(450, 100); //add more height than fxml because it does not think about title length
		loadingFrame.setLocation(dim.width/2-loadingFrame.getSize().width/2, dim.height/2-loadingFrame.getSize().height/2);
		loadingFrame.setLayout(null);
		loadingFrame.setResizable(false);
		loadingFrame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				
				e.getWindow().dispose();
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
		mainFrame.setIconImage(ICON);
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.setSize(630, 495);
		mainFrame.setLocation(dim.width/2-mainFrame.getSize().width/2, dim.height/2-mainFrame.getSize().height/2);
		mainFrame.setLayout(null);
		mainFrame.setResizable(false);
		mainFrame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {

				Main.clearTasks();
				if(TaskStatusModel.getinstance().getRowCount() != 0) { return; }
				disposeAll();
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
		browse.requestFocus();
		
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
		quality.setBounds(200, 23, quality.getPreferredSize().width, quality.getPreferredSize().height);
		path.setBounds(12, 70, path.getPreferredSize().width, path.getPreferredSize().height);
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
		
		nameFormatField.addActionListener((e) -> { Config.setFileNameFormat(nameFormatField.getText()); });
		
		pathField.setBounds(65, 65, 456, 22); 
		nameFormatField.setBounds(115, 122, 172, 22);

		mainFrame.add(pathField);
		mainFrame.add(nameFormatField);

	}
	
	private void addButtons() { 
		
		browse = new JButton("Browse...");
		cleanCompleted = new JButton("clean completed");
		removeSwitch = new JButton("remove selected");
		nameFormatHelp = new JButton("<= help?");
		openConfig = new JButton("open config.txt");
		modeSwitch = new JButton(" <-> download video ");
		openSaveDir = new JButton("open folder");
		
		browse.addActionListener((e) -> {

			if (jfc.showDialog(new JFrame(), null) != JFileChooser.APPROVE_OPTION) {
				JOptionPane.showMessageDialog(this.mainFrame, "Please choose a directory!", "ERROR!",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			String path = jfc.getSelectedFile().getAbsolutePath();
			pathField.setText(path);
			jfc.setCurrentDirectory(new File(path));

		});
		cleanCompleted.addActionListener((e) -> { TaskStatusModel.getinstance().clearDone(); });
		nameFormatHelp.addActionListener((e) -> { Main.webBrowse("https://github.com/ytdl-org/youtube-dl#output-template"); });
		openConfig.addActionListener((e) -> { Main.openConfig(); });
		modeSwitch.addActionListener((e) -> { swapMode(); });
		openSaveDir.addActionListener((e) -> { Main.openSaveFolder(); });
		
		browse.setBounds(523, 65, browse.getPreferredSize().width, browse.getPreferredSize().height);
		cleanCompleted.setBounds(8, 418, cleanCompleted.getPreferredSize().width, cleanCompleted.getPreferredSize().height);
		removeSwitch.setBounds(145, 418, removeSwitch.getPreferredSize().width, removeSwitch.getPreferredSize().height);
		nameFormatHelp.setBounds(298, 121, nameFormatHelp.getPreferredSize().width, nameFormatHelp.getPreferredSize().height);
		openConfig.setBounds(490, 418, openConfig.getPreferredSize().width, openConfig.getPreferredSize().height);
		modeSwitch.setBounds(440, 19, modeSwitch.getPreferredSize().width, modeSwitch.getPreferredSize().height);
		openSaveDir.setBounds(515, 90, openSaveDir.getPreferredSize().width, openSaveDir.getPreferredSize().height);
		
		switchRemoveSwitch(false);
		
		mainFrame.add(browse);
		mainFrame.add(cleanCompleted);
		mainFrame.add(removeSwitch);
		mainFrame.add(nameFormatHelp);
		mainFrame.add(openConfig);
		mainFrame.add(modeSwitch);
		mainFrame.add(openSaveDir);
		
	}
	
	private void swapMode() {
		
		if(Main.audioMode) {
			cb_format.setModel(videoFormatCBoxModel);
			cb_quality.setModel(videoQualityCBoxModel);
			quality.setText("Video Quality :");
			modeSwitch.setText("<-> download audio");
		} else {
			cb_format.setModel(audioFormatCBoxModel);
			cb_quality.setModel(audioQualityCBoxModel);
			quality.setText("Audio Quality :");
			modeSwitch.setText(" <-> download video ");
		}

		cb_quality.setSelectedIndex(0);
		Main.audioMode = !Main.audioMode;
		
	}


	private void addComboBoxes() {
		
		cb_format = new JComboBox<>(audioFormatCBoxModel);
		cb_quality = new JComboBox<>(audioQualityCBoxModel);
		cb_playList = new JComboBox<>(PlayListOption.getComboBoxList());
		cb_clipboardOption = new JComboBox<>(ClipBoardOption.getComboBoxList());
		cb_format.setEditable(true);
		
		if(videoQualityCBoxModel.getIndexOf(Config.getQuality()) == -1) { //It was audio mode
			cb_quality.setSelectedIndex(Integer.parseInt(Config.getQuality()));
		} else {
			swapMode();
			cb_quality.setSelectedItem(Config.getQuality());
		}
		cb_format.setSelectedItem(Config.getFormat());
		cb_playList.setSelectedItem(Config.getPlaylistOption().toComboBox());
		cb_clipboardOption.setSelectedItem(Config.getClipboardListenOption().toString());

		cb_format.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		
		((JTextComponent) cb_format.getEditor().getEditorComponent()).getDocument()
		.addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) { 
				Config.setFormat(cb_format.getEditor().getItem().toString());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				Config.setFormat(cb_format.getEditor().getItem().toString());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				Config.setFormat(cb_format.getEditor().getItem().toString());
			}

		});
 
		cb_format.addActionListener((e) -> { 
	        if(cb_format.getSelectedIndex() >= 0) {
	        	Config.setFormat(cb_format.getEditor().getItem().toString());
			} else if("comboBoxEdited".equals(e.getActionCommand())) {
				Config.setFormat(cb_format.getEditor().getItem().toString());
	        }
		});
		cb_quality.addActionListener((e) -> { Config.setQuality(cb_quality.getSelectedItem().toString()); });
		cb_playList.addActionListener((e) -> { Config.setPlaylistOption(cb_playList.getSelectedItem().toString()); });
		cb_clipboardOption.addActionListener((e) -> { Config.setClipboardListenOption(cb_clipboardOption.getSelectedItem().toString());	});
		
		cb_format.setBounds(83, 19, 80, 22);
		cb_quality.setBounds(290, 19, 100, 22);
		cb_playList.setBounds(518, 122, 90, 22);
		cb_clipboardOption.setBounds(280, 419, 200, 22);
		
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
				if (column == TableColumnEnum.PROGRESS.getIndex()) return TaskStatusModel.getinstance().getProgressToolTip(row);
				if (column == TableColumnEnum.CHECKBOX.getIndex()) return "click to select this task";
				return String.valueOf(TaskStatusModel.getinstance().getValueAt(row, column));
			}
			
		};
		
		table.setModel(TaskStatusModel.getinstance());
		table.setAutoCreateColumnsFromModel(false);
		table.getColumn("Progress").setCellRenderer(new ProgressRenderer());
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(TableColumnEnum.CHECKBOX.getIndex()).setPreferredWidth(24);
		table.getColumnModel().getColumn(TableColumnEnum.VIDEO_NAME.getIndex()).setPreferredWidth(120);
		table.getColumnModel().getColumn(TableColumnEnum.DESTINATION.getIndex()).setPreferredWidth(300);
		table.getColumnModel().getColumn(TableColumnEnum.PROGRESS.getIndex()).setPreferredWidth(73);
		table.getColumnModel().getColumn(TableColumnEnum.STATUS.getIndex()).setPreferredWidth(82);
		
		TaskStatusModel.getinstance().setCheckBoxSelectedCallback(this::switchRemoveSwitch);
		
		scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(8, 168, 600, 240);

		mainFrame.add(scrollPane);

	}

	private void switchRemoveSwitch(boolean selectedMode) {
		if(selectedMode) {
			removeSwitch.setText("remove selected");
			Arrays.stream(removeSwitch.getActionListeners()).forEach(removeSwitch::removeActionListener);
			removeSwitch.addActionListener((e) -> { switchRemoveSwitch(!TaskStatusModel.getinstance().removeSelected(table.getSelectedRows()));	});
		} else {
			removeSwitch.setText("clear All");
			Arrays.stream(removeSwitch.getActionListeners()).forEach(removeSwitch::removeActionListener);
			removeSwitch.addActionListener((e) -> { TaskStatusModel.getinstance().clearAll(); });
		}
	}
	
	public void disposeAll() {
		
		if (Main.verbose) {
			Main.log("\nExisting Windows are :");
			Stream.of(JWindow.getWindows()).map(Window::toString).forEach(Main::log);
			Main.log("\n");
		}
		
		Stream.of(JWindow.getWindows()).forEach(Window::dispose);
		disposeLoadingFrame();
		if (mainFrame != null) mainFrame.dispose();
		
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
	

	public String getSavePath() {
		final AtomicReference<String> result = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				result.set(pathField.getText());
			});
		} catch (Exception e) {
			SwingDialogs.error("Failed to wait for EDT!", "Unable to get TextField data!\n%e%", e, false);
			return Config.getSaveto();
		}
		return result.get();
	} 
	
}
