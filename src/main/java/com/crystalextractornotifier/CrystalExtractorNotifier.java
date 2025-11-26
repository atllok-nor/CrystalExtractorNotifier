package com.crystalextractornotifier;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;

import net.runelite.api.Skill;
import net.runelite.api.ChatMessageType;

import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;

import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
        name = "Crystal Extractor Notifier",
        description = "Highlights the Crystal Extractor and notifies when a mote is ready."
)
public class CrystalExtractorNotifier extends Plugin
{
    // Extractor swaps between these two IDs depending on activation state
    private static final int EXTRACTOR_ID_INACTIVE = 59703;
    private static final int EXTRACTOR_ID_ACTIVE   = 59702;

    // Confirmed Sailing XP awarded on successful harvest
    private static final int HARVEST_XP = 600;

    // Hard-coded, non-configurable trigger phrase (server message)
    private static final String TRIGGER_PHRASE = "Your crystal extractor has harvested a crystal mote!";

    @Inject private Client client;
    @Inject private Notifier notifier;
    @Inject private OverlayManager overlayManager;
    @Inject private CrystalExtractorNotifierConfig config;
    @Inject private CrystalExtractorNotifierOverlay overlay;

    @Getter private GameObject extractor;
    @Getter private boolean ready = false;

    // Track Sailing XP locally because some builds of StatChanged do not expose previous XP
    private int previousSailingXp = -1;

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        log.info("Crystal Extractor Notifier started.");
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        extractor = null;
        ready = false;
        previousSailingXp = -1;
        log.info("Crystal Extractor Notifier stopped.");
    }

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------
    private boolean isExtractor(GameObject obj)
    {
        int id = obj.getId();
        return id == EXTRACTOR_ID_INACTIVE || id == EXTRACTOR_ID_ACTIVE;
    }

    // --------------------------------------------------------------------
    // Extractor spawn/despawn handling
    // --------------------------------------------------------------------
    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject obj = event.getGameObject();
        if (isExtractor(obj))
        {
            extractor = obj;
            log.info("Extractor spawned (ID={}).", obj.getId());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        GameObject obj = event.getGameObject();
        if (isExtractor(obj))
        {
            extractor = null;
            ready = false;
            log.info("Extractor despawned.");
        }
    }

    // --------------------------------------------------------------------
    // Login → try to locate extractor on current plane
    // --------------------------------------------------------------------
    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        switch (event.getGameState())
        {
            case LOGIN_SCREEN:
            case HOPPING:
            case LOADING:
                extractor = null;
                ready = false;
                previousSailingXp = -1;
                break;

            case LOGGED_IN:
                findExtractor();
                break;
        }
    }

    private void findExtractor()
    {
        Scene scene = client.getScene();
        if (scene == null)
            return;

        Tile[][][] tiles = scene.getTiles();
        if (tiles == null)
            return;

        int plane = client.getPlane();
        if (plane < 0 || plane >= tiles.length)
            return;

        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null)
            return;

        for (Tile[] row : planeTiles)
        {
            for (Tile tile : row)
            {
                if (tile == null)
                    continue;

                GameObject[] objs = tile.getGameObjects();
                if (objs == null)
                    continue;

                for (GameObject obj : objs)
                {
                    if (obj == null)
                        continue;

                    if (isExtractor(obj))
                    {
                        extractor = obj;
                        log.info("Extractor located via scan (ID={}).", obj.getId());
                        return;
                    }

                    // Name fallback for future-proofing
                    String name = client.getObjectDefinition(obj.getId()).getName();
                    if ("Crystal extractor".equalsIgnoreCase(name))
                    {
                        extractor = obj;
                        log.info("Extractor located via NAME fallback (ID={}).", obj.getId());
                        return;
                    }
                }
            }
        }

        log.debug("Extractor not found on plane {}", plane);
    }

    // --------------------------------------------------------------------
    // READY (GREEN) — triggered by chat message
    // --------------------------------------------------------------------
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
            return;

        String msg = event.getMessage();
        if (msg == null)
            return;

        // Use the hard-coded trigger phrase to avoid a null config value
        if (msg.contains(TRIGGER_PHRASE))
        {
            ready = true;
            notifier.notify(config.notificationText());
            log.info("Extractor ready (chat trigger).");
        }
    }

    // --------------------------------------------------------------------
    // COOLDOWN (RED) — reliably server-validated using XP drop
    // --------------------------------------------------------------------
    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        // Only trigger on Sailing XP
        if (event.getSkill() != Skill.SAILING)
            return;

        int currentXp = event.getXp();

        // First event after login → initialize only
        if (previousSailingXp == -1)
        {
            previousSailingXp = currentXp;
            return;
        }

        int xpGained = currentXp - previousSailingXp;
        previousSailingXp = currentXp;

        if (xpGained == HARVEST_XP && ready)
        {
            ready = false;
            log.info("Extractor harvested (Sailing XP detected). Cooldown started.");
        }
    }

    // --------------------------------------------------------------------
    // Config
    // --------------------------------------------------------------------
    @Provides
    CrystalExtractorNotifierConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CrystalExtractorNotifierConfig.class);
    }
}