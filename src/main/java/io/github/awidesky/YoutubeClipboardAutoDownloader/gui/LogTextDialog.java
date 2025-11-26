package io.github.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import io.github.awidesky.guiUtil.AbstractLogger;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.level.Level;

public class LogTextDialog extends JDialog {

	public static final Font MONOSPACED_FONT = getMonospacedFont();
	
	private static final long serialVersionUID = 449601573420880012L;
	private final Logger log;
	private final JTextArea text = new JTextArea();
	
	public LogTextDialog(String title, Logger logger) {
		this.log = new AbstractLogger() {
			
			@Override
			public void close() throws IOException {
				log.close();
			}
			
			@Override
			public void newLine() {
				info("\n");
			}
			
			@Override
			protected void writeString(Level level, CharSequence str) {
				logger.info(str);
				SwingUtilities.invokeLater(() -> {
					text.append(str.toString());
					text.append("\n");
				});
			}
		};

		text.setFont(MONOSPACED_FONT);
		text.setEditable(false);
		text.setLineWrap(true);
		JScrollPane p = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		p.setPreferredSize(new Dimension(600, 400));
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(p, BorderLayout.CENTER);
		this.add(panel);
		add(panel);
		setTitle(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
	}

	public Logger getLogger() {
		return log;
	}
	
	private static Font getMonospacedFont() {
		return new Font(Stream.of("Courier New", "Ubuntu Mono", "Consolas")
				.filter(f ->
					Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
					.anyMatch(f::equals))
				.findFirst().orElse(Font.MONOSPACED),
				Font.PLAIN, new JTextArea().getFont().getSize());
	}
}
