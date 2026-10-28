package com.lazyscaping.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/** Swing building blocks shared by the panel's views. */
final class Components
{
	private static final int CARD_PADDING = 8;
	private static final int LONG_LIST_ROWS = 15;

	/**
	 * Width that text wraps at inside a card, in CSS pixels. Swing renders CSS
	 * pixels at roughly 4/3 scale, hence the conversion.
	 */
	static final int TEXT_WIDTH = (PluginPanel.PANEL_WIDTH - 30 - 2 * CARD_PADDING) * 3 / 4;

	/** The size of an inventory slot's sprite. */
	static final Dimension ICON_SIZE = new Dimension(36, 32);

	/** Width that text beside an item picture wraps at. */
	static final int TEXT_BESIDE_ICON_WIDTH = TEXT_WIDTH - (ICON_SIZE.width + 6) * 3 / 4;

	/** An item slot in a grid: a sprite plus a 1px border each side. */
	private static final Dimension CELL_SIZE = new Dimension(ICON_SIZE.width + 2, ICON_SIZE.height + 2);
	private static final int GRID_GAP = 3;
	/** Width of the labels in front of pickers and facts, so they line up. */
	private static final int PICKER_LABEL_WIDTH = 44;
	/** Width that a fact's value wraps at, beside its label. */
	private static final int FACT_WIDTH = TEXT_WIDTH - (PICKER_LABEL_WIDTH + 4) * 3 / 4;
	/** The line and gap down the left of an {@link #entry}. */
	private static final int ENTRY_INSET = 8;
	/** Width that text in an {@link #entry} wraps at. */
	static final int ENTRY_WIDTH = TEXT_WIDTH - ENTRY_INSET * 3 / 4;

	private Components()
	{
	}

	/** A dropdown or other control with a short label in front of it. */
	static JComponent labeledRow(String label, JComponent control)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.add(rowLabel(label, ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.WEST);
		row.add(control, BorderLayout.CENTER);
		return fullWidth(row);
	}

