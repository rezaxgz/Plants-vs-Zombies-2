package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/** Dialog whose title, content and buttons all use the published PvZ styles. */
public class PvzDialog extends Dialog {
    private final Skin pvzSkin;

    public PvzDialog(String title, Skin skin) {
        super(title, PvzSkinCompatibility.prepareDialogSkin(skin));
        this.pvzSkin = skin;
        pad(18f);
        getContentTable().pad(12f);
        getButtonTable().defaults().minWidth(170f).height(52f).pad(6f);
    }

    public Label message(String text) {
        Label label = new Label(text, pvzSkin, "secondary");
        label.setWrap(true);
        getContentTable().add(label).width(470f).left().pad(8f);
        return label;
    }

    public TextButton action(String text, Object result, String styleName) {
        TextButton button = new TextButton(text, pvzSkin, styleName);
        button(button, result);
        return button;
    }
}
