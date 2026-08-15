package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/**
 * Small compatibility fixes for the published PvZ skin.
 *
 * <p>The v0.1.0 skin JAR does not register default styles for some standard
 * Scene2D widgets used by the graphical menus (currently SelectBox and
 * CheckBox). Build those missing styles from resources that are already
 * present in the skin instead of maintaining a second skin file in this
 * project.</p>
 */
public final class PvzSkinCompatibility {
    private PvzSkinCompatibility() {
    }

    public static void installMissingStyles(Skin skin) {
        if (skin == null) {
            throw new IllegalArgumentException("skin cannot be null");
        }
        installDefaultSelectBoxStyle(skin);
        installDefaultCheckBoxStyle(skin);
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