	/** A short muted label with a value beside it, which wraps under itself rather than under the label. */
	static JComponent fact(String label, String html)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		JLabel name = rowLabel(label, Html.MUTED);
		name.setVerticalAlignment(SwingConstants.TOP);
		row.add(name, BorderLayout.WEST);
		row.add(label(html, FACT_WIDTH), BorderLayout.CENTER);
		return spaced(row, 3);
	}

	private static JLabel rowLabel(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		label.setPreferredSize(new Dimension(PICKER_LABEL_WIDTH, label.getPreferredSize().height));
		return label;
	}

	/** One entry of a list, set off by a line down its left side. */
	static JPanel entry()
	{
		JPanel entry = column();
		entry.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createCompoundBorder(new EmptyBorder(6, 0, 0, 0), BorderFactory.createMatteBorder(0, 2, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR)),
			new EmptyBorder(0, ENTRY_INSET - 2, 0, 0)));
		return entry;
	}

	/** A slot of an item grid, outlined in {@code border}, or blank when {@code border} is null. */
	static JLabel itemCell(Color border)
	{
		JLabel cell = new JLabel();
		cell.setPreferredSize(CELL_SIZE);
		cell.setHorizontalAlignment(SwingConstants.CENTER);
		cell.setOpaque(border != null);
		cell.setBackground(ColorScheme.DARK_GRAY_COLOR);
		if (border != null)
		{
			cell.setBorder(BorderFactory.createLineBorder(border));
		}
		return cell;
	}

	/** A grid of item cells, kept at its own width rather than stretched across the card. */
	static JPanel itemGrid(int columns, List<? extends JComponent> cells)
	{
		JPanel grid = new JPanel(new GridLayout(0, columns, GRID_GAP, GRID_GAP));
		grid.setOpaque(false);
		cells.forEach(grid::add);

		JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		holder.setOpaque(false);
		holder.add(grid);
		return holder;
	}

	/** Small underlined text that runs {@code action} when clicked, for secondary actions. */
	static JLabel textButton(String text, Runnable action)
	{
		JLabel button = new JLabel("<html>" + Html.color(Html.GOLD, Html.underline(Html.escape(text))) + "</html>");
		button.setFont(FontManager.getRunescapeSmallFont());
		onClick(button, action);
		return button;
	}

	/** A transparent panel that stacks its children vertically, left-aligned. */
	static JPanel column()
	{
		JPanel panel = new JPanel()
		{
			@Override
			protected void addImpl(Component comp, Object constraints, int index)
			{
				// BoxLayout misplaces children with mixed alignments
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

	/** A shaded section with a bold title. */
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

	static JComponent fullWidth(JComponent component)
	{
		JPanel row = new JPanel(new GridLayout(1, 1));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 0, 2, 0));
		row.add(component);
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

	static <T extends JComponent> T spaced(T component, int top)
	{
		component.setBorder(new EmptyBorder(top, 0, 0, 0));
		return component;
	}

	static JLabel label(String html)
	{
		return label(html, TEXT_WIDTH);
	}

	/** A small-font label whose HTML wraps to {@code width}. Later {@link JLabel#setText} calls wrap too. */
	static JLabel label(String html, int width)
	{
		JLabel label = new JLabel()
		{
			@Override
			public void setText(String text)
			{
				super.setText(text == null || text.isEmpty() || text.startsWith("<html>") ? text : Html.wrap(text, width));
			}
		};
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setText(html);
		return label;
	}

	static JLabel subheading(String text)
	{
		return spaced(label(Html.white(Html.escape(text))), 10);
	}

	static JButton button(String text)
	{
		return button(text, TEXT_WIDTH);
	}

	/** A button whose label wraps rather than being cut off. */
	static JButton button(String text, int width)
	{
		// leave room for the button's own padding
		return new JButton("<html><body style='width:" + (width - 15) + "px;text-align:center'>" + Html.escape(text) + "</body></html>");
	}

	/** Underlined text that runs {@code action} when clicked. */
	static JLabel link(String text, Runnable action)
	{
		JLabel link = label(Html.color(Html.GOLD, Html.underline(Html.escape(text))));
		onClick(link, action);
		return link;
	}

	static void onClick(JComponent component, Runnable action)
	{
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				action.run();
			}
		});
	}

	/** A clickable header that shows and hides {@code details}. */
	static JComponent expandable(JComponent header, JComponent details)
	{
		JPanel panel = column();
		details.setVisible(false);
		header.setToolTipText("Click for details");
		onClick(header, () ->
		{
			details.setVisible(!details.isVisible());
			panel.revalidate();
			panel.repaint();
		});
		panel.add(header);
		panel.add(details);
		return panel;
	}

	/**
	 * Sets up a dropdown of many named entries: sized by the placeholder rather
	 * than the longest entry so it fits the panel, with long names wrapping in
	 * the open list instead of being cut off.
	 */
	static <T> void configureLongList(JComboBox<T> box, T placeholder)
	{
		box.addItem(placeholder);
		box.setPrototypeDisplayValue(placeholder);
		box.setMaximumRowCount(LONG_LIST_ROWS);
		ListCellRenderer<? super T> renderer = box.getRenderer();
		box.setRenderer((list, value, index, selected, focused) ->
		{
			Component component = renderer.getListCellRendererComponent(list, value, index, selected, focused);
			if (index >= 0 && value != null && component instanceof JLabel)
			{
				((JLabel) component).setText(Html.wrap(Html.escape(value.toString()), TEXT_WIDTH));
			}
			return component;
		});
	}

	/** An empty label sized for an item picture. */
	static JLabel picture()
	{
		JLabel picture = new JLabel();
		picture.setPreferredSize(ICON_SIZE);
		picture.setMinimumSize(ICON_SIZE);
		picture.setHorizontalAlignment(SwingConstants.CENTER);
		picture.setVerticalAlignment(SwingConstants.TOP);
		return picture;
	}

	/** A picture with wrapped text beside it. */
	static JPanel pictureRow(JLabel picture, String html)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(picture, BorderLayout.WEST);
		row.add(label(html, TEXT_BESIDE_ICON_WIDTH), BorderLayout.CENTER);
		return spaced(row, 4);
	}

	/** Replaces a list's contents with a plain-text message, or clears it when {@code message} is null. */
	static void showMessage(JPanel list, String message)
	{
		list.removeAll();
		if (message != null)
		{
			list.add(label(Html.escape(message)));
		}
		refresh(list);
	}

	/** Replaces a list's contents with an error, and a link to try again. */
	static void showError(JPanel list, String message, Runnable retry)
	{
		list.removeAll();
		list.add(label(Html.color(Html.RED, Html.escape(message))));
		list.add(spaced(textButton("Try again", retry), 4));
		refresh(list);
	}

	static void refresh(JComponent component)
	{
		component.revalidate();
		component.repaint();
	}
}
