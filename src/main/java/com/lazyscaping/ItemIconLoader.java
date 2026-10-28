package com.lazyscaping;

import com.lazyscaping.api.GearItem;
import com.lazyscaping.api.ItemRef;
import com.lazyscaping.api.LazyScapingClient;
import com.lazyscaping.ui.IconLoader;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Loads item pictures for the panel. Gear the player owns is drawn as their own
 * copy (like an ornamented variant); other tradeable items use the game's
 * sprite, and untradeable ones lazyscaping.app's picture.
 */
@Singleton
class ItemIconLoader implements IconLoader
{
	private static final int MAX_WIDTH = 36;
	private static final int MAX_HEIGHT = 32;
	private static final float FADED_ALPHA = 0.45f;

	private final ItemManager itemManager;
	private final SkillIconManager skillIconManager;
	private final LazyScapingClient client;
	private final OwnedItems ownedItems;
	private final Map<String, CompletableFuture<BufferedImage>> siteImages = new ConcurrentHashMap<>();

	@Inject
	ItemIconLoader(ItemManager itemManager, SkillIconManager skillIconManager, LazyScapingClient client, OwnedItems ownedItems)
	{
		this.itemManager = itemManager;
		this.skillIconManager = skillIconManager;
		this.client = client;
		this.ownedItems = ownedItems;
	}

	@Override
	public void loadItem(ItemRef item, int quantity, boolean faded, JLabel target)
	{
		Integer itemId = item.getItemId();
		if (item instanceof GearItem && ((GearItem) item).getId() != null)
		{
			Integer owned = ownedItems.itemId(((GearItem) item).getId());
			if (owned != null)
			{
				itemId = owned;
			}
		}

		if (itemId != null)
		{
			AsyncBufferedImage image = itemManager.getImage(itemId, quantity, quantity > 1);
			image.onLoaded(() -> show(image, faded, target));
		}
		else if (item.getIcon() != null)
		{
			siteImages.computeIfAbsent(item.getIcon(), this::fetchSiteImage).thenAccept(image ->
			{
				if (image != null)
				{
					show(image, faded, target);
				}
			});
		}
	}

	@Override
	public void loadSkill(Skill skill, JLabel target)
	{
		show(skillIconManager.getSkillImage(skill), false, target);
	}

	void clear()
	{
		siteImages.clear();
	}

	private CompletableFuture<BufferedImage> fetchSiteImage(String path)
	{
		CompletableFuture<BufferedImage> future = new CompletableFuture<>();
		client.image(path, image -> future.complete(fit(image)), () ->
		{
			siteImages.remove(path);
			future.complete(null);
		});
		return future;
	}

	private static BufferedImage fit(BufferedImage image)
	{
		if (image.getWidth() <= MAX_WIDTH && image.getHeight() <= MAX_HEIGHT)
		{
			return image;
		}
		return ImageUtil.resizeImage(image, MAX_WIDTH, MAX_HEIGHT, true);
	}

	private static void show(BufferedImage image, boolean faded, JLabel target)
	{
		ImageIcon icon = new ImageIcon(faded ? fade(image) : image);
		SwingUtilities.invokeLater(() -> target.setIcon(icon));
	}

	private static BufferedImage fade(BufferedImage image)
	{
		BufferedImage faded = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = faded.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, FADED_ALPHA));
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return faded;
	}
}
