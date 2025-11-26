package com.crystalextractornotifier;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.Color;

@ConfigGroup("crystalextractornotifier")
public interface CrystalExtractorNotifierConfig extends Config
{
    @ConfigItem(
            position = 0,
            keyName = "readyColor",
            name = "Ready Color",
            description = "Hull color when the crystal mote is ready."
    )
    default Color readyColor()
    {
        return Color.GREEN;
    }

    @ConfigItem(
            position = 1,
            keyName = "cooldownColor",
            name = "Cooldown Color",
            description = "Hull color while waiting for the next mote."
    )
    default Color cooldownColor()
    {
        return Color.RED;
    }

    @ConfigItem(
            position = 2,
            keyName = "notificationText",
            name = "Notification Text",
            description = "Text shown in the desktop notification when ready."
    )
    default String notificationText()
    {
        return "Your crystal mote is ready.";
    }
}