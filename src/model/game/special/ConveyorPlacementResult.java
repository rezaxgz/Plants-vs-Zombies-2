package model.game.special;

/**
 * Result of trying to place one Conveyor Belt plant packet.
 */
public enum ConveyorPlacementResult {
    SUCCESS,
    NOT_CONVEYOR_LEVEL,
    INVALID_PACKET,
    INVALID_POSITION,
    POSITION_OCCUPIED,
    UNKNOWN_PLANT
}
