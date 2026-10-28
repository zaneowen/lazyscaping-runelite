package com.lazyscaping;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/** Colours, formatting and small building blocks shared by the panel's tabs. */
final class Ui
{
	static final Color READY = new Color(0x7bc96f);
	static final Color GOLD = new Color(0xd9a23a);
	static final Color MISSING = new Color(0xe0605a);
	static final String MUTED = "#a8a29e";
	static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.ROOT);

	/** Space around a card's contents. */
	private static final int CARD_PADDING = 8;
	/**
	 * Width text wraps at inside a card, in CSS px: the panel minus its border, the
	 * card's padding and some slack. Swing's HTML renderer draws CSS px about 4/3
	 * larger, hence the scaling.
	 */
	static final int WRAP_WIDTH = (PluginPanel.PANEL_WIDTH - 30 - 2 * CARD_PADDING) * 3 / 4;

	private Ui()
	{
	}

	/** Stacks components top to bottom, all left-aligned (BoxLayout misplaces mixed alignments). */
	static JPanel column()
	{
		JPanel panel = new JPanel()
		{
			@Override
			protected void addImpl(Component comp, Object constraints, int index)
			{
				if (comp instanceof JComponent)
				{
					((JComponent) comp).setAlignmentX(LEFT_ALIGNMENT);
				}
				super.addImpl(comp, constraints, index);
			}
		};
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		return panel;
	}

	/** A shaded box with a title, for one section of a tab. */
	static JPanel card(String title)
	{
		JPanel card = column();
		card.setOpaque(true);
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));
		JLabel heading = new JLabel(title);
		heading.setFont(FontManager.getRunescapeBoldFont());
		heading.setForeground(Color.WHITE);
		heading.setBorder(new EmptyBorder(0, 0, 6, 0));
		card.add(heading);
		return card;
	}

	/** Stretches a component across the panel. */
	static JComponent full(JComponent c)
	{
		JPanel row = new JPanel(new GridLayout(1, 1));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 0, 2, 0));
		row.add(c);
		return row;
	}

	static JComponent gap(int height)
	{
		JPanel gap = new JPanel();
		gap.setOpaque(false);
		gap.setBorder(new EmptyBorder(height, 0, 0, 0));
		gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		return gap;
	}

	/** Adds space above a component. */
	static JComponent spaced(JComponent c, int top)
	{
		c.setBorder(new EmptyBorder(top, 0, 0, 0));
		return c;
	}

	static JLabel subheading(String text)
	{
		return (JLabel) spaced(wrapped("<font color='#ffffff'>" + escape(text) + "</font>"), 10);
	}

	/** Text (which may contain HTML) wrapped to a width in CSS px. */
	static String html(String text, int width)
	{
		return "<html><body style='width:" + width + "px'>" + text + "</body></html>";
	}

	static JLabel wrapped(String text)
	{
		return wrapped(text, WRAP_WIDTH);
	}

	/** A label whose text wraps to a width. Text may contain HTML. */
	static JLabel wrapped(String text, int width)
	{
		JLabel label = new JLabel(html(text, width))
		{
			@Override
			public void setText(String text)
			{
				super.setText(text == null || text.isEmpty() || text.startsWith("<html>") ? text : html(text, width));
			}
		};
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	/** A button whose label wraps instead of being cut off. */
	static JButton button(String text)
	{
		return button(text, WRAP_WIDTH);
	}

	/** A button whose label wraps instead of being cut off, in a space `width` wide (CSS px). */
	static JButton button(String text, int width)
	{
		// Narrower than other text, to leave room for the button's padding.
		return new JButton("<html><body style='width:" + (width - 15) + "px;text-align:center'>" + escape(text) + "</body></html>");
	}

	/** Makes a label act like a link. */
	static void onClick(JLabel label, Runnable action)
	{
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				action.run();
			}
		});
	}

	/** A header that shows or hides its details when clicked. */
	static JComponent expandable(JLabel header, JComponent details)
	{
		JPanel box = column();
		details.setVisible(false);
		header.setToolTipText("Click for details");
		onClick(header, () ->
		{
			details.setVisible(!details.isVisible());
			box.revalidate();
			box.repaint();
		});
		box.add(header);
		box.add(details);
		return box;
	}

	/**
	 * Replaces a list's contents with a plain-text message (escaped, since it may
	 * come from the site), or empties it when the message is null.
	 */
	static void showMessage(JPanel list, String message)
	{
		list.removeAll();
		if (message != null)
		{
			list.add(wrapped(escape(message)));
		}
		refresh(list);
	}

	static void refresh(JComponent changed)
	{
		changed.revalidate();
		changed.repaint();
	}

	static String colored(Color color, String html)
	{
		return "<font color='" + hex(color) + "'>" + html + "</font>";
	}

	static String muted(String html)
	{
		return "<font color='" + MUTED + "'>" + html + "</font>";
	}

	static String white(String html)
	{
		return "<font color='#ffffff'>" + html + "</font>";
	}

	static String escape(String text)
	{
		return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String hex(Color c)
	{
		return String.format("#%06x", c.getRGB() & 0xffffff);
	}

	static String hours(double hours)
	{
		long minutes = Math.round(hours * 60);
		if (minutes < 60)
		{
			return Math.max(1, minutes) + "m";
		}
		return minutes / 60 + "h " + String.format(Locale.ROOT, "%02d", minutes % 60) + "m";
	}

	static String gp(long gp)
	{
		if (gp >= 1_000_000_000)
		{
			return String.format(Locale.ROOT, "%.2fB gp", gp / 1_000_000_000.0);
		}
		if (gp >= 1_000_000)
		{
			return String.format(Locale.ROOT, "%.2fM gp", gp / 1_000_000.0);
		}
		if (gp >= 10_000)
		{
			return String.format(Locale.ROOT, "%.1fk gp", gp / 1_000.0);
		}
		return NUMBER.format(gp) + " gp";
	}
}
