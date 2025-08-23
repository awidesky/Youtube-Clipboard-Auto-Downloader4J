package io.github.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import io.github.awidesky.YoutubeClipboardAutoDownloader.Config;
import io.github.awidesky.YoutubeClipboardAutoDownloader.Main;
import io.github.awidesky.YoutubeClipboardAutoDownloader.YoutubeClipboardAutoDownloader;
import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.ClipBoardOption;
import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.ExitCodes;
import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.LoadingStatus;
import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;
import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.TableColumnEnum;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.OSUtil;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;

public class GUI {
	
	private Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	
	private JFrame loadingFrame;
	private JLabel loadingStatus;
	private JProgressBar initProgress;
	
	public static final Image ICON = getICON();

	private JFrame mainFrame;
	private JButton browse, cleanCompleted, removeSwitch, nameFormatHelp, openAppFolder, modeSwitch, openSaveDir;
	private JLabel format, quality_icon, quality, path, nameFormat, playList;
	private JTextField pathField, nameFormatField;
	private JComboBox<String> cb_format, cb_quality, cb_playList, cb_clipboardOption;
	private DefaultComboBoxModel<String> audioFormatCBoxModel = new DefaultComboBoxModel<>(new String[] { "mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav" });
	private DefaultComboBoxModel<String> videoFormatCBoxModel = new DefaultComboBoxModel<>(new String[] { "mp4", "webm", "3gp", "flv" });
	private DefaultComboBoxModel<String> audioQualityCBoxModel = new DefaultComboBoxModel<>(new String[] { "0(best)", "1", "2", "3", "4", "5", "6", "7", "8", "9(worst)" });
	private DefaultComboBoxModel<String> videoQualityCBoxModel = new DefaultComboBoxModel<>(new String[] { "best", "240p", "360p", "360p", "480p", "720p", "1080p", "1440p", "2160p" });
	private JFileChooser jfc = new JFileChooser();
	private JTable table;
	
	private Logger logger = null;
	
