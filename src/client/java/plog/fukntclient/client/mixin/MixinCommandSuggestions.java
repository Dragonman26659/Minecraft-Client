package plog.fukntclient.client.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.CommandManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Replaces the chat suggestion popup with FukntClient's own whenever the chat box
 * starts with the command prefix. Low priority number = runs before any other
 * mod's injection at the same point (e.g. Baritone), and we cancel the original.
 */
@Mixin(value = CommandSuggestions.class, priority = 500)
public abstract class MixinCommandSuggestions {

    @Shadow @Final Minecraft minecraft;
    @Shadow @Final EditBox input;
    @Shadow @Final private List<FormattedCharSequence> commandUsage;
    @Shadow private ParseResults<SharedSuggestionProvider> currentParse;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private CommandSuggestions.SuggestionsList suggestions;
    @Shadow private boolean allowSuggestions;
    @Shadow boolean keepSuggestions;

    @Shadow public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void fuknt$updateCommandInfo(CallbackInfo ci) {
        if (FukntClientClient.cm == null) return;

        String text = this.input.getValue();
        if (!text.startsWith(CommandManager.PREFIX)) return;

        this.currentParse = null;
        if (!this.keepSuggestions) {
            this.input.setSuggestion(null);
            this.suggestions = null;
        }
        this.commandUsage.clear();

        this.pendingSuggestions = FukntClientClient.cm.getSuggestions(text, this.input.getCursorPosition());

        if (this.allowSuggestions && this.minecraft.options.autoSuggestions().get()) {
            this.showSuggestions(false);
        }
        ci.cancel();
    }
}
