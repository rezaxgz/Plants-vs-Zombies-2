package model.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFamily;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.abilities.FishingHookAbility;
import model.game.entities.zombies.abilities.ImpThrowAbility;
import model.game.entities.zombies.abilities.KingBuffAbility;
import model.game.entities.zombies.abilities.LaserBeamAbility;
import model.game.entities.zombies.abilities.OctopusThrowAbility;
import model.game.entities.zombies.abilities.SnowballThrowAbility;
import model.game.entities.zombies.abilities.SunStealAbility;
import model.game.entities.zombies.abilities.TombSummonAbility;
import model.game.entities.zombies.abilities.WeaselReleaseAbility;
import model.game.entities.zombies.abilities.WizardSpellAbility;
import model.game.entities.zombies.abilities.ZombossAbility;
import model.game.entities.zombies.abilities.ZombieAbility;
import model.game.gameTypes.GameType;

abstract class GameAbilityLogic extends GameUpdateLogic {
    protected GameAbilityLogic(Board board, GameType gameType,
            int initialSunCount, List<ZombieWave> zombieWaves,
            Random random, boolean startWavesImmediately,
            ChapterRuleset chapterRuleset, int difficultyLevel) {
        super(board, gameType, initialSunCount, zombieWaves, random,
                startWavesImmediately, chapterRuleset, difficultyLevel);
    }

