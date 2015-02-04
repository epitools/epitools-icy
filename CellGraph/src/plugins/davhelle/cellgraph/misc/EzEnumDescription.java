package plugins.davhelle.cellgraph.misc;

import icy.system.thread.ThreadUtil;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import plugins.adufour.ezplug.EzComponent;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarListener;
import plugins.davhelle.cellgraph.painters.OverlayEnum;

public class EzEnumDescription extends EzComponent
{

	private JTextArea jTextArea;
	private EzVarEnum<OverlayEnum> overlay;

	/**
	 * Creates a new EzButton with given title and action listener (i.e. the method which will be
	 * called when the button is clicked)
	 * 
	 * @param text
	 *            the text to display
	 * @param textColor
	 *            the default text color
	 */
	public EzEnumDescription(final EzVarEnum<OverlayEnum> overlay_choice)
	{
		
		super("label");
		
		
		ThreadUtil.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				String text = "to be changed";
				jTextArea = new JTextArea(text.isEmpty() ? " " : text);
				jTextArea.setLineWrap(true);
				jTextArea.setWrapStyleWord(true);
				jTextArea.setForeground(Color.BLACK);
				jTextArea.setMargin(new Insets(0, 2, 0, 2));
				jTextArea.setColumns(20);
			}
		}, !SwingUtilities.isEventDispatchThread());
	}

	/**
	 * Sets the text of this label
	 * 
	 * @param text
	 *            the text to display
	 */
	public void setText(String text)
	{
		jTextArea.setText(text);
	}

	/**
	 * Set the color of the text in this label
	 * 
	 * @param textColor
	 *            the new text color
	 */
	public void setColor(Color textColor)
	{
		jTextArea.setForeground(textColor);
	}

	@Override
	protected void addTo(Container container)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 5, 2, 5);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;

		jTextArea.setEditable(false);
		jTextArea.setOpaque(false);

		container.add(jTextArea, gbc);
	}

	@Override
	public void setToolTipText(String text)
	{
		jTextArea.setToolTipText(text);
	}

	/**
	 * Sets the number of columns in this label (this will affect its apparent width).<br/>
	 * NOTE: By default, the number of columns is translated into a width measure by taking the
	 * character "<code>m</code>" as reference (assumed to be the largest possible character). It
	 * does *not* indicate a limit in the number of characters per line, since the width of each
	 * character is potentially different in many fonts.
	 * 
	 * @param nbCols
	 *            the number of columns of this label
	 */
	public void setNumberOfColumns(final int nbCols)
	{
		ThreadUtil.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				jTextArea.setColumns(nbCols);
			}
		});
	}

	/**
	 * Sets the number of rows in this label (this will affect its apparent height)
	 * 
	 * @param nbRows
	 */
	public void setNumberOfRows(final int nbRows)
	{
		ThreadUtil.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				jTextArea.setRows(nbRows);
			}
		});
	}
}


