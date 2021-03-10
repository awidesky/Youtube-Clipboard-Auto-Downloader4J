package gui;

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
	private JButton btn_browse, btn_cleanCompleted, btn_cleanAll;
	private JLabel tlb_format, tlb_quality, tlb_path;
	private JTextField jft_path;
	private JComboBox<String> cb_format, cb_quality;
	private JFileChooser jfc = new JFileChooser();
	private JTable table;
	private static final String[] table_header = { "Video", "Destination", "Status", "Progress" };
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

		tlb_format = new JLabel("Format :");
		tlb_quality = new JLabel("Audio Quality :");
		tlb_path = new JLabel("Save to :");
		jft_path = new JTextField(Main.getProperties().getSaveto());

		jft_path.addActionListener((e) -> {

			Main.getProperties().setSaveto(jft_path.getText());

		});

		btn_browse = new JButton("Browse...");
		btn_browse.addActionListener((e) -> {

			if (jfc.showDialog(new JFrame(), null) != JFileChooser.APPROVE_OPTION) {
				JOptionPane.showMessageDialog(null, "Please choose a directory!", "ERROR!",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			String path = jfc.getSelectedFile().getAbsolutePath();
			Main.getProperties().setSaveto(path);
			jft_path.setText(path);
			jfc.setCurrentDirectory(new File(path));

		});
		btn_cleanCompleted = new JButton("clean completed"); //TODO: listner
		btn_cleanAll = new JButton("clean all");

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
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(1);
		
		scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(8, 122, 600, 280);

		btn_browse.setBounds(523, 76, btn_browse.getPreferredSize().width, btn_browse.getPreferredSize().height);
		btn_cleanCompleted.setBounds(14, 418, btn_cleanCompleted.getPreferredSize().width, btn_cleanCompleted.getPreferredSize().height);
		btn_cleanAll.setBounds(142, 418, btn_cleanAll.getPreferredSize().width, btn_cleanAll.getPreferredSize().height);
		
		tlb_format.setBounds(26, 23, tlb_format.getPreferredSize().width, tlb_format.getPreferredSize().height);
		tlb_quality.setBounds(273, 23, tlb_quality.getPreferredSize().width, tlb_quality.getPreferredSize().height);
		tlb_path.setBounds(14, 80, tlb_path.getPreferredSize().width, tlb_path.getPreferredSize().height);

		jft_path.setBounds(65, 76, 456, 22);

		cb_format.setBounds(83, 19, 96, 22);
		cb_quality.setBounds(365, 19, 150, 22);

		mainFrame.add(btn_browse);

		mainFrame.add(tlb_format);
		mainFrame.add(tlb_path);
		mainFrame.add(tlb_quality);

		mainFrame.add(jft_path);

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

	public void addTaskModel(TaskStatusViewerModel t) {

		// TODO : give t a whenDone object
		// TODO : give t a processUpdater object
		// TODO : put t to Table

	}
	

}
