package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Maps the project's 69 plant names to their PvZ2 preview PAM clips. */
final class PlantAnimationCatalog {
    static final class Preview {
        private final String path;
        private final String clip;

        private Preview(String path, String clip) {
            this.path = path;
            this.clip = clip;
        }

        String getPath() {
            return path;
        }

        String getClip() {
            return clip;
        }
    }

    private static final Map<String, Preview> PREVIEWS = createPreviews();

    private PlantAnimationCatalog() {
    }

    static Preview find(String plantName) {
        return PREVIEWS.get(normalize(plantName));
    }

    private static Map<String, Preview> createPreviews() {
        Map<String, Preview> map = new HashMap<>();
        put(map, "Sunflower", "768/INITIAL/PLANT/SUNFLOWER/SUNFLOWER.PAM", "idle");
        put(map, "Twin Sunflower", "768/INITIAL/PLANT/SUNFLOWER_TWIN/SUNFLOWER_TWIN.PAM", "idle");
        put(map, "Sun-shroom", "768/FULL/PLANT/SUNSHROOM/SUNSHROOM.PAM", "idle_stage1");
        put(map, "Primal Sunflower", "768/FULL/PLANT/PRIMAL_SUNFLOWER/PRIMAL_SUNFLOWER.PAM", "idle");
        put(map, "Gold Bloom", "768/INITIAL/PLANT/GOLDBLOOM/GOLDBLOOM.PAM", "idle");
        put(map, "Peashooter", "768/INITIAL/PLANT/PEASHOOTER/PEASHOOTER.PAM", "idle");
        put(map, "Repeater", "768/INITIAL/PLANT/REPEATER/REPEATER.PAM", "idle");
        put(map, "Threepeater", "768/INITIAL/PLANT/THREEPEATER/THREEPEATER.PAM", "idle");
        put(map, "Snow Pea", "768/INITIAL/PLANT/SNOWPEA/SNOWPEA.PAM", "idle");
        put(map, "Rotobaga", "768/FULL/PLANT/ROTORUTABAGA/ROTORUTABAGA.PAM", "idle");
        put(map, "Pea Pod", "768/FULL/PLANT/PEAPOD/PEAPOD.PAM", "idle");
        put(map, "Split Pea", "768/FULL/PLANT/SPLITPEA/SPLITPEA.PAM", "idle");
        put(map, "Citron", "768/FULL/PLANT/CITRON/CITRON.PAM", "idle");
        put(map, "Caulipower", "768/INITIAL/PLANT/CAULIPOWER/CAULIPOWER.PAM", "idle1_1");
        put(map, "Electric Blueberry", "768/INITIAL/PLANT/ELECTRICBLUEBERRY/ELECTRICBLUEBERRY.PAM", "idle1_1");
        put(map, "Bowling Bulb", "768/FULL/PLANT/BOWLINGBULB/BOWLINGBULB.PAM", "idle");
        put(map, "Cactus", "768/INITIAL/PLANT/CACTUS/CACTUS.PAM", "idle");
        put(map, "Fire Peashooter", "768/INITIAL/PLANT/FIREPEASHOOTER/FIREPEASHOOTER.PAM", "idle");
        put(map, "Starfruit", "768/INITIAL/PLANT/STARFRUIT/STARFRUIT.PAM", "idle");
        put(map, "Goo Peashooter", "768/INITIAL/PLANT/GOOPEASHOOTER/GOOPEASHOOTER.PAM", "idle");
        put(map, "Mega Gatling Pea", "768/INITIAL/PLANT/MEGAGATLING/MEGAGATLING.PAM", "idle");
        put(map, "Sea-shroom", "768/FULL/PLANT/SEASHROOM/SEASHROOM.PAM", "idle");
        put(map, "Puff-shroom", "768/INITIAL/PLANT/PUFFSHROOM/PUFFSHROOM.PAM", "idle_stage1");
        put(map, "Fume-shroom", "768/INITIAL/PLANT/FUMESHROOM/FUMESHROOM.PAM", "idle");
        put(map, "Cabbage-pult", "768/INITIAL/PLANT/CABBAGEPULT/CABBAGEPULT.PAM", "idle");
        put(map, "Kernel-pult", "768/INITIAL/PLANT/KERNALPULT/KERNALPULT.PAM", "idle");
        put(map, "Melon-pult", "768/INITIAL/PLANT/MELONPULT/MELONPULT.PAM", "idle");
        put(map, "Winter Melon", "768/FULL/PLANT/WINTERMELON/WINTERMELON.PAM", "idle");
        put(map, "Pepper-pult", "768/FULL/PLANT/PEPPERPULT/PEPPERPULT.PAM", "idle");
        put(map, "Potato Mine", "768/INITIAL/PLANT/POTATOMINE/POTATOMINE.PAM", "idle");
        put(map, "Primal Potato Mine", "768/FULL/PLANT/PRIMAL_POTATOMINE/PRIMAL_POTATOMINE.PAM", "idle");
        put(map, "Cherry Bomb", "768/FULL/PLANT/CHERRYBOMB/CHERRYBOMB.PAM", "idle");
        put(map, "Squash", "768/INITIAL/PLANT/SQUASH/SQUASH.PAM", "idle");
        put(map, "Grapeshot", "768/INITIAL/PLANT/GRAPESHOT/GRAPESHOT.PAM", "idle");
        put(map, "Jalapeno", "768/INITIAL/PLANT/JALAPENO/JALAPENO.PAM", "idle");
        put(map, "Doom-shroom", "768/FULL/PLANT/DOOMSHROOM/DOOMSHROOM.PAM", "stage1_idle");
        put(map, "Tangle Kelp", "768/FULL/PLANT/TANGLEKELP/TANGLEKELP.PAM", "idle");
        put(map, "Iceberg Lettuce", "768/INITIAL/PLANT/ICEBURG/ICEBURG.PAM", "idle");
        put(map, "Bonk Choy", "768/INITIAL/PLANT/BONKCHOY/BONKCHOY.PAM", "idle");
        put(map, "Phat Beet", "768/FULL/PLANT/PHATBEETS/PHATBEETS.PAM", "idle");
        put(map, "Chomper", "768/INITIAL/PLANT/CHOMPER/CHOMPER.PAM", "idle");
        put(map, "Wasabi Whip", "768/INITIAL/PLANT/WASABIWHIP/WASABIWHIP.PAM", "idle");
        put(map, "Kiwibeast", "768/INITIAL/PLANT/KIWIBEAST/KIWIBEAST.PAM", "idle_stage1_");
        put(map, "Wall-nut", "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM", "idle");
        put(map, "Tall-nut", "768/FULL/PLANT/TALLNUT/TALLNUT.PAM", "idle");
        put(map, "Endurian", "768/FULL/PLANT/ENDURIAN/ENDURIAN.PAM", "idle");
        put(map, "Garlic", "768/FULL/PLANT/GARLIC/GARLIC.PAM", "idle");
        put(map, "Sweet Potato", "768/INITIAL/PLANT/SWEETPOTATO/SWEETPOTATO.PAM", "idle");
        put(map, "Explode-o-nut", "768/INITIAL/PLANT/EXPLODEONUT/EXPLODEONUT.PAM", "idle");
        put(map, "Pumpkin", "768/INITIAL/PLANT/PUMPKIN/PUMPKIN.PAM", "idle");
        put(map, "Sun Bean", "768/FULL/PLANT/SUNBEAN/SUNBEAN.PAM", "idle");
        put(map, "Torchwood", "768/INITIAL/PLANT/TORCHWOOD/TORCHWOOD.PAM", "idle");
        put(map, "Magnet-shroom", "768/FULL/PLANT/MAGNETSHROOM/MAGNETSHROOM.PAM", "idle");
        put(map, "Hypno-shroom", "768/INITIAL/PLANT/HYPNOSHROOM/HYPNOSHROOM.PAM", "idle");
        put(map, "Cat-tail", "768/INITIAL/PLANT/HOMINGTHISTLE/HOMINGTHISTLE.PAM", "idle");
        put(map, "Imitater", "768/INITIAL/PLANT/IMITATER/IMITATER.PAM", "idle");
        put(map, "Ice-shroom", "768/FULL/PLANT/ICESHROOM/ICESHROOM.PAM", "idle");
        put(map, "Lily Pad", "768/FULL/PLANT/LILYPAD/LILYPAD.PAM", "idle");
        put(map, "Hot Potato", "768/FULL/PLANT/HOTPOTATO/HOTPOTATO.PAM", "idle");
        put(map, "Grave Buster", "768/INITIAL/PLANT/GRAVEBUSTER/GRAVEBUSTER.PAM", "attack1");
        put(map, "Enlighten-mint", "768/INITIAL/EMPOWERMINTS/PLANT/ENLIGHTENMINT/ENLIGHTENMINT.PAM", "loop");
        put(map, "Appease-mint", "768/INITIAL/EMPOWERMINTS/PLANT/APPEASEMINT/APPEASEMINT.PAM", "loop");
        put(map, "Arma-mint", "768/INITIAL/EMPOWERMINTS/PLANT/ARMAMINT/ARMAMINT.PAM", "loop");
        put(map, "Bombard-mint", "768/INITIAL/EMPOWERMINTS/PLANT/BOMBARDMINT/BOMBARDMINT.PAM", "loop");
        put(map, "Enforce-mint", "768/INITIAL/EMPOWERMINTS/PLANT/ENFORCEMINT/ENFORCEMINT.PAM", "loop");
        put(map, "Reinforce-mint", "768/INITIAL/EMPOWERMINTS/PLANT/REINFORCEMINT/REINFORCEMINT.PAM", "loop");
        put(map, "Enchant-mint", "768/INITIAL/EMPOWERMINTS/PLANT/ENCHANTMINT/ENCHANTMINT.PAM", "loop");
        put(map, "Pierce-mint", "768/INITIAL/EMPOWERMINTS/PLANT/SPEARMINT/SPEARMINT.PAM", "loop");
        put(map, "catTail-mint", "768/INITIAL/EMPOWERMINTS/PLANT/CONTAINMINT/CONTAINMINT.PAM", "loop");
        return Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, Preview> map, String name,
            String path, String clip) {
        map.put(normalize(name), new Preview(path, clip));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
