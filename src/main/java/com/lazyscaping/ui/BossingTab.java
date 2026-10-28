package com.lazyscaping.ui;

import com.lazyscaping.api.Boss;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/** Pick a boss to see the best gear setup from what you own, and what a budget buys. */
class BossingTab extends JPanel
{
	private static final Boss NO_BOSS = new Boss(null, "Choose a boss");

	private final JComboBox<Boss> bossBox = new JComboBox<>();
	private final GearSetupView setupView;
	private boolean updating;

	BossingTab(PanelActions actions, IconLoader icons)
	{
		super(new BorderLayout());
		setOpaque(false);
		setupView = new GearSetupView(GearTarget.BOSS, actions, icons);

		Components.configureLongList(bossBox, NO_BOSS);
		bossBox.addActionListener(e ->
		{
			if (!updating)
			{
				setupView.resetStyle();
				Boss boss = (Boss) bossBox.getSelectedItem();
				actions.bossSelected(boss == null ? null : boss.getId());
			}
		});

		JPanel card = Components.card("Gear setup");
		card.add(Components.label(Html.muted("Pick a boss to see the best setup from your gear.")));
		card.add(Components.gap(4));
		card.add(Components.fullWidth(bossBox));
		card.add(setupView.getComponent());
		add(card, BorderLayout.NORTH);
	}

	GearSetupView getSetupView()
	{
		return setupView;
	}

	void setBossOptions(List<Boss> bosses)
	{
		Boss selected = (Boss) bossBox.getSelectedItem();
		updating = true;
		bossBox.removeAllItems();
		bossBox.addItem(NO_BOSS);
		bosses.forEach(bossBox::addItem);
		updating = false;
		selectBoss(selected == null ? null : selected.getId());
	}

	void selectBoss(String bossId)
	{
		updating = true;
		bossBox.setSelectedItem(NO_BOSS);
		for (int i = 0; i < bossBox.getItemCount(); i++)
		{
			if (Objects.equals(bossBox.getItemAt(i).getId(), bossId))
			{
				bossBox.setSelectedIndex(i);
				break;
			}
		}
		setupView.resetStyle();
		updating = false;
	}
}
