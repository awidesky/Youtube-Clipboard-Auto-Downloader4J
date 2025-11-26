package io.github.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec.ProcessExecutor;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.workers.ProcessIOThreadPool;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;

public class GUI {
	
	private Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	
	private JFrame loadingFrame;
	private JLabel loadingStatus;
	private JProgressBar initProgress;
	
	public static final Image ICON = getICON();

	private JFrame mainFrame;

	private JMenuBar menuBar;
	private JMenu fileMenu, ytdlpMenu, ffmpegMenu;
	private JMenuItem mi_openConfig, mi_showLog, mi_saveFolder, mi_ytdlp, mi_update, mi_addOption, mi_ffmpeg, mi_ffprobe;
	
	private JButton browse, cleanCompleted, removeSwitch, nameFormatHelp, openAppFolder, modeSwitch, openSaveDir;
	private JLabel format, quality_icon, path, nameFormat, playList;
	private JTextField manualFormatField, pathField, nameFormatField;
	private JComboBox<String> cb_format, cb_quality, cb_playList, cb_clipboardOption;
	private JCheckBox chb_editFormat;
	private DefaultComboBoxModel<String> audioFormatCBoxModel = new DefaultComboBoxModel<>(new String[] { "mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav" });
	private DefaultComboBoxModel<String> videoFormatCBoxModel = new DefaultComboBoxModel<>(new String[] { "mp4", "mov", "webm", "3gp", "flv" });
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
				logger.info("LoadingFrame was closed");
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

		setMenubar();
		setFileChooser();
		setLabels();
		setTextFields();
		setButtons();
		setComboBoxes();
		setCheckBoxes();
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
	

	private void setMenubar() {
		menuBar = new JMenuBar();

		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().setAccessibleDescription("File menu");
		
		mi_openConfig = new JMenuItem("Open config.txt", KeyEvent.VK_C);
		mi_openConfig.getAccessibleContext().setAccessibleDescription("Open config.txt");
		mi_openConfig.addActionListener((e) -> {
			try {
				Desktop.getDesktop().open(new File(YoutubeClipboardAutoDownloader.getAppdataPath(), "config.txt"));
			} catch (IOException ex) {
				SwingDialogs.warning("Cannot open directory explorer!",
						"Please open manually " +YoutubeClipboardAutoDownloader.getAppdataPath() + "\n%e%", ex, true);
			}
		});
		mi_showLog = new JMenuItem("Show log", KeyEvent.VK_L);
		mi_showLog.getAccessibleContext().setAccessibleDescription("Show log file");
		mi_showLog.addActionListener((e) -> {
			try {
				LogTextDialog dialog = new LogTextDialog(new File(Main.getLogFile()).getName(), Logger.nullLogger);
				dialog.setVisible(true);
				Logger l = dialog.getLogger();
				Files.lines(Paths.get(Main.getLogFile())).forEach(l::info);
			} catch (IOException ex) {
				SwingDialogs.warning("Cannot open directory explorer!",
						"Please open manually " +YoutubeClipboardAutoDownloader.getAppdataPath() + "\n%e%", ex, true);
			}
		});
		mi_saveFolder = new JMenuItem("Open save folder", KeyEvent.VK_S);
		mi_saveFolder.getAccessibleContext().setAccessibleDescription("Open save folder");
		mi_saveFolder.addActionListener((e) -> { Main.openSaveFolder(); });

		fileMenu.add(mi_openConfig);
		fileMenu.add(mi_showLog);
		fileMenu.add(mi_saveFolder);
		

		ytdlpMenu = new JMenu("yt-dlp");
		ytdlpMenu.getAccessibleContext().setAccessibleDescription("yt-dlp menu");

		mi_ytdlp = new JMenuItem("Run yt-dlp", KeyEvent.VK_Y);
		mi_ytdlp.getAccessibleContext().setAccessibleDescription("Run yt-dlp manually");
		mi_ytdlp.addActionListener((e) -> {
			showTextAreaInputDialog("Enter yt-dlp options, separated in each lines.",
					"Run : " + YoutubeClipboardAutoDownloader.getYtdlpPath() + "ffmpeg",
					"yt-dlp", Main.getLogger("[Run yt-dlp] "));
		});
		
		mi_update = new JMenuItem("Update yt-dlp", KeyEvent.VK_U);
		mi_update.getAccessibleContext().setAccessibleDescription("Update yt-dlp");
		mi_update.addActionListener((e) -> {
			ProcessIOThreadPool.submit(() ->
				YoutubeClipboardAutoDownloader.updateYtdlp(YoutubeClipboardAutoDownloader.getYtdlpPath() + "yt-dlp", logger)
			);
		});
		
		mi_addOption = new JMenuItem("Add option", KeyEvent.VK_A);
		mi_addOption.getAccessibleContext().setAccessibleDescription("Add yt-dlp options");
		mi_addOption.addActionListener((e) -> {
			//TODO : implement
		});
		
		ytdlpMenu.add(mi_ytdlp);
		ytdlpMenu.add(mi_update);
		ytdlpMenu.add(mi_addOption);
		
		
		ffmpegMenu = new JMenu("ffmpeg");
		ffmpegMenu.getAccessibleContext().setAccessibleDescription("ffmpeg menu");
		
		mi_ffmpeg = new JMenuItem("Run ffmpeg", KeyEvent.VK_F);
		mi_ffmpeg.getAccessibleContext().setAccessibleDescription("Run ffmpeg with selected file");
		mi_ffmpeg.addActionListener((e) -> {
			showTextAreaInputDialog("Enter ffmpeg options, separated in each lines.",
					"Run : " + YoutubeClipboardAutoDownloader.getYtdlpPath() + "ffmpeg",
					"ffmpeg", Main.getLogger("[Run ffmpeg] "));
		});
		
		mi_ffprobe = new JMenuItem("Run ffprobe", KeyEvent.VK_P);
		mi_ffprobe.getAccessibleContext().setAccessibleDescription("Run ffprobe with selected file");
		mi_ffprobe.addActionListener((e) -> {
			JFileChooser ffprobeFileChooser = new JFileChooser(Config.getSaveto());
			ffprobeFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			ffprobeFileChooser.setDialogTitle("Choose file to run ffprobe");
			if(ffprobeFileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
				File f = ffprobeFileChooser.getSelectedFile();
				ProcessIOThreadPool.submit(() -> {
					LogTextDialog dial = new LogTextDialog("ffprobe " +  f.getName(), Logger.nullLogger);
					dial.setVisible(true);
					try {
						ProcessExecutor.runNow(dial.getLogger(), f.getParentFile(), 
								new File(YoutubeClipboardAutoDownloader.getYtdlpPath(), "ffprobe").getAbsolutePath(),
								"-hide_banner", f.getAbsolutePath());
					} catch (IOException | InterruptedException | ExecutionException ex) {
						SwingDialogs.error("Failed to get ffprobe result of " + f.getAbsolutePath(),
								"%e%", ex, true);
					}
				});
			}
		});
		ffmpegMenu.add(mi_ffmpeg);
		ffmpegMenu.add(mi_ffprobe);

		menuBar.add(fileMenu);
		menuBar.add(ytdlpMenu);
		menuBar.add(ffmpegMenu);

		mainFrame.setJMenuBar(menuBar);
	}
	

