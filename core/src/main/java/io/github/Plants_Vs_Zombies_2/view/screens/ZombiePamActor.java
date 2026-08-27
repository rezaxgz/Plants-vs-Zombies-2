package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.ZombieAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.ZombossAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.armor.Armor;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.armor.ArmorType;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.PamPlayer.AnimationPart;

/**
 * Game-board PAM actor for zombies. Only walk/eat presentation is selected by
 * the GUI for now; the model can still run its complete Phase-1 behaviour.
 */
final class ZombiePamActor extends Actor {
    private final PamPlayer player;
    private final Zombie zombie;
    private final String pamPath;
    private final String idleClip;
    private final List<String> clips;
    private final Map<String, Boolean> armorVisibleParts;
    private final Map<String, Boolean> armorHiddenParts;

    private String currentClip;
    private Rectangle animationBounds;
    private Rectangle sizingBounds;
    private Rectangle footReferenceBounds;
    private float stateTime;
    private float hurtFlashRemainingSeconds;
    private boolean eating;
    private boolean lastArmorVisible;
    private int lastBossActionSequence;
    private float bossActionRemainingSeconds;

    ZombiePamActor(PamPlayer player, Zombie zombie,
            ZombieVisualCatalog.Visual visual) {
        if (player == null || zombie == null || visual == null) {
            throw new IllegalArgumentException(
                    "player, zombie and visual cannot be null");
        }
        this.player = player;
        this.zombie = zombie;
        this.pamPath = visual.getPamPath();
        this.idleClip = visual.getIdleClip();
        this.clips = player.clips(pamPath);
        this.armorHiddenParts = createArmorVisibility(false);
        this.armorVisibleParts = createArmorVisibility(true);
        this.lastArmorVisible = hasVisibleArmor();
        ZombossAbility bossAbility = findZombossAbility();
        this.lastBossActionSequence = bossAbility == null
                ? 0 : bossAbility.getActionSequence();
        refreshSizingBounds();
        setClip(resolveWalkClip());
    }

    Zombie getZombie() {
        return zombie;
    }

    void setEating(boolean eating) {
        if (zombie.getType().isBoss()) {
            refreshBossPresentation(0f);
            return;
        }
        boolean armorVisible = hasVisibleArmor();
        if (this.eating == eating && armorVisible == lastArmorVisible) {
            return;
        }
        this.eating = eating;
        boolean armorChanged = armorVisible != lastArmorVisible;
        this.lastArmorVisible = armorVisible;
        if (armorChanged) {
            refreshSizingBounds();
        }
        setClip(eating ? resolveEatClip() : resolveWalkClip());
    }

    void flashHurt() {
        hurtFlashRemainingSeconds = HurtFlashEffect.start(
                hurtFlashRemainingSeconds);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
        hurtFlashRemainingSeconds = HurtFlashEffect.advance(
                hurtFlashRemainingSeconds,
                HurtFlashEffect.realFrameDeltaSeconds());
        if (zombie.getType().isBoss()) {
            refreshBossPresentation(Math.max(0f, delta));
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Rectangle referenceBounds = validBounds(sizingBounds)
                ? sizingBounds : animationBounds;
        if (!validBounds(animationBounds)
                || !validBounds(referenceBounds)
                || getWidth() <= 0f || getHeight() <= 0f) {
            return;
        }

        // Walk clips often have much wider aggregate bounds than the actual
        // zombie because the animation contains root movement. Scaling from
        // those bounds makes walking zombies appear tiny, so keep using the
        // eat/idle pose as the stable sizing reference.
        //
        // PAM coordinates are Y-down. PamPlayer.bounds() therefore reports
        // the top of the animation in bounds.y and the bottom in
        // bounds.y + bounds.height. PamPlayer.draw() flips Y when rendering.
        // The previous code treated bounds.y as the foot edge; that only
        // happened to work for nearly symmetric PAMs. Asymmetric animations
        // such as Egypt Ra were consequently shifted by a large fraction of a
        // lane. Anchor the BOTTOM of the stable idle bounds to the lane foot
        // line instead, which gives every walk/eat clip the same ground point.
        float scale = Math.min(
                getWidth() / referenceBounds.width,
                getHeight() / referenceBounds.height);
        float renderedWidth = referenceBounds.width * scale;
        float visualLeft = getX() + (getWidth() - renderedWidth) * 0.5f;
        Rectangle feetBounds = validBounds(footReferenceBounds)
                ? footReferenceBounds : referenceBounds;
        float anchorX = visualLeft - referenceBounds.x * scale;
        float footBottomY = feetBounds.y + feetBounds.height;
        float anchorY = getY() + footBottomY * scale;

        Color oldColor = new Color(batch.getColor());
        Matrix4 oldTransform = new Matrix4(batch.getTransformMatrix());
        Matrix4 scaledTransform = new Matrix4(oldTransform);
        scaledTransform.translate(anchorX, anchorY, 0f);
        scaledTransform.scale(scale, scale, 1f);
        scaledTransform.translate(-anchorX, -anchorY, 0f);

        batch.flush();
        batch.setTransformMatrix(scaledTransform);

        Map<String, Boolean> visibleParts = hasVisibleArmor()
                ? armorVisibleParts : armorHiddenParts;
        Color actorColor = getColor();
        batch.setColor(actorColor.r, actorColor.g, actorColor.b,
                actorColor.a * parentAlpha);
        player.draw(batch, pamPath, currentClip, stateTime,
                anchorX, anchorY, true, visibleParts);

        float hurtOverlayAlpha = HurtFlashEffect.overlayAlpha(
                hurtFlashRemainingSeconds);
        if (hurtOverlayAlpha > 0f) {
            batch.flush();
            int oldBlendSrc = batch.getBlendSrcFunc();
            int oldBlendDst = batch.getBlendDstFunc();
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 1f, 1f,
                    actorColor.a * parentAlpha * hurtOverlayAlpha);
            player.draw(batch, pamPath, currentClip, stateTime,
                    anchorX, anchorY, true, visibleParts);
            batch.flush();
            batch.setBlendFunction(oldBlendSrc, oldBlendDst);
        }
        batch.flush();
        batch.setTransformMatrix(oldTransform);
        batch.setColor(oldColor);
    }

