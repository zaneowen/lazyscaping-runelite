package com.lazyscaping.ui;

import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.SlayerSpot;
import com.lazyscaping.api.SlayerTask;
import com.lazyscaping.api.SlayerTaskRef;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** The current Slayer task, where and how to do it, and the gear setup for it. */
class SlayerTab extends JPanel
{
	private static final SlayerTaskRef NO_TASK = new SlayerTaskRef(null, "Choose a task", List.of());

	private final JLabel status = Components.label("");
	private final JComboBox<SlayerTaskRef> taskBox = new JComboBox<>();
	private final JPanel taskInfo = Components.column();
	private final JCheckBox helmetBox = new JCheckBox("Plan with my Slayer helmet (i)");
	private final GearSetupView setupView;
	private boolean updating;

	SlayerTab(PanelActions actions, IconLoader icons)
	{
		super(new BorderLayout());
		setOpaque(false);
		setupView = new GearSetupView(GearTarget.SLAYER, actions, icons);

		Components.configureLongList(taskBox, NO_TASK);
		taskBox.addActionListener(e ->
		{
			if (!updating)
			{
				setupView.resetStyle();
				SlayerTaskRef task = (SlayerTaskRef) taskBox.getSelectedItem();
				actions.taskSelected(task == null ? null : task.getId());
			}
		});

		helmetBox.setOpaque(false);
		helmetBox.setFont(FontManager.getRunescapeSmallFont());
		helmetBox.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		helmetBox.setToolTipText("Ticked automatically when you own one. Tick it if yours is somewhere the plugin can't see, like your house.");
		helmetBox.addActionListener(e ->
		{
			if (!updating)
			{
				actions.slayerHelmetChanged(helmetBox.isSelected());
			}
		});

		JPanel taskCard = Components.card("Slayer task");
		taskCard.add(status);
		taskCard.add(Components.gap(4));
		taskCard.add(Components.fullWidth(taskBox));
		taskCard.add(taskInfo);

		JPanel gearCard = Components.card("Gear for this task");
		gearCard.add(helmetBox);
		gearCard.add(setupView.getComponent());

		JPanel content = Components.column();
		content.add(taskCard);
		content.add(Components.gap(8));
		content.add(gearCard);
		add(content, BorderLayout.NORTH);
		setCurrentTask(null);
	}

	GearSetupView getSetupView()
	{
		return setupView;
	}

	void setTaskOptions(List<SlayerTaskRef> tasks)
	{
		SlayerTaskRef selected = (SlayerTaskRef) taskBox.getSelectedItem();
		updating = true;
		taskBox.removeAllItems();
		taskBox.addItem(NO_TASK);
		tasks.stream()
			.sorted(Comparator.comparing(SlayerTaskRef::getName, String.CASE_INSENSITIVE_ORDER))
			.forEach(taskBox::addItem);
		updating = false;
		selectTask(selected == null ? null : selected.getId());
	}

	void selectTask(String taskId)
	{
		updating = true;
		taskBox.setSelectedItem(NO_TASK);
		for (int i = 0; i < taskBox.getItemCount(); i++)
		{
			if (Objects.equals(taskBox.getItemAt(i).getId(), taskId))
			{
				taskBox.setSelectedIndex(i);
				break;
			}
		}
		setupView.resetStyle();
		updating = false;
	}

	void setSlayerHelmet(boolean planWithHelmet)
	{
		updating = true;
		helmetBox.setSelected(planWithHelmet);
		updating = false;
	}

	/** Shows the task the game has assigned, or prompts to pick one when there's none. */
	void setCurrentTask(String description)
	{
		status.setText(description == null
			? Html.muted("No task assigned. Pick one to plan for it.")
			: Html.white("Current task:") + " " + Html.escape(description));
	}

	void showSetup(GearSetup setup)
	{
		showTaskInfo(setup.getTask());
		setupView.showSetup(setup);
	}