    void activateAutomaticZombieAbilities(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            for (ZombieAbility ability : zombie.getAbilities()) {
                activateWeaselRelease(zombie, ability);
            }
            if (zombie.isDead() || zombie.isHypnotized()
                    || zombie.isEncasedInIce()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                activateImpThrow(zombie, ability);
                activateSunSteal(zombie, ability);
                activateTombSummon(zombie, ability);
                activateSnowballThrow(zombie, ability);
                activateFishingHook(zombie, ability);
                activateOctopusThrow(zombie, ability);
                activateWizardSpell(zombie, ability);
                activateKingBuff(zombie, ability);
                activateCrystalSkull(zombie, ability);
                activateZomboss(zombie, ability);
            }
        }
    }

    void activateZomboss(Zombie zomboss,
            ZombieAbility ability) {
        if (!(ability instanceof ZombossAbility)) {
            return;
        }

        ZombossAbility bossAbility = (ZombossAbility) ability;
        if (!bossAbility.tryUse(zomboss, board)) {
            return;
        }

        for (Zombie spawned : bossAbility.getLastSpawnedZombies()) {
            trackSpawnedZombie(spawned);
        }
        if (bossAbility.didPhaseChangeThisUse()) {
            pendingResults.add(zomboss.getName()
                    + " entered phase "
                    + bossAbility.getCurrentPhase() + ".");
        }
        for (BasePlant plant :
                bossAbility.getLastDestroyedPlants()) {
            pendingResults.add("Plant " + plant.getName()
                    + " at " + plant.getEntityPosition()
                    + " is destroyed.");
        }
        if (bossAbility.didPerformActionThisUse()) {
            pendingResults.add(zomboss.getName() + " "
                    + bossAbility.getLastActionDescription());
        }
    }

    void activateCrystalSkull(Zombie crystalSkull,
            ZombieAbility ability) {
        if (!(ability instanceof LaserBeamAbility)) {
            return;
        }

        LaserBeamAbility laser = (LaserBeamAbility) ability;
        boolean stateChanged = laser.tryUse(crystalSkull, board);
        int requestedSun = laser.drainPendingSunRequest();
        int stolenSun = Math.min(sunCount, requestedSun);
        if (stolenSun > 0) {
            spendSun(stolenSun);
            laser.recordStolenSun(stolenSun);
            pendingResults.add(crystalSkull.getName() + " stole "
                    + stolenSun + " stored sun while charging.");
        }

        if (stateChanged && laser.didStartChargingThisUse()) {
            pendingResults.add(crystalSkull.getName()
                    + " started charging its skull laser.");
        }
        if (stateChanged && laser.didFireThisUse()) {
            pendingResults.add(crystalSkull.getName()
                    + " fired its laser and destroyed "
                    + laser.getLastDestroyedPlantCount()
                    + " plant(s).");
        }
    }

    void activateWizardSpell(Zombie wizard,
            ZombieAbility ability) {
        if (!(ability instanceof WizardSpellAbility)
                || !ability.tryUse(wizard, board)) {
            return;
        }
        BasePlant target =
                ((WizardSpellAbility) ability).getLastTarget();
        if (target != null) {
            pendingResults.add(wizard.getName() + " transformed "
                    + target.getName() + " into a cat at "
                    + target.getEntityPosition() + ".");
        }
    }

    void activateKingBuff(Zombie king,
            ZombieAbility ability) {
        if (!(ability instanceof KingBuffAbility)
                || !ability.tryUse(king, board)) {
            return;
        }
        Zombie target =
                ((KingBuffAbility) ability).getLastKnightedZombie();
        if (target != null) {
            pendingResults.add(king.getName() + " knighted "
                    + target.getName()
                    + " with helmet and shoulder armor.");
        }
    }

    void activateFishingHook(Zombie fisherman,
            ZombieAbility ability) {
        if (!(ability instanceof FishingHookAbility)
                || !ability.tryUse(fisherman, board)) {
            return;
        }
        FishingHookAbility hook = (FishingHookAbility) ability;
        BasePlant target = hook.getLastTarget();
        if (target == null) {
            return;
        }
        if (hook.wasLastTargetDestroyed()) {
            pendingResults.add(fisherman.getName() + " hooked and destroyed "
                    + target.getName() + " beside the right edge.");
        } else {
            pendingResults.add(fisherman.getName() + " pulled "
                    + target.getName() + " from " + hook.getLastFromPosition()
                    + " to " + hook.getLastToPosition() + ".");
        }
    }

    void activateOctopusThrow(Zombie octopusZombie,
            ZombieAbility ability) {
        if (!(ability instanceof OctopusThrowAbility)
                || !ability.tryUse(octopusZombie, board)) {
            return;
        }
        BasePlant target =
                ((OctopusThrowAbility) ability).getLastTarget();
        if (target != null) {
            pendingResults.add(octopusZombie.getName()
                    + " covered " + target.getName()
                    + " with an octopus at "
                    + target.getEntityPosition() + ".");
        }
    }

    void activateSnowballThrow(Zombie hunter, ZombieAbility ability) {
        if (!(ability instanceof SnowballThrowAbility)
                || !ability.tryUse(hunter, board)) {
            return;
        }
        SnowballThrowAbility snowball = (SnowballThrowAbility) ability;
        BasePlant target = snowball.getLastTarget();
        if (target == null) {
            return;
        }
        pendingResults.add(hunter.getName() + " hit " + target.getName()
                + " with " + snowball.getLastSnowballCount()
                + " snowball(s), raising it to freeze level "
                + target.getFreezeLevel() + "/"
                + BasePlant.MAX_FREEZE_LEVEL + "."
                + (snowball.didLastBarrageFreezeTarget()
                        ? " The plant is frozen inside a "
                                + BasePlant.ICE_SHELL_HIT_POINTS
                                + " HP ice shell."
                        : ""));
    }

    void activateWeaselRelease(Zombie hoarder, ZombieAbility ability) {
        if (!(ability instanceof WeaselReleaseAbility)
                || !ability.tryUse(hoarder, board)) {
            return;
        }
        WeaselReleaseAbility release = (WeaselReleaseAbility) ability;
        for (Zombie weasel : release.getLastSpawnedWeasels()) {
            trackSpawnedZombie(weasel);
        }
        pendingResults.add(hoarder.getName() + " released "
                + release.getLastSpawnedWeasels().size() + " weasel(s).");
    }

    void activateImpThrow(Zombie gargantuar, ZombieAbility ability) {
        if (!(ability instanceof ImpThrowAbility)
                || !ability.tryUse(gargantuar, board)) {
            return;
        }

        ImpThrowAbility impThrow = (ImpThrowAbility) ability;
        Zombie imp = impThrow.getSpawnedImp();
        if (imp == null) {
            return;
        }

        trackSpawnedZombie(imp);
        pendingResults.add(gargantuar.getName() + " threw "
                + imp.getName() + " into lane " + imp.getLane()
                + " at column "
                + String.format(Locale.ROOT, "%.0f", imp.getColumnPosition())
                + ".");
    }

    void activateSunSteal(Zombie raZombie, ZombieAbility ability) {
        if (!(ability instanceof SunStealAbility)
                || !ability.tryUse(raZombie, board)) {
            return;
        }
        SunStealAbility sunSteal = (SunStealAbility) ability;
        pendingResults.add(raZombie.getName() + " pulled and stole "
                + sunSteal.getLastStolenAmount() + " sun.");
    }

    void activateTombSummon(Zombie tombRaiser, ZombieAbility ability) {
        if (!(ability instanceof TombSummonAbility)
                || !ability.tryUse(tombRaiser, board)) {
            return;
        }
        TombSummonAbility tombSummon = (TombSummonAbility) ability;
        pendingResults.add(tombRaiser.getName() + " raised "
                + tombSummon.getLastSpawnedCount() + " grave(s) at "
                + tombSummon.getLastSpawnedPositions() + ".");
    }

    void returnStolenSunFromDeadZombies(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof SunStealAbility)) {
                    continue;
                }
                int returnedSun = ((SunStealAbility) ability).releaseStolenSun();
                if (returnedSun > 0) {
                    addSun(returnedSun);
                    pendingResults.add(returnedSun + " stolen sun returned after "
                            + zombie.getName() + " died.");
                }
            }
        }
    }

    void returnCrystalSkullSunFromDeadZombies(
            List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof LaserBeamAbility)) {
                    continue;
                }
                int droppedSun =
                        ((LaserBeamAbility) ability)
                                .releaseHalfStolenSun();
                if (droppedSun <= 0) {
                    continue;
                }
                EntityPosition position = new EntityPosition(
                        zombie.getLane(),
                        Math.max(0, Math.min(
                                board.getNumberOfColumns() - 1,
                                (int) Math.floor(
                                        zombie.getColumnPosition()))));
                board.addEntity(Sun.createPlantSun(
                        droppedSun, position));
                pendingResults.add(zombie.getName() + " dropped "
                        + droppedSun + " stolen sun on death.");
            }
        }
    }

    void restoreWizardSheepFromDeadZombies(
            List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof WizardSpellAbility)) {
                    continue;
                }
                int restored =
                        ((WizardSpellAbility) ability)
                                .restoreTransformedPlants();
                if (restored > 0) {
                    pendingResults.add(restored
                            + " cat-transformed plant(s) returned to normal after "
                            + zombie.getName() + " died.");
                }
            }
        }
    }

    void trackBoardSpawnedZombies() {
        for (Zombie zombie : board.drainSpawnedZombies()) {
            trackSpawnedZombie(zombie);
        }
    }

    void trackSpawnedZombie(Zombie zombie) {
        applyDifficultyToZombie(zombie);
        int waveIndex = zombie.getWaveNumber() - 1;
        if (waveIndex < 0 || waveIndex >= spawnedZombiesByWave.size()) {
            return;
        }
        List<Zombie> waveZombies = spawnedZombiesByWave.get(waveIndex);
        if (!waveZombies.contains(zombie)) {
            waveZombies.add(zombie);
            onZombieSpawned(zombie);
        }
    }

    void updatePlantCooldowns(float deltaSeconds) {
        plantCooldowns.replaceAll((name, remaining) ->
                Math.max(0.0, remaining - deltaSeconds));
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, Double> entry : plantCooldowns.entrySet()) {
            if (entry.getValue() <= TIME_EPSILON) {
                expiredKeys.add(entry.getKey());
            }
        }
        for (String key : expiredKeys) {
            plantCooldowns.remove(key);
            plantCooldownFamilies.remove(key);
        }
    }

    void applyPlantCooldownResetRequests() {
        for (PlantFamily family : board.drainPlantCooldownResetRequests()) {
            List<String> resetKeys = new ArrayList<>();
            for (Map.Entry<String, PlantFamily> entry : plantCooldownFamilies.entrySet()) {
                if (entry.getValue() == family) {
                    resetKeys.add(entry.getKey());
                }
            }
            for (String key : resetKeys) {
                plantCooldowns.remove(key);
                plantCooldownFamilies.remove(key);
            }
        }
    }

    double getPlantCooldownRemaining(BasePlant plant) {
        return plantCooldowns.getOrDefault(getCooldownKey(plant), 0.0);
    }

    void startPlantCooldown(BasePlant plant) {
        double rechargeSeconds = Math.max(0.0, plant.getRechargeSeconds());
        if (rechargeSeconds > TIME_EPSILON) {
            String key = getCooldownKey(plant);
            plantCooldowns.put(key, rechargeSeconds);
            PlantFamily family = PlantFamily.findForPlant(plant);
            if (family == null) {
                plantCooldownFamilies.remove(key);
            } else {
                plantCooldownFamilies.put(key, family);
            }
        }
    }
}