    private void refreshSizingBounds() {
        String referenceClip = resolveEatClip();
        Rectangle bounds = player.bounds(pamPath, referenceClip);
        Rectangle idleBounds = player.bounds(pamPath, idleClip);
        if (!validBounds(bounds)) {
            bounds = idleBounds;
        }
        sizingBounds = bounds;
        footReferenceBounds = validBounds(idleBounds)
                ? idleBounds : bounds;
    }

    private boolean validBounds(Rectangle bounds) {
        return bounds != null && bounds.width > 0f && bounds.height > 0f;
    }

    private void setClip(String clip) {
        if (clip == null || clip.isBlank()) {
            clip = idleClip;
        }
        if (clip.equals(currentClip)) {
            return;
        }
        currentClip = clip;
        animationBounds = player.bounds(pamPath, currentClip);
        stateTime = 0f;
    }

    private String resolveWalkClip() {
        if (zombie.getType().isBoss()) {
            return firstAvailable(idleClip, "idle", "almanac_idle");
        }
        if (zombie.getType() == ZombieType.NEWSPAPER && hasVisibleArmor()) {
            return firstAvailable("walk_newspaper", "walk", idleClip);
        }
        if (zombie.getType() == ZombieType.PHARAOH && !hasVisibleArmor()) {
            return firstAvailable("walk_norm", "walk", idleClip);
        }
        if (zombie.getType() == ZombieType.SURFER && hasVisibleArmor()) {
            return firstAvailable("walk_board", "walk", idleClip);
        }
        return firstAvailable("walk", idleClip);
    }

    private String resolveEatClip() {
        if (zombie.getType().isBoss()) {
            return resolveWalkClip();
        }
        if (zombie.getType() == ZombieType.NEWSPAPER && hasVisibleArmor()) {
            return firstAvailable("eat_newspaper", "eat", idleClip);
        }
        if (zombie.getType() == ZombieType.PHARAOH && !hasVisibleArmor()) {
            return firstAvailable("eat_norm", "eat", idleClip);
        }
        if (zombie.getType() == ZombieType.WEASEL) {
            return firstAvailable("eat_loop", "eat", idleClip);
        }
        return firstAvailable("eat", "eat_loop", idleClip);
    }

    private void refreshBossPresentation(float delta) {
        ZombossAbility ability = findZombossAbility();
        if (ability == null) {
            setClip(resolveWalkClip());
            return;
        }
        if (zombie.isStunned()) {
            bossActionRemainingSeconds = 0f;
            setClip(resolveBossStunClip());
            return;
        }
        int sequence = ability.getActionSequence();
        if (sequence != lastBossActionSequence) {
            lastBossActionSequence = sequence;
            String actionClip = resolveBossActionClip(
                    ability.getLastActionName());
            setClip(actionClip);
            bossActionRemainingSeconds = Math.max(0.05f,
                    player.clipDurationSeconds(pamPath, actionClip));
            return;
        }
        if (bossActionRemainingSeconds > 0f) {
            bossActionRemainingSeconds = Math.max(0f,
                    bossActionRemainingSeconds - delta);
            if (bossActionRemainingSeconds > 0f) {
                return;
            }
        }
        setClip(resolveWalkClip());
    }

