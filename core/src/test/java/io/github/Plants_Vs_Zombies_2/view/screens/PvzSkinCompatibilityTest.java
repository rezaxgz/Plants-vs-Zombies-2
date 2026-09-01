package io.github.Plants_Vs_Zombies_2.view.screens;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import pvz.skin.PvzSkin;

class PvzSkinCompatibilityTest {
    private static HeadlessApplication application;
    private static Skin publishedSkin;

    @BeforeAll
    static void startLibGdx() {
        application = new HeadlessApplication(new ApplicationAdapter() {
        }, new HeadlessApplicationConfiguration());
        GL20 gl = (GL20) Proxy.newProxyInstance(
                GL20.class.getClassLoader(), new Class<?>[] {GL20.class},
                (proxy, method, arguments) -> "glGenTexture".equals(
                        method.getName()) ? 1
                                : defaultValue(method.getReturnType()));
        Gdx.gl = gl;
        Gdx.gl20 = gl;
        publishedSkin = PvzSkin.get();
    }

    @AfterAll
    static void stopLibGdx() {
        if (application != null) application.exit();
    }

    @Test
    void publishedSkinConstructsInvitationAndTerminalDialogsWithFonts() {
        PvzDialog invitation = assertDoesNotThrow(() -> new PvzDialog(
                "Multiplayer invitation", publishedSkin));
        Label invitationMessage = invitation.message(
                "A player invited you to I, Zombie.");
        TextButton accept = invitation.action(
                "Accept", Boolean.TRUE, "green");
        TextButton reject = invitation.action(
                "Reject", Boolean.FALSE, "brown");

        assertDialogFonts(invitation, invitationMessage, accept, reject);

        PvzDialog terminal = assertDoesNotThrow(() -> new PvzDialog(
                "Match Cancelled", publishedSkin));
        Label terminalMessage = terminal.message("You left the match.");
        TextButton back = terminal.action(
                "Return to Multiplayer", Boolean.TRUE, "green");
        assertDialogFonts(terminal, terminalMessage, back);
    }

    @Test
    void repairsOnlyMissingFieldsOnExistingDefaults() {
        Skin skin = compatibleTestSkin();
        Drawable labelBackground = new BaseDrawable();
        Drawable buttonBackground = new BaseDrawable();
        Drawable windowBackground = new BaseDrawable();
        Color labelColor = new Color(0.1f, 0.2f, 0.3f, 1f);
        Color buttonColor = new Color(0.2f, 0.3f, 0.4f, 1f);
        Color titleColor = new Color(0.3f, 0.4f, 0.5f, 1f);

        Label.LabelStyle label = new Label.LabelStyle();
        label.fontColor = labelColor;
        label.background = labelBackground;
        skin.add("default", label, Label.LabelStyle.class);

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.fontColor = buttonColor;
        button.up = buttonBackground;
        skin.add("default", button, TextButton.TextButtonStyle.class);

        Window.WindowStyle window = new Window.WindowStyle();
        window.titleFontColor = titleColor;
        window.background = windowBackground;
        skin.add("default", window, Window.WindowStyle.class);

        PvzSkinCompatibility.installMissingStyles(skin);

        assertNotNull(label.font);
        assertSame(labelColor, label.fontColor);
        assertSame(labelBackground, label.background);
        assertNotNull(button.font);
        assertSame(buttonColor, button.fontColor);
        assertSame(buttonBackground, button.up);
        assertNotNull(window.titleFont);
        assertSame(titleColor, window.titleFontColor);
        assertSame(windowBackground, window.background);
    }

    private static Skin compatibleTestSkin() {
        PvzSkinCompatibility.installMissingStyles(publishedSkin);
        Skin skin = new Skin();
        skin.add("secondary", publishedSkin.get("secondary",
                Label.LabelStyle.class), Label.LabelStyle.class);
        skin.add("big", publishedSkin.get("big", Label.LabelStyle.class),
                Label.LabelStyle.class);
        skin.add("brown", publishedSkin.get("brown",
                TextButton.TextButtonStyle.class),
                TextButton.TextButtonStyle.class);
        skin.add("green", publishedSkin.get("green",
                TextButton.TextButtonStyle.class),
                TextButton.TextButtonStyle.class);
        skin.add("default", publishedSkin.get("default",
                TextField.TextFieldStyle.class), TextField.TextFieldStyle.class);
        skin.add("image_ui_dialog_asset_inner_bkgd_10",
                publishedSkin.getDrawable(
                        "image_ui_dialog_asset_inner_bkgd_10"),
                Drawable.class);
        skin.add("image_ui_generic_greenbutton_10",
                publishedSkin.getDrawable("image_ui_generic_greenbutton_10"),
                Drawable.class);
        return skin;
    }

    private static void assertDialogFonts(PvzDialog dialog,
            Label message, TextButton... buttons) {
        assertNotNull(dialog.getTitleLabel().getStyle().font);
        assertNotNull(message.getStyle().font);
        for (TextButton button : buttons) {
            assertNotNull(button.getStyle().font);
        }
        assertNotNull(dialog.getStyle().background);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }
}
