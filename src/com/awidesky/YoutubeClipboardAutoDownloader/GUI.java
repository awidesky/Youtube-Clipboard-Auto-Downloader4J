package com.awidesky.YoutubeClipboardAutoDownloader;

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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class GUI extends JFrame {

	/**
	 * serial version UID
	 */
	private static final long serialVersionUID = 7447706573178166218L;

	private JButton btn_browse;
	private JLabel tlb_format, tlb_quality, tlb_path;
	private JTextField jft_path;
	private JComboBox<String> cb_format, cb_quality;
	private JFileChooser jfc = new JFileChooser();
	private JTable table;
	private static final String[] table_header = {"Video", "Destination", "Status", "Progress"};
	private JScrollPane scrollPane;
	
	public GUI() {
		
		setTitle("Youtube Audio Auto Downloader " + Main.version);
		setIconImage(new ImageIcon(YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\icon.jpg").getImage());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(630,450);
		setLayout(null);
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				
				Thread t = new Thread(() -> { Main.writeProperties(); });
				t.start(); t.join();
		        	e.getWindow().dispose();
		       	 	System.exit(0);

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
			
			 if (jfc.showDialog(new JFrame(), null) != JFileChooser.APPROVE_OPTION) { JOptionPane.showMessageDialog(null, "Please choose a directory!","ERROR!",JOptionPane.WARNING_MESSAGE); return; }
		        
			 String path = jfc.getSelectedFile().getAbsolutePath();
			 Main.getProperties().setSaveto(path);
		     	 YoutubeAudioDownloader.setDownloadPath(path);
		     	 jft_path.setText(path);
		     	 jfc.setCurrentDirectory(new File(path));
			
		});
		
		cb_format = new JComboBox<>(new String[] {"mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav"});
		cb_quality = new JComboBox<>(new String[] {"0(best)", "1", "2", "3", "4", "5", "6", "7", "8", "9(worst)"});
		
		cb_format.setSelectedItem(Main.getProperties().getFormat());
		cb_quality.setSelectedIndex(Integer.parseInt(Main.getProperties().getQuality()));
		
		cb_format.addActionListener((e) -> {
			
			Main.getProperties().setFormat(cb_format.getSelectedItem().toString());
			
		});
		
		cb_quality.addActionListener((e) -> {
			
			Main.getProperties().setQuality(String.valueOf(cb_quality.getSelectedIndex()));
			
		});
		
		table = new JTable();
		jta_console = new JTextArea();
		jta_console.setEditable(false);
		jta_console.setFont(jta_console.getFont().deriveFont(15f));

		scrollPane = new JScrollPane(jta_console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBounds(8, 122, 600, 278);
		
		btn_browse.setBounds(523, 83, btn_browse.getPreferredSize().width, btn_browse.getPreferredSize().height);
		
		tlb_format.setBounds(26, 23, tlb_format.getPreferredSize().width, tlb_format.getPreferredSize().height);
		tlb_quality.setBounds(273, 23, tlb_quality.getPreferredSize().width, tlb_quality.getPreferredSize().height);
		tlb_path.setBounds(11, 80, tlb_path.getPreferredSize().width, tlb_path.getPreferredSize().height);
		
		jft_path.setBounds(92, 83, 417, 22);
		
		cb_format.setBounds(117, 26, 96, 22);
		cb_quality.setBounds(420, 26, 150, 22);
		
		add(btn_browse);
		
		add(tlb_format);
		add(tlb_path);
		add(tlb_quality);
		
		add(jft_path);
		
		add(cb_format);
		add(cb_quality);
		
		add(scrollPane);
		
	}

	public void show() {

		setVisible(true);

	}


	public static void error(String title, String content) {
		
		JOptionPane.showMessageDialog(null, content ,title,JOptionPane.ERROR_MESSAGE);
		
	}


	public static void warning(String title, String content) {
		
		JOptionPane.showMessageDialog(null, content ,title,JOptionPane.WARNING_MESSAGE);
		
	}


	public void addTaskModel(TaskStatusViewerModel t) {

		//TODO : give t a whenDone object
		//TODO : give t a processUpdater object
		//TODO : put t to Table
		
	}

}
