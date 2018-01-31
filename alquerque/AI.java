package qirkat;

import static qirkat.PieceColor.*;
import java.util.ArrayList;

/** A Player that computes its own moves.
 *  @author Mudabbir Khan
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 5;
    /** A position magnitude indicating a win (for white if positive, black
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();

        if (myColor() == WHITE) {
            game().reportMove("White moves " + move + ".");
        } else {
            game().reportMove("Black moves " + move + ".");
        }
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        Move best = null;

        if (depth == 0) {
            return staticScore(board);
        } else {
            int moveFound;
            ArrayList<Move> moves = board.getMoves();
            ArrayList<Move> movesPossible = new ArrayList<>();
            ArrayList<Move> jumpsPossible = new ArrayList<>();

            for (Move mov: moves) {
                if (beta <= alpha) {
                    break;
                }
                if (board.legalMove(mov)) {
                    if (mov.isJump()) {
                        jumpsPossible.add(mov);
                    } else {
                        movesPossible.add(mov);
                    }
                }
                Board temp = new Board(board);
                temp.makeMove(mov);
                moveFound = findMove(temp, depth - 1,
                        false, -sense, alpha, beta);

                if (sense == 1
                        && moveFound > alpha) {
                    alpha = moveFound;
                    best = mov;
                } else if (sense == -1
                        && moveFound < beta) {
                    beta = moveFound;
                    best = mov;
                }
            }
            if (!board.gameOver()) {
                if (best == null && saveMove) {
                    if (jumpsPossible.size() > 0) {
                        best = jumpsPossible.get(game().
                                nextRandom(jumpsPossible.size()));
                    } else {
                        best = movesPossible.get(game().
                                nextRandom(movesPossible.size()));
                    }
                }
            }
        }

        if (saveMove) {
            _lastFoundMove = best;
        }

        return sense == -1 ? beta : alpha;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        int whitePieces = 0 , blackPieces = 0;
        PieceColor[] b = board.getBoard();
        for (int i = 0; i < b.length; i += 1) {
            if (b[i] == WHITE) {
                whitePieces += 1;
            } else if (b[i] == BLACK) {
                blackPieces -= 1;
            }
        }

        ArrayList<Move> moves = board.getMoves();
        int jumps = 0;
        int maxLength = moves.size() > 0 ? moveSize(moves.get(0)) : 0;
        for (int i = 1; i < moves.size(); i += 1) {
            Move move1 = moves.get(i);
            if (move1.isJump()) {
                jumps += 1;
            }
            maxLength = Math.max(maxLength, moveSize(move1));
        }
        if (board.whoseMove() == WHITE) {
            return whitePieces + blackPieces + jumps + maxLength;
        } else {
            return whitePieces + blackPieces - jumps - maxLength;
        }
    }

    /** Returns the size of the jump move MOVE. */
    private int moveSize(Move move) {
        int size = 0;
        while (move.jumpTail() != null) {
            size += 1;
            move = move.jumpTail();
        }

        return size;
    }
}
