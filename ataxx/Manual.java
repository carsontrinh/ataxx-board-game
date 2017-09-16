package ataxx;

import static ataxx.PieceColor.*;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Carson Trinh
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Move m = null;
        Command cmd = game().getMoveCmnd(myColor().toString() + ": ");
        if (cmd == null) {
            return null;
        }
        String[] op = cmd.operands();
        if (cmd.commandType() == Command.Type.PASS) {
            m = Move.pass();
        } else if (cmd.commandType() == Command.Type.PIECEMOVE) {
            m = Move.move(op[0].charAt(0), op[1].charAt(0), op[2].charAt(0),
                    op[3].charAt(0));
        }
        return m;
    }
}

