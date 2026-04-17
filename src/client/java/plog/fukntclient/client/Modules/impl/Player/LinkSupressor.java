package plog.fukntclient.client.Modules.impl.Player;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;

import java.util.regex.Pattern;

@ModuleInfo(name = "LinkSuppressor", description = "Removes chat messages containing links", category = Category.Player)
public class LinkSupressor extends Module {

    public final BooleanSetting allowFriends  = new BooleanSetting("Allow Friends",  true);
    public final BooleanSetting notifyBlocked = new BooleanSetting("Notify Blocked", true);

    // Matches common URL patterns:
    // - http(s)://anything
    // - www.anything.tld
    // - bare domains like "example.com", "go.gg/abc"
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(" +
                    "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+" +   // http(s):// URLs
                    "|www\\.[\\w\\-]+\\.[a-z]{2,}[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*" + // www. URLs
                    "|[\\w\\-]{2,}\\.[a-z]{2,6}(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?" + // bare domain.tld
                    ")\\b"
    );

    // Extracts the sender name from vanilla chat format "<PlayerName> message"
    private static final Pattern SENDER_PATTERN = Pattern.compile("^<([^>]+)>");

    public LinkSupressor() {
        addSettings(allowFriends, notifyBlocked);
    }

    /**
     * Called by the chat mixin before a message is added to the chat HUD.
     * Returns true if the message should be suppressed (dropped), false to allow it through.
     */
    public boolean shouldSuppress(Component message) {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return false;

        String raw = message.getString();

        if (!URL_PATTERN.matcher(raw).find()) return false;

        // Check if the message was sent by a friend
        if (allowFriends.isActive()) {
            var senderMatch = SENDER_PATTERN.matcher(raw);
            if (senderMatch.find()) {
                String sender = senderMatch.group(1);
                if (FukntClientClient.fm.isFriend(sender)) return false;
            }
        }

        if (notifyBlocked.isActive()) {
            mc.gui.getChat().addMessage(
                    Component.literal("§7[§cLinkSuppressor§7] §8Blocked a message containing a link.")
            );
        }

        return true;
    }
}