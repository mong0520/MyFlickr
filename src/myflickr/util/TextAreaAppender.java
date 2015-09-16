/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.util;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

public class TextAreaAppender extends WriterAppender {

	static private JTextArea jTextArea = null;

	/** Set the target JTextArea for the logging information to appear. */
	static public void setTextArea(JTextArea jTextArea) {
		TextAreaAppender.jTextArea = jTextArea;
	}
	/**
	 * Format and then append the loggingEvent to the stored
	 * JTextArea.
	 */
    @Override
	public void append(LoggingEvent event) {
		final String message = this.layout.format(event);

		// Append formatted message to textarea using the Swing Thread.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
                            if(jTextArea != null)
				jTextArea.append(message);
			}
		});
	}
}