package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.HashMap;
import java.util.Map;

import io.github.Plants_Vs_Zombies_2.model.collections.zombies.ZombieCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;

/**
 * Maps project zombie types to their official PvZ2 Almanac packet art and an
 * idle PAM preview. Several armor variants share the same character PAM; the
 * packet art remains variant-specific so the collection grid still displays
 * the correct armor.
 */
final class ZombieVisualCatalog {
    static final class Visual {
        private final String packetAsset;
        private final String pamPath;
        private final String idleClip;

        Visual(String packetAsset, String pamPath, String idleClip) {
            this.packetAsset = packetAsset;
            this.pamPath = pamPath;
            this.idleClip = idleClip;
        }

        String getPacketAsset() {
            return packetAsset;
        }

        String getPamPath() {
            return pamPath;
        }

        String getIdleClip() {
            return idleClip;
        }
    }

    private static final Map<String, Visual> VISUALS = createVisuals();

    private ZombieVisualCatalog() {
    }

    static Visual find(ZombieCollectionItem zombie) {
        if (zombie == null) {
            return null;
        }
        return VISUALS.get(zombie.getTypeName());
    }

    static Visual find(ZombieType zombieType) {
        return zombieType == null ? null : VISUALS.get(zombieType.name());
    }

    static String packetAssetFor(ZombieCollectionItem zombie) {
        Visual visual = find(zombie);
        return visual == null
                ? "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_GUIDE"
                : visual.getPacketAsset();
    }