	void showMessage(String message)
	{
		showTaskInfo(null);
		setupView.showMessage(message);
	}

	void showError(String message)
	{
		showTaskInfo(null);
		setupView.showError(message);
	}

	private void showTaskInfo(SlayerTask task)
	{
		taskInfo.removeAll();
		if (task != null)
		{
			addRequirements(task);
			addSpots(task);
			addMore(task);
		}
		Components.refresh(taskInfo);
	}

	private void addRequirements(SlayerTask task)
	{
		List<String> requirements = new ArrayList<>();
		if (task.getSlayer() > 1)
		{
			requirements.add(task.getSlayer() + " Slayer");
		}
		if (task.getRequires() != null)
		{
			requirements.add(task.getRequires());
		}
		if (!requirements.isEmpty() || !task.getNeeds().isEmpty())
		{
			taskInfo.add(Components.gap(4));
		}
		if (!requirements.isEmpty())
		{
			taskInfo.add(Components.fact("Needs", lines(requirements)));
		}
		if (!task.getNeeds().isEmpty())
		{
			taskInfo.add(Components.fact("Bring", lines(task.getNeeds())));
		}
	}

	private static String lines(List<String> items)
	{
		return items.stream().map(Html::escape).collect(Collectors.joining("<br>"));
	}

	private void addSpots(SlayerTask task)
	{
		if (task.getSpots().isEmpty())
		{
			return;
		}
		taskInfo.add(Components.subheading("Where to kill them"));
		task.getSpots().forEach(spot -> taskInfo.add(spotEntry(spot)));
	}

	/** Tips, other places, masters and skip advice, behind a link to keep the tab short. */
	private void addMore(SlayerTask task)
	{
		JPanel more = Components.column();
		addList(more, "Tips", task.getTips());
		if (!task.getOtherPlaces().isEmpty())
		{
			more.add(Components.spaced(Components.label(Html.white("Also found at:") + " " + Html.escape(String.join(", ", task.getOtherPlaces()))), 6));
		}
		if (!task.getMasters().isEmpty())
		{
			more.add(Components.spaced(Components.label(Html.white("Assigned by:") + " " + Html.escape(String.join(", ", task.getMasters()))), 6));
		}
		if (task.getSkip() != null)
		{
			more.add(Components.spaced(Components.label(Html.white("Rather not?") + " " + Html.escape(task.getSkip())), 6));
		}
		if (more.getComponentCount() > 0)
		{
			JLabel header = Components.spaced(Components.label(Html.color(Html.GOLD, Html.underline("Tips and more"))), 8);
			taskInfo.add(Components.expandable(header, more));
		}
	}

	/** A place to kill the task: its name, what it's like there, then how to do it. */
	private static JPanel spotEntry(SlayerSpot spot)
	{
		List<String> tags = new ArrayList<>();
		if (spot.isMulti())
		{
			tags.add("Multi-combat");
		}
		if (spot.isCannon())
		{
			tags.add("Cannon");
		}
		if (spot.isWilderness())
		{
			tags.add(Html.color(Html.RED, "Wilderness"));
		}

		JPanel entry = Components.entry();
		entry.add(Components.label(Html.color(Html.GOLD, Html.escape(spot.getWhere())), Components.ENTRY_WIDTH));
		if (!tags.isEmpty())
		{
			entry.add(Components.label(Html.muted(String.join(" · ", tags)), Components.ENTRY_WIDTH));
		}
		entry.add(Components.spaced(Components.label(Html.escape(spot.getHow()), Components.ENTRY_WIDTH), 2));
		return entry;
	}

	private static void addList(JPanel panel, String title, List<String> items)
	{
		if (items.isEmpty())
		{
			return;
		}
		StringBuilder html = new StringBuilder(Html.white(title + ":"));
		items.forEach(item -> html.append("<br>- ").append(Html.escape(item)));
		panel.add(Components.spaced(Components.label(html.toString()), 6));
	}
}
