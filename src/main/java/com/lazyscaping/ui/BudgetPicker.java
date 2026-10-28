package com.lazyscaping.ui;

import com.lazyscaping.Budget;
import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** An editable budget picker: a preset, "My coins", or a typed amount like "25m". */
class BudgetPicker extends JPanel
{
	private final JComboBox<String> box;
	private final JLabel note = Components.label("");
	private boolean updating;
	private long coins;

	BudgetPicker(String[] presets, String tooltip, Consumer<String> onChange)
	{
		super(new BorderLayout());
		setOpaque(false);

		box = new JComboBox<>(presets);
		box.setEditable(true);
		box.setToolTipText(tooltip + ". Pick one, or type an amount like 25m.");
		box.addActionListener(e ->
		{
			if (updating)
			{
				return;
			}
			updateNote();
			if (Budget.isValid(text()))
			{
				onChange.accept(text());
			}
		});

		JLabel label = new JLabel("Budget");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(label, BorderLayout.WEST);
		row.add(box, BorderLayout.CENTER);

		JPanel content = Components.column();
		content.add(Components.fullWidth(row));
		content.add(note);
		add(content, BorderLayout.CENTER);
		updateNote();
	}

	void setBudget(String budget)
	{
		updating = true;
		box.setSelectedItem(budget);
		updating = false;
		updateNote();
	}

	/** The amount "My coins" stands for. */
	void setCoins(long coins)
	{
		this.coins = coins;
		updateNote();
	}

	private String text()
	{
		Object value = box.getSelectedItem();
		return value == null ? "" : value.toString().trim();
	}

	private void updateNote()
	{
		String text = text();
		if (!Budget.isValid(text))
		{
			note.setText(Html.color(Html.RED, "Type an amount like 25m, or Unlimited."));
		}
		else if (Budget.isMyCoins(text))
		{
			note.setText(Format.gp(coins) + " in your bank and inventory" + (coins == 0 ? " (open your bank once)" : ""));
		}
		else
		{
			note.setText("");
		}
		note.setVisible(!note.getText().isEmpty());
	}
}
