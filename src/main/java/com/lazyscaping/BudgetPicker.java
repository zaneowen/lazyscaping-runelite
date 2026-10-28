package com.lazyscaping;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * A labelled budget picker: a preset, "My coins", or a typed amount like "25m",
 * with a note under it for the coins it stands for or a typing mistake.
 */
class BudgetPicker extends JPanel
{
	private final JComboBox<String> box;
	private final JLabel note = Ui.wrapped("");
	/** Set while the budget is changed in code, so it isn't reported as the player's choice. */
	private boolean setting;
	private long coins;

	/** `changed` gets valid budgets the player picks or types. */
	BudgetPicker(String[] presets, String tooltip, Consumer<String> changed)
	{
		setLayout(new BorderLayout());
		setOpaque(false);
		box = new JComboBox<>(presets);
		box.setEditable(true);
		box.setToolTipText(tooltip + ". Pick one, or type an amount like 25m.");
		box.addActionListener(e ->
		{
			if (setting)
			{
				return;
			}
			updateNote();
			String text = text();
			if (Budget.isValid(text))
			{
				changed.accept(text);
			}
		});

		JLabel label = new JLabel("Budget");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(label, BorderLayout.WEST);
		row.add(box, BorderLayout.CENTER);

		JPanel stack = Ui.column();
		stack.add(Ui.full(row));
		stack.add(note);
		add(stack, BorderLayout.CENTER);
		updateNote();
	}

	/** Shows a budget without reporting it back as a change. */
	void setBudget(String budget)
	{
		setting = true;
		box.setSelectedItem(budget);
		setting = false;
		updateNote();
	}

	/** The coins "My coins" stands for. */
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
			note.setText(Ui.colored(Ui.MISSING, "Type an amount like 25m, or Unlimited."));
		}
		else if (Budget.isMyCoins(text))
		{
			note.setText(Ui.gp(coins) + " in your bank and inventory" + (coins == 0 ? " (open your bank once)" : ""));
		}
		else
		{
			note.setText("");
		}
		note.setVisible(!note.getText().isEmpty());
	}
}
