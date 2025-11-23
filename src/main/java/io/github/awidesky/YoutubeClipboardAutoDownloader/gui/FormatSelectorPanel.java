package io.github.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import io.github.awidesky.YoutubeClipboardAutoDownloader.Config;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.OSUtil;

public class FormatSelectorPanel {
	
	private JPanel simple, complex;
	private JLabel format, quality_icon;
	private JComboBox<String> cb_format, cb_quality;

	private DefaultComboBoxModel<String> audioFormatCBoxModel = new DefaultComboBoxModel<>(new String[] { "mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav" });
	private DefaultComboBoxModel<String> videoFormatCBoxModel = new DefaultComboBoxModel<>(new String[] { "mp4", "webm", "3gp", "flv" });
	private DefaultComboBoxModel<String> audioQualityCBoxModel = new DefaultComboBoxModel<>(new String[] { "0(best)", "1", "2", "3", "4", "5", "6", "7", "8", "9(worst)" });
	private DefaultComboBoxModel<String> videoQualityCBoxModel = new DefaultComboBoxModel<>(new String[] { "best", "240p", "360p", "360p", "480p", "720p", "1080p", "1440p", "2160p" });
	
	private boolean audioMode = true;
	private boolean isSimple = true;

	
	public FormatSelectorPanel() {
		quality_icon = new JLabel("\uD83C\uDFB5\u0020");
		if(OSUtil.isWindows()) quality_icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
		else if(OSUtil.isMac()) quality_icon.setFont(new Font("Apple Color Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
		else if(OSUtil.isLinux()) quality_icon.setFont(new Font("Noto Color Emoji", Font.PLAIN, new JLabel().getFont().getSize()));
		if("Dialog".equals(quality_icon.getFont().getFontName()) || quality_icon.getFont().canDisplayUpTo("\uD83C\uDF9E\uD83C\uDFB5") != -1) {
			Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
				.filter(f -> f.canDisplayUpTo("\uD83C\uDF9E\uD83C\uDFB5") == -1).findFirst()
				.ifPresent(quality_icon::setFont);
		}
		setSimple();
		setComplex();
	}

	private void setSimple() {
		simple = new JPanel();
		format = new JLabel("Format & Quality :");
		format.setSize(format.getPreferredSize().width, format.getPreferredSize().height);

		cb_format = new JComboBox<>(audioFormatCBoxModel);
		cb_quality = new JComboBox<>(audioQualityCBoxModel);
		cb_format.setEditable(true);
		
		if(videoQualityCBoxModel.getIndexOf(Config.getQuality()) == -1) { //It was audio mode
			cb_quality.setSelectedIndex(Integer.parseInt(Config.getQuality()));
		} else {
			swapMode();
			cb_quality.setSelectedItem(Config.getQuality());
		}
		cb_format.setSelectedItem(Config.getExtension());
		cb_format.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		
		((JTextComponent) cb_format.getEditor().getEditorComponent()).getDocument()
		.addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e)  { Config.setExtension(cb_format.getEditor().getItem().toString()); }
			@Override
			public void insertUpdate(DocumentEvent e)  { Config.setExtension(cb_format.getEditor().getItem().toString()); }
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
		cb_format.setSize(80, 22);
		cb_quality.setSize(100, 22);
		
		simple.add(Box.createHorizontalStrut(5));
		simple.add(quality_icon);
		simple.add(format);
		simple.add(cb_format);
		simple.add(cb_quality);				
	}
	
	private void setComplex() {
		complex = new JPanel();
		complex.add(Box.createHorizontalStrut(5));
		//complex.add(quality_icon);
		//complex.add(format);
	}
	
	public JPanel getPanel() {
		JPanel root = new JPanel(new BorderLayout());
		root.add(isSimple ? simple : complex, BorderLayout.WEST);
		return root;
	}

	String swapMode() {
		if(audioMode) {
			cb_format.setModel(videoFormatCBoxModel);
			cb_quality.setModel(videoQualityCBoxModel);
			quality_icon.setText("\uD83C\uDF9E\u0020");
		} else {
			cb_format.setModel(audioFormatCBoxModel);
			cb_quality.setModel(audioQualityCBoxModel);
			quality_icon.setText("\uD83C\uDFB5\u0020");
		}

		audioMode = !audioMode;

		cb_format.setSelectedIndex(0);
		cb_quality.setSelectedIndex(0);
		return audioMode ? "Mode : Audio" : "Mode : Video";
	}

	public boolean isAudioMode() {
		return audioMode;
	}

	public void getFormat(boolean audioMode, List<String> arguments) {
		if(audioMode) {
			arguments.add("--extract-audio");
			arguments.add("--audio-format");
			arguments.add(Config.getExtension());
			arguments.add("--audio-quality");
			arguments.add(Config.getQuality());
		} else {
			arguments.add("-f");
			arguments.add(getVideoFormat());
		}
	}

	private String getVideoFormat() {
		if (isSimple) {
			String height = "", video = "[ext=" + Config.getExtension() + "]", audio = "";
			if ("mp4".equals(Config.getExtension())) {
				audio = "[ext=m4a]";
			}
			if (!"best".equals(Config.getQuality())) {
				height = "[height<=" + Config.getQuality().replace("p", "") + "]";
			}
			return "bv" + video + height + "+ba" + audio + "/b" + video + height + " / bv*+ba/b";
		} else {
			return "";
		}
	}

}
