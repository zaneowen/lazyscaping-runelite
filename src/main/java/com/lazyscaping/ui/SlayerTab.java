package com.lazyscaping.ui;

import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.SlayerSpot;
import com.lazyscaping.api.SlayerTask;
import com.lazyscaping.api.SlayerTaskRef;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** The current Slayer task, where and how to do it, and the gear setup for it. */
class SlayerTab extends JPanel
{
	private static final SlayerTaskRef NO_TASK = new SlayerTaskRef(null, "Choose a task", List.of());

	private final JLabel status = Components.label("");
	private final JComboBox<SlayerTaskRef> taskBox = new JComboBox<>();
	private final JPanel taskInfo = Components.column();
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

		JPanel taskCard = Components.card("Slayer task");
		taskCard.add(status);
		taskCard.add(Components.gap(4));
		taskCard.add(Components.fullWidth(taskBox));
		taskCard.add(taskInfo);

		JPanel gearCard = Components.card("Gear for this task");
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
		tasks.forEach(taskBox::addItem);
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

	private void showTaskInfo(SlayerTask task)
	{
		taskInfo.removeAll();
		if (task != null)
		{
			addTaskInfo(task);
		}
		Components.refresh(taskInfo);
	}

	private void addTaskInfo(SlayerTask task)
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
		if (!requirements.isEmpty())
		{
			taskInfo.add(Components.spaced(Components.label(Html.white("Needs:") + " " + Html.escape(String.join(", ", requirements))), 6));
		}
		if (!task.getMasters().isEmpty())
		{
			taskInfo.add(Components.spaced(Components.label(Html.muted("Assigned by " + Html.escape(String.join(", ", task.getMasters())))), 2));
		}

		if (!task.getSpots().isEmpty())
		{
			taskInfo.add(Components.subheading("Where to kill them"));
			task.getSpots().forEach(spot -> taskInfo.add(Components.spaced(Components.label(spotHtml(spot)), 4)));
		}
		if (!task.getOtherPlaces().isEmpty())
		{
			taskInfo.add(Components.spaced(Components.label(Html.muted("Also found at: " + Html.escape(String.join(", ", task.getOtherPlaces())))), 4));
		}
		addList("Bring", task.getNeeds());
		addList("Tips", task.getTips());
		if (task.getSkip() != null)
		{
			taskInfo.add(Components.spaced(Components.label(Html.white("Rather not?") + " " + Html.escape(task.getSkip())), 6));
		}
	}

	private static String spotHtml(SlayerSpot spot)
	{
		List<String> tags = new ArrayList<>();
		if (spot.isMulti())
		{
			tags.add("multi-combat");
		}
		if (spot.isCannon())
		{
			tags.add("cannon");
		}
		if (spot.isWilderness())
		{
			tags.add(Html.color(Html.RED, "Wilderness"));
		}
		return Html.color(Html.GOLD, Html.escape(spot.getWhere()))
			+ (tags.isEmpty() ? "" : " " + Html.muted("(" + String.join(", ", tags) + ")"))
			+ "<br>" + Html.escape(spot.getHow());
	}

	private void addList(String title, List<String> items)
	{
		if (items.isEmpty())
		{
			return;
		}
		StringBuilder html = new StringBuilder(Html.white(title + ":"));
		items.forEach(item -> html.append("<br>- ").append(Html.escape(item)));
		taskInfo.add(Components.spaced(Components.label(html.toString()), 6));
	}
}