    private static Map<String, Visual> createVisuals() {
        Map<String, Visual> result = new HashMap<>();

        add(result, "BASIC", "TUTORIAL", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM", "idle");
        add(result, "CONEHEAD", "TUTORIAL_ARMOR1", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM", "idle");
        add(result, "BUCKETHEAD", "TUTORIAL_ARMOR2", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM", "idle");
        add(result, "BRICKHEAD", "TUTORIAL_ARMOR4", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM", "idle");
        add(result, "GARGANTUAR", "TUTORIAL_GARGANTUAR", "768/INITIAL/ZOMBIE/TUTORIAL_GARGANTUAR/TUTORIAL_GARGANTUAR.PAM", "idle");
        add(result, "IMP", "TUTORIAL_IMP", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL_IMP/ZOMBIE_TUTORIAL_IMP.PAM", "idle");
        add(result, "FLAG", "TUTORIAL_FLAG", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL_FLAG/ZOMBIE_TUTORIAL_FLAG.PAM", "idle");

        add(result, "MUMMY", "MUMMY", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM", "idle");
        add(result, "MUMMY_CONEHEAD", "MUMMY_ARMOR1", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM", "idle");
        add(result, "MUMMY_BUCKETHEAD", "MUMMY_ARMOR2", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM", "idle");
        add(result, "MUMMY_BRICKHEAD", "MUMMY_ARMOR4", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM", "idle");
        add(result, "PHARAOH", "PHARAOH", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_SARCOPHAGUS/ZOMBIE_EGYPT_SARCOPHAGUS.PAM", "idle");
        add(result, "RA", "RA", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM", "idle");
        add(result, "EXPLORER", "EXPLORER", "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM", "idle");
        add(result, "TOMB_RAISER", "TOMB_RAISER", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM", "idle");
        add(result, "CAMEL", "CAMEL_ALMANAC", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_CAMEL/ZOMBIE_EGYPT_CAMEL.PAM", "idle");
        add(result, "EGYPT_GARGANTUAR", "EGYPT_GARGANTUAR", "768/INITIAL/ZOMBIE/EGYPT_GARGANTUAR/EGYPT_GARGANTUAR.PAM", "idle");
        add(result, "EGYPT_IMP", "EGYPT_IMP", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_IMP/ZOMBIE_EGYPT_IMP.PAM", "idle");

        add(result, "ICEAGE", "ICEAGE", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_BASIC/ZOMBIE_ICEAGE_BASIC.PAM", "idle");
        add(result, "ICEAGE_CONEHEAD", "ICEAGE_ARMOR1", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_BASIC/ZOMBIE_ICEAGE_BASIC.PAM", "idle");
        add(result, "ICEAGE_BUCKETHEAD", "ICEAGE_ARMOR2", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_BASIC/ZOMBIE_ICEAGE_BASIC.PAM", "idle");
        add(result, "ICEAGE_BLOCKHEAD", "ICEAGE_ARMOR3", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_BASIC/ZOMBIE_ICEAGE_BASIC.PAM", "idle");
        add(result, "HUNTER", "ICEAGE_HUNTER", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM", "idle");
        add(result, "TROGLOBITE", "ICEAGE_TROGLOBITE", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM", "idle");
        add(result, "DODO", "ICEAGE_DODO", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM", "idle");
        add(result, "WEASEL_HOARDER", "ICEAGE_WEASELHOARDER", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_WEASELHOARDER/ZOMBIE_ICEAGE_WEASELHOARDER.PAM", "idle");
        add(result, "WEASEL", "ICEAGE_WEASEL", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_WEASEL/ZOMBIE_ICEAGE_WEASEL.PAM", "idle");
        add(result, "ICEAGE_GARGANTUAR", "ICEAGE_GARGANTUAR", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_GARGANTUAR/ZOMBIE_ICEAGE_GARGANTUAR.PAM", "idle");
        add(result, "ICEAGE_IMP", "ICEAGE_IMP", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_IMP/ZOMBIE_ICEAGE_IMP.PAM", "idle");

        add(result, "BEACH", "BEACH", "768/FULL/ZOMBIE/ZOMBIE_BEACH_BASIC/ZOMBIE_BEACH_BASIC.PAM", "idle");
        add(result, "BEACH_CONEHEAD", "BEACH_ARMOR1", "768/FULL/ZOMBIE/ZOMBIE_BEACH_BASIC/ZOMBIE_BEACH_BASIC.PAM", "idle");
        add(result, "BEACH_BUCKETHEAD", "BEACH_ARMOR2", "768/FULL/ZOMBIE/ZOMBIE_BEACH_BASIC/ZOMBIE_BEACH_BASIC.PAM", "idle");
        add(result, "SNORKEL", "BEACH_SNORKEL", "768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM", "idle");
        add(result, "SURFER", "BEACH_SURFER", "768/FULL/ZOMBIE/ZOMBIE_BEACH_SURFER/ZOMBIE_BEACH_SURFER.PAM", "idle");
        add(result, "FISHERMAN", "BEACH_FISHERMAN", "768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM", "idle");
        add(result, "OCTOPUS", "BEACH_OCTOPUS", "768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM", "idle");
        add(result, "BEACH_GARGANTUAR", "BEACH_GARGANTUAR", "768/FULL/ZOMBIE/BEACH_GARGANTUAR/BEACH_GARGANTUAR.PAM", "idle");
        add(result, "BEACH_IMP", "BEACH_IMP", "768/FULL/ZOMBIE/ZOMBIE_BEACH_IMP_MERMAID/ZOMBIE_BEACH_IMP_MERMAID.PAM", "idle");
        // The project-specific fast swimmer has no separate official packet/PAM.
        add(result, "FAST_SWIMMER", "BEACH_FEM", "768/FULL/ZOMBIE/ZOMBIE_BEACH_BASICFEM/ZOMBIE_BEACH_BASICFEM.PAM", "idle");

        add(result, "DARK", "DARK", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM", "idle");
        add(result, "DARK_CONEHEAD", "DARK_ARMOR1", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM", "idle");
        add(result, "DARK_BUCKETHEAD", "DARK_ARMOR2", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM", "idle");
        add(result, "DARK_SHOULDER_ARMOR", "DARK_ARMOR3", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM", "idle");
        add(result, "DARK_BRICKHEAD", "DARK_ARMOR4", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC_BRICK/ZOMBIE_DARK_BASIC_BRICK.PAM", "idle");
        add(result, "WIZARD", "DARK_WIZARD", "768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM", "idle");
        add(result, "JUGGLER", "DARK_JUGGLER", "768/FULL/ZOMBIE/ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM", "idle");
        add(result, "DARK_KING", "DARK_KING", "768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM", "idle");
        add(result, "DARK_GARGANTUAR", "DARK_GARGANTUAR", "768/FULL/ZOMBIE/DARK_GARGANTUAR/DARK_GARGANTUAR.PAM", "idle");
        add(result, "DARK_IMP", "DARK_IMP", "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_MONK/ZOMBIE_DARK_IMP_MONK.PAM", "idle");
        add(result, "DRAGON_IMP", "DARK_IMP_DRAGON", "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM", "idle");

        add(result, "ALL_STAR", "MODERN_ALLSTAR", "768/FULL/ZOMBIE/ZOMBIE_MODERN_ALLSTAR/ZOMBIE_MODERN_ALLSTAR.PAM", "idle");
        add(result, "ARCADE", "EIGHTIES_ARCADE", "768/FULL/ZOMBIE/ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM", "idle");
        add(result, "LOST_CITY_JANE", "LOSTCITY_JANE", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_JANE/ZOMBIE_LOSTCITY_JANE.PAM", "idle");
        add(result, "CRYSTAL_SKULL", "LOSTCITY_CRYSTALSKULL", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_CRYSTALSKULL/ZOMBIE_LOSTCITY_CRYSTALSKULL.PAM", "idle");
        add(result, "PROSPECTOR", "PROSPECTOR", "768/FULL/ZOMBIE/ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM", "idle");
        add(result, "PIANO", "PIANO", "768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM", "idle");
        add(result, "NEWSPAPER", "MODERN_NEWSPAPER", "768/FULL/ZOMBIE/ZOMBIE_MODERN_NEWSPAPER/ZOMBIE_MODERN_NEWSPAPER.PAM", "idle_newspaper");
        add(result, "ROLLER_BARREL", "BARRELROLLER", "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM", "idle");
        // ZombiePetDefault is project-specific; use the official chicken art.
        add(result, "PET", "CHICKEN", "768/FULL/ZOMBIE/CHICKEN/CHICKEN.PAM", "idle");

        add(result, "ZOMBOSS_EGYPT", "ZOMBOSSMECH_EGYPT", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_ZOMBOSS/ZOMBIE_EGYPT_ZOMBOSS.PAM", "idle");
        add(result, "ZOMBOSS_PIRATE", "ZOMBOSSMECH_PIRATE", "768/FULL/ZOMBIE/ZOMBIE_PIRATE_ZOMBOSS/ZOMBIE_PIRATE_ZOMBOSS.PAM", "idle");
        add(result, "ZOMBOSS_COWBOY", "ZOMBOSSMECH_COWBOY", "768/FULL/ZOMBIE/ZOMBIE_COWBOY_ZOMBOSS/ZOMBIE_COWBOY_ZOMBOSS.PAM", "idle");
        add(result, "ZOMBOSS_DARK", "ZOMBOSSMECH_DARK", "768/FULL/ZOMBIE/ZOMBIE_DARK_ZOMBOSS/ZOMBIE_DARK_ZOMBOSS.PAM", "idle");

        return result;
    }

    private static void add(Map<String, Visual> result,
            String typeName, String packetSuffix,
            String pamPath, String idleClip) {
        result.put(typeName, new Visual(
                "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_" + packetSuffix,
                pamPath, idleClip));
    }
}
