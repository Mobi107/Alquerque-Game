package qirkat;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Mudabbir Khan
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
        _prompt = myColor + ": ";
    }

    @Override
    Move myMove() {
        try {
            Command cmd = game().getMoveCmnd(_prompt);
            return Move.parseMove(cmd.operands()[0]);
        } catch (NullPointerException e) {
            return null;
        }
    }

    /** Identifies the player serving as a source of input commands. */
    private String _prompt;
}