	public GUI() {}
	
	
	/**
	 * 
	 * Make <code>loadingFrame</code>> and show <code>loadingFrame</code>> before making <code>mainFrame</code>
	 * This does not show <code>mainFrame</code>.
	 * To show <code>mainFrame</code>, use <code>showmMainFrame</code>
	 * @see GUI#showmMainFrame() showmMainFrame
	 * 
	 * */
	public void initLoadingFrame() {
		
		logger = Main.getLogger("[GUI] ");
		
		/** make <code>loadingFrame</code> */
		loadingFrame = new JFrame();
		loadingFrame.setTitle("loading...");
		if(!OSUtil.isWindows() && Taskbar.isTaskbarSupported()) Taskbar.getTaskbar().setIconImage(ICON);
		loadingFrame.setIconImage(ICON);
		loadingFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		loadingFrame.setSize(450, 100); //add more height than fxml because it does not think about title length
		loadingFrame.setLocation(dim.width/2-loadingFrame.getSize().width/2, dim.height/2-loadingFrame.getSize().height/2);
		loadingFrame.getContentPane().setLayout(new BoxLayout(loadingFrame.getContentPane(), BoxLayout.Y_AXIS));
		loadingFrame.setResizable(false);
		loadingFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				e.getWindow().dispose();
				logger.log("LoadingFrame was closed");
				Main.kill(ExitCodes.SUCCESSFUL);
			}
		});
		
		
		loadingStatus = new JLabel("");
		JPanel loading = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
		loading.add(Box.createHorizontalStrut(10));
		loading.add(loadingStatus, gbc);
		
		initProgress = new JProgressBar();
		JPanel init = new JPanel(new GridBagLayout());
		init.add(Box.createHorizontalStrut(10));
		init.add(initProgress, gbc);
		init.add(Box.createHorizontalStrut(10));
		

		loadingFrame.add(loading);
		loadingFrame.add(init);
		loadingFrame.setVisible(true);
		
	}
	
	public void initMainFrame() { 
		
		/** make <code>mainFrame</code> */
		mainFrame = new JFrame();
		mainFrame.setTitle("Clipboard-dl " + Main.version);
		if(Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Feature.ICON_IMAGE)) Taskbar.getTaskbar().setIconImage(ICON);
		mainFrame.setIconImage(ICON);
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.setSize(630, 500);
		mainFrame.setLayout(new BorderLayout());
		mainFrame.setResizable(true);
		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				Main.clearTasks();
				if(TaskStatusModel.getinstance().getRowCount() != 0) { return; }
				disposeAll();
				e.getWindow().dispose();
				Main.kill(ExitCodes.SUCCESSFUL);
			}
		});

		setFileChooser();
		setLabels();
		setTextFields();
		setButtons();
		setComboBoxes();
		setTable();
		
		final int strutBetweenPanels = 10;
		JPanel configPanel = new JPanel();
		configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
		configPanel.add(Box.createVerticalStrut(strutBetweenPanels));
		configPanel.add(formatPanel());
		configPanel.add(Box.createVerticalStrut(strutBetweenPanels));
		configPanel.add(savetoPanel());
		configPanel.add(Box.createVerticalStrut(strutBetweenPanels));
		configPanel.add(fileNamePanel());
		configPanel.add(Box.createVerticalStrut(strutBetweenPanels));
		mainFrame.add(configPanel, BorderLayout.NORTH);
		
		mainFrame.add(tablePanel(), BorderLayout.CENTER);
		
		mainFrame.add(bottomPanel(), BorderLayout.SOUTH);
		mainFrame.pack();
		mainFrame.setLocation(dim.width/2-mainFrame.getSize().width/2, dim.height/2-mainFrame.getSize().height/2);

		initProgress.setValue(100);
		disposeLoadingFrame();

		mainFrame.setVisible(true);
		browse.requestFocus();
		
	}
	

	private void setFileChooser() {
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setDialogTitle("Choose directory to save music!");
		jfc.setCurrentDirectory(new File(Config.getSaveto()));
	}
	
	private void setLabels() {
		format = new JLabel("Format :");
		quality_icon = new JLabel("\uD83C\uDFB5\u0020");
		if(quality_icon.getFont().canDisplayUpTo("\uD83C\uDF9E\uD83C\uDFB5") != -1) {
			if(OSUtil.isWindows()) quality_icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
			else if(OSUtil.isLinux()) quality_icon.setFont(new Font("Noto Color Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
			else if(OSUtil.isMac()) quality_icon.setFont(new Font("Apple Color Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
			else Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
					.filter(f -> f.canDisplayUpTo("\uD83C\uDF9E\uD83C\uDFB5") == -1).findFirst()
					.ifPresent(quality_icon::setFont);
		}
		quality = new JLabel("Audio Quality :");
		path = new JLabel("Save to :");
		nameFormat = new JLabel("Filename Format : ");
		playList = new JLabel("Download Playlist? : ");
		
		format.setSize(format.getPreferredSize().width, format.getPreferredSize().height);
		quality.setSize(quality.getPreferredSize().width, quality.getPreferredSize().height);
		path.setSize(path.getPreferredSize().width, path.getPreferredSize().height);
		nameFormat.setSize(nameFormat.getPreferredSize().width, nameFormat.getPreferredSize().height);
		playList.setSize(playList.getPreferredSize().width, playList.getPreferredSize().height);
	}
	
	private void setTextFields() {
		
		pathField = new JTextField(Config.getSaveto());
		nameFormatField =  new JTextField(Config.getFileNameFormat());
		
		nameFormatField.setColumns(10);
		
		pathField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) { changed(); }
			public void removeUpdate(DocumentEvent e) {	changed(); }
			public void insertUpdate(DocumentEvent e) { changed(); }
			public void changed() {
				String str = pathField.getText();
				File f = new File(str);
				if (f.isDirectory() && f.exists()) Config.setSaveto(str);
				int w = pathField.getPreferredSize().width - pathField.getWidth();
				if(w <= 0) return;
				w = mainFrame.getX() - w / 2;
				w = w > 0 ? w : 0;
				mainFrame.pack();
				mainFrame.setLocation(dim.width / 2 - mainFrame.getSize().width / 2, mainFrame.getLocation().y);
			}
		});
		nameFormatField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) { changed(); }
			public void removeUpdate(DocumentEvent e) {	changed(); }
			public void insertUpdate(DocumentEvent e) { changed(); }
			public void changed() { Config.setFileNameFormat(nameFormatField.getText()); }
		});

	}
	
	private void setButtons() { 
		
		browse = new JButton("browse...");
		cleanCompleted = new JButton("clean completed");
		removeSwitch = new JButton("remove selected");
		nameFormatHelp = new JButton("<= help?");
		openAppFolder = new JButton("open app folder");
		modeSwitch = new JButton(" <-> download video ");
		openSaveDir = new JButton("open");
		
		browse.addActionListener((e) -> {
			if (jfc.showDialog(mainFrame, null) != JFileChooser.APPROVE_OPTION) {
				SwingDialogs.error("ERROR!", "Please choose a directory!", null, false);
				return;
			}

			String path = jfc.getSelectedFile().getAbsolutePath();
			pathField.setText(path);
			File f = new File(path);
			if(f.isDirectory() && f.exists()) Config.setSaveto(path);
			jfc.setCurrentDirectory(f);
		});
		cleanCompleted.addActionListener((e) -> { TaskStatusModel.getinstance().clearDone(); });
		nameFormatHelp.addActionListener((e) -> { Main.webBrowse("https://github.com/yt-dlp/yt-dlp#output-template"); });
		openAppFolder.addActionListener((e) -> { Main.openAppFolder(); });
		modeSwitch.addActionListener((e) -> { swapMode(); });
		openSaveDir.addActionListener((e) -> { Main.openSaveFolder(); });
		
		browse.setSize(browse.getPreferredSize().width, browse.getPreferredSize().height);
		cleanCompleted.setSize(cleanCompleted.getPreferredSize().width, cleanCompleted.getPreferredSize().height);
		removeSwitch.setSize(removeSwitch.getPreferredSize().width, removeSwitch.getPreferredSize().height);
		nameFormatHelp.setSize(nameFormatHelp.getPreferredSize().width, nameFormatHelp.getPreferredSize().height);
		openAppFolder.setSize(openAppFolder.getPreferredSize().width, openAppFolder.getPreferredSize().height);
		modeSwitch.setSize(modeSwitch.getPreferredSize().width, modeSwitch.getPreferredSize().height);
		openSaveDir.setSize(openSaveDir.getPreferredSize().width, openSaveDir.getPreferredSize().height);
		
		removeSwitch(false);
		
	}
	
	private void swapMode() {
		if(Main.audioMode.get()) {
			Main.audioMode.set(false);
			cb_format.setModel(videoFormatCBoxModel);
			cb_quality.setModel(videoQualityCBoxModel);
			quality_icon.setText("\uD83C\uDF9E\u0020");
			quality.setText("Video Quality :");
			modeSwitch.setText("<-> download audio");
		} else {
			Main.audioMode.set(true);
			cb_format.setModel(audioFormatCBoxModel);
			cb_quality.setModel(audioQualityCBoxModel);
			quality_icon.setText("\uD83C\uDFB5\u0020");
			quality.setText("Audio Quality :");
			modeSwitch.setText("<-> download video");
		}

		cb_quality.setSelectedIndex(0);
	}


	private void setComboBoxes() {
		
		cb_format = new JComboBox<>(audioFormatCBoxModel);
		cb_quality = new JComboBox<>(audioQualityCBoxModel);
		cb_playList = new JComboBox<>(PlayListOption.getComboBoxList());
		cb_clipboardOption = new JComboBox<>(ClipBoardOption.getComboBoxStrings());
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
			public void removeUpdate(DocumentEvent e) {  Config.setFormat(cb_format.getEditor().getItem().toString()); }
			@Override
			public void insertUpdate(DocumentEvent e) { Config.setFormat(cb_format.getEditor().getItem().toString()); }
			@Override
			public void changedUpdate(DocumentEvent e) { Config.setFormat(cb_format.getEditor().getItem().toString()); }
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
		
		cb_format.setSize(80, 22);
		cb_quality.setSize(100, 22);
		cb_playList.setSize(90, 22);
		cb_clipboardOption.setSize(200, 22);
		
	}
	
	private void setTable() {
		
		table = new JTable() {
			/** serialVersionUID */
			private static final long serialVersionUID = 6021131657635813356L;
			@Override
			public String getToolTipText(MouseEvent e) { 
				int row = rowAtPoint(e.getPoint());
				int column = columnAtPoint(e.getPoint());
				if (row == -1) return "";
				if (column == TableColumnEnum.PROGRESS.getIndex()) return TaskStatusModel.getinstance().getProgressToolTip(row);
				if (column == TableColumnEnum.STATUS.getIndex()) return TaskStatusModel.getinstance().getStatusToolTip(row);
				if (column == TableColumnEnum.CHECKBOX.getIndex()) return "click to select this task";
				return String.valueOf(TaskStatusModel.getinstance().getValueAt(row, column));
			}
		};
		
		table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent mouseEvent) {
		        Point point = mouseEvent.getPoint();
		        int row = table.rowAtPoint(point);
		        if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1 && row != -1) {
		        	JTextArea ta = new JTextArea(TaskStatusModel.getinstance().getTaskData(table.convertRowIndexToModel(row)).toString());
		        	ta.setEditable(false);
		        	//TODO : add retry button?
		        	JOptionPane.showMessageDialog(null, ta, "Task info", JOptionPane.INFORMATION_MESSAGE);
		        }
		    }
		});
		table.setModel(TaskStatusModel.getinstance());
		table.setAutoCreateColumnsFromModel(false);
		table.getColumn("Progress").setCellRenderer(new ProgressRenderer());
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(TableColumnEnum.CHECKBOX.getIndex()).setPreferredWidth(24);
		table.getColumnModel().getColumn(TableColumnEnum.VIDEO_NAME.getIndex()).setPreferredWidth(120);
		table.getColumnModel().getColumn(TableColumnEnum.DESTINATION.getIndex()).setPreferredWidth(300);
		table.getColumnModel().getColumn(TableColumnEnum.PROGRESS.getIndex()).setPreferredWidth(73);
		table.getColumnModel().getColumn(TableColumnEnum.STATUS.getIndex()).setPreferredWidth(82);
		table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredSize().width, 300));
		table.setFillsViewportHeight(true);
		
		TaskStatusModel.getinstance().setCheckBoxSelectedCallback(this::removeSwitch);
		
	}

	
	private JPanel formatPanel() {
		JPanel root = new JPanel(new BorderLayout());
		JPanel formats = new JPanel();
		formats.add(Box.createHorizontalStrut(5));
		formats.add(format);
		formats.add(cb_format);
		formats.add(Box.createHorizontalStrut(15));
		formats.add(quality_icon);
		formats.add(quality);
		formats.add(cb_quality);
		root.add(formats, BorderLayout.WEST);
		JPanel mode = new JPanel();
		mode.add(modeSwitch);
		root.add(mode, BorderLayout.EAST);
		return root;
	}
	
	private JPanel savetoPanel() {
		JPanel root = new JPanel(new BorderLayout());
		
		JPanel saveTo = new JPanel(new GridBagLayout());
		saveTo.add(Box.createHorizontalStrut(15));
		saveTo.add(path, new GridBagConstraints());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        saveTo.add(Box.createHorizontalStrut(5));
        saveTo.add(pathField, gbc);
        saveTo.add(Box.createHorizontalStrut(10));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
		buttons.add(browse);
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(openSaveDir);
		buttons.add(Box.createHorizontalStrut(5));
		
		root.add(saveTo, BorderLayout.CENTER);
		root.add(buttons, BorderLayout.EAST);
		return root;
	}
	
	private JPanel fileNamePanel() {
		JPanel root = new JPanel(new BorderLayout());
		JPanel nameForm = new JPanel();
		
		nameForm.add(Box.createHorizontalStrut(5));
		nameForm.add(nameFormat);
		nameForm.add(nameFormatField);
		nameForm.add(Box.createHorizontalStrut(5));
		nameForm.add(nameFormatHelp);
		
		JPanel playListOpt = new JPanel();
		playListOpt.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 5));
		playListOpt.add(playList);
		playListOpt.add(cb_playList);
		playListOpt.add(Box.createHorizontalStrut(5));
		
		root.add(nameForm, BorderLayout.WEST);
		root.add(playListOpt, BorderLayout.EAST);
		return root;
	}
	
	private JComponent tablePanel() {
		JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JPanel root = new JPanel(new BorderLayout(5, 5));
		root.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		root.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		root.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);
		root.add(scrollPane, BorderLayout.CENTER);
		return root;
	}
	
	private JPanel bottomPanel() {
		JPanel root = new JPanel(new FlowLayout(FlowLayout.CENTER));
		root.add(cleanCompleted);
		root.add(removeSwitch);
		root.add(cb_clipboardOption);
		root.add(openAppFolder);
		return root;
	}
	
	private void removeSwitch(boolean selectedMode) {
		if(selectedMode) {
			removeSwitch.setText("remove selected");
			Arrays.stream(removeSwitch.getActionListeners()).forEach(removeSwitch::removeActionListener);
			removeSwitch.addActionListener((e) -> { 
				removeSwitch(!TaskStatusModel.getinstance().removeSelected());	});
		} else {
			removeSwitch.setText("clear All");
			Arrays.stream(removeSwitch.getActionListeners()).forEach(removeSwitch::removeActionListener);
			removeSwitch.addActionListener((e) -> { TaskStatusModel.getinstance().clearAll(); });
		}
	}
	
	public void disposeAll() {

		logger.logVerbose("");
		logger.logVerbose("Existing Windows are :");
		Stream.of(JWindow.getWindows()).map(Window::toString).forEach(logger::logVerbose);
		logger.logVerbose("");

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
	
	
	private static Image getICON() {
		final File f = new File(YoutubeClipboardAutoDownloader.getProjectPath().replace(File.separator, "/") + "/icon.png");
		try {
			return ImageIO.read(f);
		} catch (IOException e) {
			SwingDialogs.warning("Unable to find the icon image file!", "%e%\n" + f.getAbsolutePath() + "\nDoes not exist! Default Java icon will be used...", e, false);
			return null;
		}
	}
	
}
