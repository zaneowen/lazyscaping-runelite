package com.lazyscaping.ui;

import com.lazyscaping.api.Boss;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 * Pick a boss to see the best gear setup from what you own, and what a budget
 * buys. Bosses are listed by name, narrowed to one of the site's boss groups.
 */
class BossingTab extends JPanel
{
	private static final String ALL_BOSSES = "All bosses";
	private static final Boss NO_BOSS = new Boss(null, "Choose a boss", null);

	private final JComboBox<String> groupBox = new JComboBox<>();
	private final JComboBox<Boss> bossBox = new JComboBox<>();
	private final GearSetupView setupView;
	private List<Boss> bosses = Collections.emptyList();
	private boolean updating;

	BossingTab(PanelActions actions, IconLoader icons)
	{
		super(new BorderLayout());
		setOpaque(false);
		setupView = new GearSetupView(GearTarget.BOSS, actions, icons);

		groupBox.addItem(ALL_BOSSES);
		// sized by the shortest entry, so the longer group names don't widen the panel
		groupBox.setPrototypeDisplayValue(ALL_BOSSES);
		groupBox.setToolTipText("Narrow the boss list to one kind of boss");
		groupBox.addActionListener(e ->
		{
			if (!updating)
			{
				updating = true;
				fillBosses((Boss) bossBox.getSelectedItem());
				updating = false;
			}
		});

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
		card.add(Components.labeledRow("Type", groupBox));
		card.add(Components.gap(4));
		card.add(Components.labeledRow("Boss", bossBox));
		card.add(setupView.getComponent());
		add(card, BorderLayout.NORTH);
	}

	GearSetupView getSetupView()
	{
		return setupView;
	}

	/** Sets the bosses to pick from, and the groups they're in, in the order to list them. */
	void setBossOptions(List<Boss> bosses, List<String> groups)
	{
		Boss selected = (Boss) bossBox.getSelectedItem();
		Object group = groupBox.getSelectedItem();
		this.bosses = new ArrayList<>(bosses);
		this.bosses.sort(Comparator.comparing(Boss::getName, String.CASE_INSENSITIVE_ORDER));

		updating = true;
		groupBox.removeAllItems();
		groupBox.addItem(ALL_BOSSES);
		groups.stream()
			.filter(g -> bosses.stream().anyMatch(b -> g.equals(b.getGroup())))
			.forEach(groupBox::addItem);
		groupBox.setSelectedItem(group);
		updating = false;
		selectBoss(selected == null ? null : selected.getId());
	}

	/** Shows a boss as picked, switching to its group when another group is showing. */
	void selectBoss(String bossId)
	{
		updating = true;
		Boss boss = bosses.stream().filter(b -> Objects.equals(b.getId(), bossId)).findFirst().orElse(null);
		if (boss != null && !inSelectedGroup(boss))
		{
			groupBox.setSelectedItem(boss.getGroup() != null ? boss.getGroup() : ALL_BOSSES);
		}
		fillBosses(boss);
		setupView.resetStyle();
		updating = false;
	}

	/** Lists the selected group's bosses, keeping {@code selected} picked if it's among them. */
	private void fillBosses(Boss selected)
	{
		bossBox.removeAllItems();
		bossBox.addItem(NO_BOSS);
		bosses.stream().filter(this::inSelectedGroup).forEach(bossBox::addItem);
		bossBox.setSelectedItem(NO_BOSS);
		for (int i = 0; selected != null && i < bossBox.getItemCount(); i++)
		{
			if (Objects.equals(bossBox.getItemAt(i).getId(), selected.getId()))
			{
				bossBox.setSelectedIndex(i);
				break;
			}
		}
	}

	private boolean inSelectedGroup(Boss boss)
	{
		Object group = groupBox.getSelectedItem();
		return group == null || ALL_BOSSES.equals(group) || group.equals(boss.getGroup());
	}
}