    private ZombossAbility findZombossAbility() {
        if (!zombie.getType().isBoss()) {
            return null;
        }
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof ZombossAbility) {
                return (ZombossAbility) ability;
            }
        }
        return null;
    }

    private String resolveBossStunClip() {
        if (zombie.getType() == ZombieType.ZOMBOSS_ICEAGE) {
            return firstAvailable("stun", "idle", idleClip);
        }
        return firstAvailable("stun_loop", "vulnerable_loop",
                "stun_start", "idle", idleClip);
    }

    private String resolveBossActionClip(String action) {
        if (action == null) {
            return resolveWalkClip();
        }
        switch (zombie.getType()) {
            case ZOMBOSS_EGYPT:
            case ZOMBOSS_COWBOY:
                if ("SPAWN".equals(action)) {
                    return firstAvailable("zombie_portal_start", "idle");
                }
                if ("RUSH".equals(action)) {
                    return firstAvailable("stomp", "idle");
                }
                if ("ROCKET".equals(action)) {
                    return firstAvailable("rocket_launch", "missile_start", "idle");
                }
                if ("MOVE".equals(action)) {
                    return firstAvailable("walk_up", "walk_down", "idle");
                }
                break;
            case ZOMBOSS_ICEAGE:
                if ("ROCKET".equals(action)) {
                    return firstAvailable("slingshot", "idle", "almanac_idle");
                }
                if ("ICY_WIND".equals(action)) {
                    return firstAvailable("wind_1", "wind_2", "idle");
                }
                if ("FREEZE_COLUMN".equals(action)) {
                    return firstAvailable("glacier_column_1", "idle");
                }
                break;
            case ZOMBOSS_BEACH:
                if ("TURBINE".equals(action)) {
                    return firstAvailable("suction_on", "suction_loop", "idle");
                }
                if ("BABY_SHARK".equals(action) || "SPAWN".equals(action)) {
                    return firstAvailable("spawn", "idle");
                }
                if ("MOVE".equals(action)) {
                    return firstAvailable("submerge", "emerge", "idle");
                }
                break;
            case ZOMBOSS_DARK:
                if ("SPAWN".equals(action)) {
                    return firstAvailable("summoning", "idle");
                }
                if ("FIRE_BREATH".equals(action)) {
                    return firstAvailable("fire_attack", "idle");
                }
                if ("FIREBALLS".equals(action)) {
                    return firstAvailable("fire_bomb", "idle");
                }
                break;
            default:
                break;
        }
        return resolveWalkClip();
    }

    private String firstAvailable(String... candidates) {
        if (clips != null) {
            for (String candidate : candidates) {
                if (candidate == null) {
                    continue;
                }
                for (String available : clips) {
                    if (candidate.equalsIgnoreCase(available)) {
                        return available;
                    }
                }
            }
        }
        return idleClip;
    }

    private boolean hasVisibleArmor() {
        Armor armor = zombie.getArmor();
        return armor != null && !armor.isDestroyed()
                && armor.getType() != ArmorType.NONE;
    }

    private Map<String, Boolean> createArmorVisibility(boolean showArmor) {
        Map<String, Boolean> visibility = new HashMap<>();
        AnimationPart root = player.getParts(pamPath);
        if (root == null) {
            return visibility;
        }
        collectArmorVisibility(root, showArmor,
                zombie.getArmor() == null
                        ? ArmorType.NONE
                        : zombie.getArmor().getType(),
                visibility);
        return visibility;
    }

    /**
     * libPVZ deliberately hides PAM parts whose names contain "armor" unless
     * their exact part names are opted back in. Walk the PAM hierarchy so the
     * requested full-health armor and all of its armor ancestors are visible,
     * while damaged armor layers stay hidden.
     */
    private boolean collectArmorVisibility(AnimationPart part,
            boolean showArmor, ArmorType armorType,
            Map<String, Boolean> visibility) {
        boolean wantedBelow = false;
        for (AnimationPart child : part.children) {
            wantedBelow |= collectArmorVisibility(
                    child, showArmor, armorType, visibility);
        }

        String name = part.name == null ? "" : part.name;
        String normalized = name.toLowerCase(Locale.ROOT);
        boolean armorPart = normalized.contains("armor");
        boolean selfWanted = showArmor && armorPart
                && matchesArmorType(normalized, armorType)
                && isFullHealthArmorPart(normalized);
        boolean wanted = selfWanted || wantedBelow;
        if (armorPart) {
            visibility.put(name, showArmor && wanted);
        }
        return wanted;
    }

    private boolean matchesArmorType(String partName, ArmorType type) {
        if (type == null) {
            return false;
        }
        switch (type) {
        case CONE:
            return partName.contains("cone");
        case BUCKET:
            return partName.contains("bucket");
        case BRICK:
            return partName.contains("brick");
        case ICE_BLOCK:
            return partName.contains("iceblock")
                    || partName.contains("ice_block");
        case SHOULDER_ARMOR:
            return partName.contains("shoulder");
        case CROWN:
            return partName.contains("crown");
        case KNIGHT:
            return partName.contains("shoulder")
                    || partName.contains("crown")
                    || partName.contains("knight");
        default:
            // Newspaper, sarcophagus and surfboard use dedicated PAM layers
            // rather than the generic hidden "armor" hierarchy.
            return false;
        }
    }

    private boolean isFullHealthArmorPart(String partName) {
        return !partName.contains("damage")
                && !partName.contains("dmg")
                && !partName.contains("state");
    }
}