	private void setFileChooser() {
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setDialogTitle("Choose directory to save music!");
		jfc.setCurrentDirectory(new File(Config.getSaveto()));
	}
	
	private void setLabels() {
		format = new JLabel("Format :");
		quality_icon = new JLabel("\uD83C\uDFB5\u0020");
		if(OSUtil.isWindows()) quality_icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
		else if(OSUtil.isMac()) quality_icon.setFont(new Font("Apple Color Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
		else if(OSUtil.isLinux()) quality_icon.setFont(new Font("Noto Color Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
		if("Dialog".equals(quality_icon.getFont().getFontName()) || quality_icon.getFont().canDisplayUpTo("\uD83C\uDF9E\uD83C\uDFB5") != -1) {
			Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
				.filter(f -> f.canDisplayUpTo("\uD83C\uDF9E\uD83C\uDFB5") == -1).findFirst()
				.ifPresent(quality_icon::setFont);
		}
		path = new JLabel("Save to :");
		nameFormat = new JLabel("Output : ");
		playList = new JLabel("Playlist : ");
		
		format.setSize(format.getPreferredSize().width, format.getPreferredSize().height);
		path.setSize(path.getPreferredSize().width, path.getPreferredSize().height);
		nameFormat.setSize(nameFormat.getPreferredSize().width, nameFormat.getPreferredSize().height);
		playList.setSize(playList.getPreferredSize().width, playList.getPreferredSize().height);
	}
	
	private void setTextFields() {
		
		manualFormatField = new JTextField(Config.getFormat());
		nameFormatField = new JTextField(Config.getFileNameFormat());
		pathField = new JTextField(Config.getSaveto());
		
		manualFormatField.setEnabled(false);
		nameFormatField.setColumns(10);
		
		manualFormatField.getDocument().addDocumentListener(new DocumentChangeListener(() -> Config.setFormat(manualFormatField.getText())));
		nameFormatField.getDocument().addDocumentListener(new DocumentChangeListener(() -> Config.setFileNameFormat(nameFormatField.getText())));
		pathField.getDocument().addDocumentListener(new DocumentChangeListener(() -> {
			String str = pathField.getText();
			File f = new File(str);
			if (f.isDirectory() && f.exists()) Config.setSaveto(str);
			int w = pathField.getPreferredSize().width - pathField.getWidth();
			if(w <= 0) return;
			w = mainFrame.getX() - w / 2;
			w = w > 0 ? w : 0;
			mainFrame.pack();
			mainFrame.setLocation(dim.width / 2 - mainFrame.getSize().width / 2, mainFrame.getLocation().y);
		}));

	}
	
	private void setButtons() { 
		
		browse = new JButton("browse...");
		cleanCompleted = new JButton("clean completed");
		removeSwitch = new JButton("remove selected");
		nameFormatHelp = new JButton("<= help?");
		openAppFolder = new JButton("open app folder");
		modeSwitch = new JButton("Mode : Audio");
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
			modeSwitch.setText("Mode : Video");
		} else {
			Main.audioMode.set(true);
			cb_format.setModel(audioFormatCBoxModel);
			cb_quality.setModel(audioQualityCBoxModel);
			quality_icon.setText("\uD83C\uDFB5\u0020");
			modeSwitch.setText("Mode : Audio");
		}

		cb_format.setSelectedIndex(0);
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
		cb_format.setSelectedItem(Config.getExtension());
		cb_playList.setSelectedItem(Config.getPlaylistOption().toComboBox());
		cb_clipboardOption.setSelectedItem(Config.getClipboardListenOption().toString());

		cb_format.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		
		((JTextComponent) cb_format.getEditor().getEditorComponent()).getDocument()
		.addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {  Config.setExtension(cb_format.getEditor().getItem().toString()); }
			@Override
			public void insertUpdate(DocumentEvent e) { Config.setExtension(cb_format.getEditor().getItem().toString()); }
			@Override
			public void changedUpdate(DocumentEvent e) { Config.setExtension(cb_format.getEditor().getItem().toString()); }
		});
 
		cb_format.addActionListener((e) -> { 
	        if(cb_format.getSelectedIndex() >= 0) {
	        	Config.setExtension(cb_format.getEditor().getItem().toString());
			} else if("comboBoxEdited".equals(e.getActionCommand())) {
				Config.setExtension(cb_format.getEditor().getItem().toString());
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
	
	private void setCheckBoxes() {
		
		chb_editFormat = new JCheckBox("Override");
		chb_editFormat.addActionListener(e -> {
			boolean checked = chb_editFormat.isSelected();
			
			manualFormatField.setEnabled(checked);
			cb_format.setEnabled(!checked);
			cb_quality.setEnabled(!checked);
			
			Config.isFormatSelectionManual(checked);
		});
		
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
		formats.add(quality_icon);
		formats.add(format);
		formats.add(cb_format);
		formats.add(cb_quality);
		root.add(formats, BorderLayout.WEST);
		
        
		JPanel manual = new JPanel(new GridBagLayout());
		manual.add(Box.createHorizontalStrut(15));
		manual.add(chb_editFormat, new GridBagConstraints());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        manual.add(Box.createHorizontalStrut(5));
        manual.add(manualFormatField, gbc);
        manual.add(Box.createHorizontalStrut(10));
		root.add(manual, BorderLayout.CENTER);
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
		nameForm.add(nameFormatHelp);
		nameForm.add(Box.createHorizontalStrut(5));
		nameForm.add(playList);
		nameForm.add(cb_playList);

		JPanel mode = new JPanel();
		mode.add(modeSwitch);
		
		root.add(nameForm, BorderLayout.WEST);
		root.add(mode, BorderLayout.EAST);
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

		logger.debug();
		logger.debug("Existing Windows are :");
		Stream.of(JWindow.getWindows()).map(Window::toString).forEach(logger::debug);
		logger.debug();

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
	
	private void showTextAreaInputDialog(String message, String title, String executable, Logger l) {
		JTextArea textArea = new JTextArea(10, 30);
	    textArea.setLineWrap(false);
	    JScrollPane scrollPane = new JScrollPane(textArea);
	    scrollPane.setPreferredSize(new Dimension(400, 200));
	
		if (JOptionPane.showConfirmDialog(null, new Object[] { message, scrollPane },
				title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
			
			final String str = textArea.getText();
			ProcessIOThreadPool.submit(() -> {
				List<String> cmd = new LinkedList<>();
				cmd.add(new File(YoutubeClipboardAutoDownloader.getYtdlpPath(), executable).getAbsolutePath());
				cmd.addAll(str.lines().toList());
				String cmdstr = cmd.stream().collect(Collectors.joining(" "));
				l.info("Running independent command :");
				l.info("    \"" + cmdstr + "\""); l.info();
				LogTextDialog dial = new LogTextDialog(cmdstr, l);
				dial.setVisible(true);
				dial.getLogger().info("[COMMAND] " + cmdstr + "\n");
				try {
					ProcessExecutor.runNow(dial.getLogger(), new File(YoutubeClipboardAutoDownloader.getYtdlpPath()), 
							cmd.toArray(String[]::new));
				} catch (IOException | InterruptedException | ExecutionException ex) {
					SwingDialogs.error("Failed to run " + cmdstr, "%e%", ex, true);
				}
				l.info("Independent command execution finished"); l.info();
			});
			
		}
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
