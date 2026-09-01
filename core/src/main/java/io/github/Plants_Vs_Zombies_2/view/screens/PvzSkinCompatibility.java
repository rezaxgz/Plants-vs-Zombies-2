package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Small compatibility fixes for the published PvZ skin.
 *
 * <p>The v0.1.0 skin JAR does not register default styles for some standard
 * Scene2D widgets used by the graphical menus. Build or repair those styles
 * from resources that are already present in the skin instead of maintaining
 * a second skin file in this project.</p>
 */
public final class PvzSkinCompatibility {
    private PvzSkinCompatibility() {
    }

    public static void installMissingStyles(Skin skin) {
        if (skin == null) {
            throw new IllegalArgumentException("skin cannot be null");
        }
        installDefaultLabelStyle(skin);
        installDefaultTextButtonStyle(skin);
        installDefaultWindowStyle(skin);
        installDefaultSelectBoxStyle(skin);
        installDefaultCheckBoxStyle(skin);
    }

    /**
     * Prepares a skin before {@link com.badlogic.gdx.scenes.scene2d.ui.Dialog}
     * enters its constructor. Returning the same instance makes this usable in
     * a {@code super(...)} argument, where a later repair would be too late.
     */
    static Skin prepareDialogSkin(Skin skin) {
        installMissingStyles(skin);
        return skin;
    }

    private static void installDefaultLabelStyle(Skin skin) {
        Label.LabelStyle source = skin.get("secondary", Label.LabelStyle.class);
        boolean present = skin.has("default", Label.LabelStyle.class);
        Label.LabelStyle target = present
                ? skin.get("default", Label.LabelStyle.class)
                : new Label.LabelStyle();
        if (target.font == null) target.font = source.font;
        if (target.fontColor == null) target.fontColor = source.fontColor;
        if (target.background == null) target.background = source.background;
        if (!present) {
            skin.add("default", target, Label.LabelStyle.class);
        }
    }

    private static void installDefaultTextButtonStyle(Skin skin) {
        TextButton.TextButtonStyle source =
                skin.get("brown", TextButton.TextButtonStyle.class);
        boolean present = skin.has("default", TextButton.TextButtonStyle.class);
        TextButton.TextButtonStyle target = present
                ? skin.get("default", TextButton.TextButtonStyle.class)
                : new TextButton.TextButtonStyle();
        copyMissingButtonDrawables(target, source);
        if (target.font == null) target.font = source.font;
        if (target.fontColor == null) target.fontColor = source.fontColor;
        if (target.downFontColor == null) {
            target.downFontColor = source.downFontColor;
        }
        if (target.overFontColor == null) {
            target.overFontColor = source.overFontColor;
        }
        if (target.focusedFontColor == null) {
            target.focusedFontColor = source.focusedFontColor;
        }
        if (target.checkedFontColor == null) {
            target.checkedFontColor = source.checkedFontColor;
        }
        if (target.checkedOverFontColor == null) {
            target.checkedOverFontColor = source.checkedOverFontColor;
        }
        if (target.disabledFontColor == null) {
            target.disabledFontColor = source.disabledFontColor;
        }
        if (!present) {
            skin.add("default", target, TextButton.TextButtonStyle.class);
        }
    }

    private static void copyMissingButtonDrawables(
            TextButton.TextButtonStyle target,
            TextButton.TextButtonStyle source) {
        if (target.up == null) target.up = source.up;
        if (target.down == null) target.down = source.down;
        if (target.over == null) target.over = source.over;
        if (target.focused == null) target.focused = source.focused;
        if (target.disabled == null) target.disabled = source.disabled;
        if (target.checked == null) target.checked = source.checked;
        if (target.checkedOver == null) {
            target.checkedOver = source.checkedOver;
        }
        if (target.checkedFocused == null) {
            target.checkedFocused = source.checkedFocused;
        }
    }

    private static void installDefaultWindowStyle(Skin skin) {
        Label.LabelStyle title = skin.get("big", Label.LabelStyle.class);
        boolean present = skin.has("default", Window.WindowStyle.class);
        Window.WindowStyle target = present
                ? skin.get("default", Window.WindowStyle.class)
                : new Window.WindowStyle();
        if (target.titleFont == null) target.titleFont = title.font;
        if (target.titleFontColor == null) {
            target.titleFontColor = Color.WHITE;
        }
        if (target.background == null) {
            target.background = dialogBackground(skin);
        }
        if (!present) {
            skin.add("default", target, Window.WindowStyle.class);
        }
    }

    private static Drawable dialogBackground(Skin skin) {
        return skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
    }

    private static void installDefaultCheckBoxStyle(Skin skin) {
        if (skin.has("default", CheckBox.CheckBoxStyle.class)) {
            return;
        }

        TextButton.TextButtonStyle offStyle =
                skin.get("brown", TextButton.TextButtonStyle.class);
        TextButton.TextButtonStyle onStyle =
                skin.get("green", TextButton.TextButtonStyle.class);

        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle(
                offStyle.up,
                onStyle.up,
                offStyle.font,
                offStyle.fontColor);
        checkBoxStyle.checkboxOver = offStyle.over != null
                ? offStyle.over
                : offStyle.up;
        checkBoxStyle.checkboxOnOver = onStyle.over != null
                ? onStyle.over
                : onStyle.up;
        checkBoxStyle.disabledFontColor = offStyle.disabledFontColor;

        skin.add("default", checkBoxStyle, CheckBox.CheckBoxStyle.class);
    }

    private static void installDefaultSelectBoxStyle(Skin skin) {
        if (skin.has("default", SelectBox.SelectBoxStyle.class)) {
            return;
        }

        TextField.TextFieldStyle textField =
                skin.get("default", TextField.TextFieldStyle.class);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = textField.font;
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = textField.fontColor;
        listStyle.selection = skin.getDrawable("image_ui_generic_greenbutton_10");

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background =
                skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = textField.font;
        selectBoxStyle.fontColor = textField.fontColor;
        selectBoxStyle.disabledFontColor = textField.disabledFontColor;
        selectBoxStyle.background = textField.background;
        selectBoxStyle.backgroundOver = textField.focusedBackground != null
                ? textField.focusedBackground
                : textField.background;
        selectBoxStyle.backgroundOpen = selectBoxStyle.backgroundOver;
        selectBoxStyle.backgroundDisabled = textField.disabledBackground;
        selectBoxStyle.scrollStyle = scrollStyle;
        selectBoxStyle.listStyle = listStyle;

        skin.add("default", selectBoxStyle, SelectBox.SelectBoxStyle.class);
    }
}
