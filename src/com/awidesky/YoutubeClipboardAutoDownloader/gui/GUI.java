package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

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

import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;

public class GUI {
	
	
	private JFrame loadingFrame;
	private JLabel tlb_loadingStatus;
	private JProgressBar jpb_initProgress;
	

	private JFrame mainFrame;
	private JButton browse, cleanCompleted, cleanAll;
	private JLabel format, quality, path;
	private JTextField pathField;
	private JComboBox<String> cb_format, cb_quality;
	private JFileChooser jfc = new JFileChooser();
	private JTable table;
	private JScrollPane scrollPane;
	
	
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
		
		/** make <code>loadingFrame</code> */
		loadingFrame = new JFrame();
		loadingFrame.setTitle("loading...");
		loadingFrame.setIconImage(new ImageIcon(
				YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\icon.jpg")
						.getImage());
		loadingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadingFrame.setSize(450, 100); //add more height than fxml because it does not think about title length
		loadingFrame.setLayout(null);
		loadingFrame.setResizable(false);
		
		tlb_loadingStatus = new JLabel("");
		tlb_loadingStatus.setBounds(14, 8, 370, 18);
		
		jpb_initProgress = new JProgressBar();
		jpb_initProgress.setBounds(15, 27, 370, 18);
		
		loadingFrame.add(tlb_loadingStatus);
		loadingFrame.add(jpb_initProgress);
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
		mainFrame.setSize(630, 455);
		mainFrame.setLayout(null);
		mainFrame.setResizable(false);
		mainFrame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				
				e.getWindow().dispose();
				Main.kill();

			}

		});

		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setDialogTitle("Choose directory to save music!");
		jfc.setCurrentDirectory(new File(Main.getProperties().getSaveto()));

		format = new JLabel("Format :");
		quality = new JLabel("Audio Quality :");
		path = new JLabel("Save to :");
		pathField = new JTextField(Main.getProperties().getSaveto());

		pathField.addActionListener((e) -> {

			Main.getProperties().setSaveto(pathField.getText());

		});

		browse = new JButton("Browse...");
		browse.addActionListener((e) -> {

			if (jfc.showDialog(new JFrame(), null) != JFileChooser.APPROVE_OPTION) {
				JOptionPane.showMessageDialog(null, "Please choose a directory!", "ERROR!",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			String path = jfc.getSelectedFile().getAbsolutePath();
			Main.getProperties().setSaveto(path);
			pathField.setText(path);
			jfc.setCurrentDirectory(new File(path));

		});
		cleanCompleted = new JButton("clean completed");
		cleanCompleted.addActionListener((e) -> { TaskStatusModel.getinstance().clearDone(); });
		cleanAll = new JButton("clean all");
		cleanAll.addActionListener((e) -> { TaskStatusModel.getinstance().clearAll(); });

		cb_format = new JComboBox<>(new String[] { "mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav" });
		cb_quality = new JComboBox<>(new String[] { "0(best)", "1", "2", "3", "4", "5", "6", "7", "8", "9(worst)" });

		cb_format.setSelectedItem(Main.getProperties().getFormat());
		cb_quality.setSelectedIndex(Integer.parseInt(Main.getProperties().getQuality()));

		cb_format.addActionListener((e) -> {

			Main.getProperties().setFormat(cb_format.getSelectedItem().toString());

		});

		cb_quality.addActionListener((e) -> {

			Main.getProperties().setQuality(String.valueOf(cb_quality.getSelectedIndex()));

		});

		table = new JTable();
		table.setModel(TaskStatusModel.getinstance());
		table.getColumn("Progress").setCellRenderer(new ProgressRenderer());
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(120);
		table.getColumnModel().getColumn(1).setPreferredWidth(324);
		table.getColumnModel().getColumn(2).setPreferredWidth(73);
		table.getColumnModel().getColumn(3).setPreferredWidth(82);
		
		scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(8, 122, 600, 280);

		browse.setBounds(523, 76, browse.getPreferredSize().width, browse.getPreferredSize().height);
		cleanCompleted.setBounds(14, 418, cleanCompleted.getPreferredSize().width, cleanCompleted.getPreferredSize().height);
		cleanAll.setBounds(142, 418, cleanAll.getPreferredSize().width, cleanAll.getPreferredSize().height);
		
		format.setBounds(26, 23, format.getPreferredSize().width, format.getPreferredSize().height);
		quality.setBounds(273, 23, quality.getPreferredSize().width, quality.getPreferredSize().height);
		path.setBounds(14, 80, path.getPreferredSize().width, path.getPreferredSize().height);

		pathField.setBounds(65, 76, 456, 22);

		cb_format.setBounds(83, 19, 96, 22);
		cb_quality.setBounds(365, 19, 150, 22);

		mainFrame.add(browse);

		mainFrame.add(format);
		mainFrame.add(path);
		mainFrame.add(quality);

		mainFrame.add(pathField);

		mainFrame.add(cb_format);
		mainFrame.add(cb_quality);

		mainFrame.add(scrollPane);

		

		loadingFrame.setVisible(false);
		loadingFrame.dispose();
		mainFrame.setVisible(true);
		
		loadingFrame = null;
		tlb_loadingStatus = null;
		jpb_initProgress = null;
		
	}

	public void setLoadingStat(LoadingStatus stat) {
		
		tlb_loadingStatus.setText(stat.getStatus());
		jpb_initProgress.setValue(stat.getProgress());
		
	}
	
	
	public static void error(String title, String content) {

		JOptionPane.showMessageDialog(null, content, title, JOptionPane.ERROR_MESSAGE);
		Main.log("[GUI.error] " + title + "\n\t" + content);
		
	}

	public static void warning(String title, String content) {

		JOptionPane.showMessageDialog(null, content, title, JOptionPane.WARNING_MESSAGE);
		Main.log("[GUI.warning] " + title + "\n\t" + content);
		
	}


}
