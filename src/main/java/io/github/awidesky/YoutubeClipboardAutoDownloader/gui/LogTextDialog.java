package io.github.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import io.github.awidesky.guiUtil.AbstractLogger;
import io.github.awidesky.guiUtil.Logger;

public class LogTextDialog extends JDialog {
	
	private static final long serialVersionUID = 449601573420880012L;
	private final Logger log;
	private final JTextArea text = new JTextArea();
	
	public LogTextDialog(String[] updateCommands, Logger logger) {
		this.log = new AbstractLogger() {
			
			@Override
			public void close() throws IOException {
				log.close();
			}
			
			@Override
			public void newLine() {
				log("\n");
			}
			
			@Override
			public void log(String data) {
				logger.log(data);
				SwingUtilities.invokeLater(() -> {
					text.append(data);
					text.append("\n");
				});
			}
		};
		
		text.setEditable(false);
		text.setLineWrap(true);
		JScrollPane p = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		p.setPreferredSize(new Dimension(600, 400));
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(p, BorderLayout.CENTER);
		this.add(panel);
		add(panel);
		setTitle(Arrays.stream(updateCommands).collect(Collectors.joining(" ")));
		setAlwaysOnTop(true); //TODO : maybe false? check if it's hid when false
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
	}

	public Logger getLogger() {
		return log;
	}
}
