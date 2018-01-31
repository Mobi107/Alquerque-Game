package qirkat;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Observable;
import java.util.Observer;
import java.util.Stack;

import static qirkat.PieceColor.*;
import static qirkat.Move.*;

/** A Qirkat board.   The squares are labeled by column (a char value between
 *  'a' and 'e') and row (a char value between '1' and '5'.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (with row 0 being the bottom row)
 *  counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Mudabbir Khan
 */
class Board extends Observable {

    /** New board at the start of game. */
    private PieceColor[] _board = new PieceColor[SIDE * SIDE];

    /** Stack to keep track of all moves. */
    private Stack<Move> _completedMoves = new Stack<>();

    /** Array of chars to keep track of illegal horizontal moves. */
    private char[] _backTrack = new char[SIDE * SIDE];

    /** Stack to keep track of previous backtracks. */
    private Stack<char[]> _trackHistory = new Stack<>();

    /** A new, cleared board at the start of the game. */
    Board() {
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        internalCopy(b);
    }

    /** Return a constant view of me (allows any access method, but no
     *  method that modifies it). */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions. */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;
        _completedMoves.clear();
        _trackHistory.clear();

        setPieces("w w w w w w w w w w b b - w w b b b b b b b b b b",
                _whoseMove);

        setChanged();
        notifyObservers();
    }

    /** Copy B into me. */
    void copy(Board b) {
        internalCopy(b);
    }

    /** Copy B into me. */
    private void internalCopy(Board b) {
        for (int i = 0; i < b._board.length; i += 1) {
            set(i, b._board[i]);
        }
        for (int j = 0; j < b._backTrack.length; j += 1) {
            _backTrack[j] = b._backTrack[j];
        }
        this._whoseMove = b.whoseMove();
        this._gameOver = b.gameOver();
        this._completedMoves.addAll(b._completedMoves);
        this._trackHistory.addAll(b._trackHistory);
    }

    /** Set my contents as defined by STR.  STR consists of 25 characters,
     *  each of which is b, w, or -, optionally interspersed with whitespace.
     *  These give the contents of the Board in row-major order, starting
     *  with the bottom row (row 1) and left column (column a). All squares
     *  are initialized to allow horizontal movement in either direction.
     *  NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }

        if (!str.contains("-")) {
            throw new IllegalArgumentException("bad board description");
        }
        _whoseMove = nextMove;

        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b': case 'B':
                set(k, BLACK);
                break;
            case 'w': case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
        }
        for (int i = 0; i < _backTrack.length; i += 1) {
            _backTrack[i] = '-';
        }
        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if the current player has
     *  no moves. */
    boolean gameOver() {
        return _gameOver;
    }

    /** Return the current contents of square C R, where 'a' <= C <= 'e',
     *  and '1' <= R <= '5'.  */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);
        return get(index(c, r));
    }

    /** Return the current contents of the square at linearized index K. */
    PieceColor get(int k) {
        assert validSquare(k);
        return _board[k];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'e', and
     *  '1' <= R <= '5'. */
    public void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        set(index(c, r), v);
    }

    /** Set get(K) to V, where K is the linearized index of a square. */
    public void set(int k, PieceColor v) {
        assert validSquare(k);
        _board[k] = v;
    }

    /** Return true iff MOV is legal on the current board. */
    boolean legalMove(Move mov) {
        if (get(mov.fromIndex()) != whoseMove()) {
            return false;
        }
        if (get(mov.toIndex()) == EMPTY) {
            if (jumpPossible()) {
                if (mov.isJump()) {
                    return checkJump(mov, false);
                } else {
                    return false;
                }
            } else if ((mov.isLeftMove() && _backTrack[mov.fromIndex()] == 'l')
                    || (mov.isRightMove()
                    && _backTrack[mov.fromIndex()] == 'r')) {
                return false;
            } else if (((whoseMove() == WHITE && mov.fromIndex() >= SIDE * 4)
                    || (whoseMove() == BLACK && mov.fromIndex() < SIDE))
                    && (mov.isLeftMove() || mov.isRightMove())) {
                return false;
            } else {
                return mov.isSingleMove(whoseMove());
            }
        }
        return false;
    }

    /** Return a list of all legal moves from the current position. */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        return result;
    }

    /** Add all legal moves from the current position to MOVES. */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {
            return;
        }
        if (jumpPossible()) {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        } else {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getMoves(moves, k);
            }
        }
    }

    /** Add all legal non-capturing moves from the position
     *  with linearized index K to MOVES. */
    public void getMoves(ArrayList<Move> moves, int k) {
        for (int i = 0; i < _board.length; i += 1) {
            Move mov = Move.move(k, i);
            if (!mov.isJump() && legalMove(mov)) {
                moves.add(mov);
            }
        }
    }

    /** Add all legal captures from the position with linearized index K
     *  to MOVES. */
    public void getJumps(ArrayList<Move> moves, int k) {
        for (int i = 0; i < _board.length; i += 1) {
            Move mov = Move.move(k, i);
            if (jumpPossible(k, i)) {
                set(mov.fromIndex(), EMPTY);
                set(mov.toIndex(), whoseMove());
                set(mov.jumpedIndex(), EMPTY);
                ArrayList<Move> jumps = new ArrayList<>();
                getJumps(jumps, i);
                if (jumps.isEmpty()) {
                    moves.add(mov);
                } else {
                    for (Move m : jumps) {
                        moves.add(Move.move(mov, m));
                    }
                }
                set(mov.fromIndex(), whoseMove());
                set(mov.toIndex(), EMPTY);
                set(mov.jumpedIndex(), whoseMove().opposite());
            }
        }
    }

    /** Return true iff MOV is a valid jump sequence on the current board.
     *  MOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     *  could be continued and are valid as far as they go.  */
    boolean checkJump(Move mov, boolean allowPartial) {
        if (mov == null) {
            return true;
        }
        if (!mov.isJump()) {
            return false;
        }
        if (!mov.isValidJump(mov.fromIndex(), mov.toIndex())) {
            return false;
        }
        if (get(mov.jumpedIndex()) != whoseMove().opposite()
                || get(mov.toIndex()) != EMPTY) {
            return false;
        }
        if (!allowPartial) {
            set(mov.fromIndex(), EMPTY);
            set(mov.toIndex(), whoseMove());
            set(mov.jumpedIndex(), EMPTY);
            boolean b;
            if (mov.jumpTail() == null) {
                b = !jumpPossible(mov.toIndex());
                set(mov.fromIndex(), whoseMove());
                set(mov.toIndex(), EMPTY);
                set(mov.jumpedIndex(), whoseMove().opposite());
                return b;
            } else {
                b = checkJump(mov.jumpTail(), allowPartial);
                set(mov.fromIndex(), whoseMove());
                set(mov.toIndex(), EMPTY);
                set(mov.jumpedIndex(), whoseMove().opposite());
                return b;
            }
        }
        return true;
    }

    /** Return true iff a jump is possible for a piece at position C R. */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /** Return true iff a jump is possible for a piece at position with
     *  linearized index K. */
    boolean jumpPossible(int k) {
        for (int i = 0; i < _board.length; i += 1) {
            if (jumpPossible(k, i)) {
                return true;
            }
        }
        return false;
    }

    /** Return true iff a jump is possible from index K to index I. */
    boolean jumpPossible(int k, int i) {
        Move mov = Move.move(k, i);
        return (mov.isValidJump(k, i) && get(mov.fromIndex()) == whoseMove()
                && get(mov.jumpedIndex()) == whoseMove().opposite()
                && get(mov.toIndex()) == EMPTY);
    }

    /** Return true iff a jump is possible from the current board. */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Perform the move C0R0-C1R1. Assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null));
    }

    /** Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     *  Assumes the result is legal. */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next));
    }

    /** Make the Move MOV on this Board, assuming it is legal. */
    void makeMove(Move mov) {
        if (!legalMove(mov)) {
            System.out.println("Illegal move");
        } else {
            _trackHistory.push(_backTrack);
            if (mov.isJump()) {
                makeJump(mov);
            } else {
                if (mov.isLeftMove()) {
                    _backTrack[mov.toIndex()] = 'r';
                } else if (mov.isRightMove()) {
                    _backTrack[mov.toIndex()] = 'l';
                }
                set(mov.fromIndex(), EMPTY);
                set(mov.toIndex(), whoseMove());
            }
            _backTrack[mov.fromIndex()] = '-';
            _whoseMove = whoseMove().opposite();
            _completedMoves.push(mov);
            _gameOver = !isMove();

            setChanged();
            notifyObservers();
        }
    }

    /** Make a jump if the move MOV is a jump. */
    void makeJump(Move mov) {
        while (mov.jumpTail() != null) {
            set(mov.fromIndex(), EMPTY);
            set(mov.jumpedIndex(), EMPTY);
            _backTrack[mov.jumpedIndex()] = '-';
            mov = mov.jumpTail();
        }
        set(mov.fromIndex(), EMPTY);
        set(mov.jumpedIndex(), EMPTY);
        _backTrack[mov.jumpedIndex()] = '-';
        set(mov.toIndex(), whoseMove());
    }

    /** Undo the last move, if any. */
    void undo() {
        Move prev = _completedMoves.pop();
        char[] bt = _trackHistory.pop();
        _whoseMove = whoseMove().opposite();

        set(prev.fromIndex(), whoseMove());
        set(prev.toIndex(), EMPTY);

        if (prev.isJump()) {
            undoJumps(prev);
        }
        for (int i = 0; i < bt.length; i += 1) {
            _backTrack[i] = bt[i];
        }

        setChanged();
        notifyObservers();
    }

    /** Undo jumps if there were any jumps in the last move MOV. */
    void undoJumps(Move mov) {
        set(mov.jumpedIndex(), whoseMove().opposite());
        set(mov.toIndex(), EMPTY);
        if (mov.jumpTail() != null) {
            undoJumps(mov.jumpTail());
        }
    }

    /** Returns the board of the current game. */
    public PieceColor[] getBoard() {
        return _board;
    }

    /** Returns the stack of completed moves. */
    public Stack getMoveHistory() {
        return _completedMoves;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /** Return a text depiction of the board.  If LEGEND, supply row and
     *  column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        char[] cols = {'a', 'b', 'c', 'd', 'e'};
        char[] rows = {'5', '4', '3', '2', '1'};
        if (!legend) {
            for (char r : rows) {
                for (char c : cols) {
                    if (c == 'e' && r != '1') {
                        out.format("%s\n", get(c, r).shortName());
                    } else if (c == 'e') {
                        out.format("%s", get(c, r).shortName());
                    } else if (c == 'a') {
                        out.format("  %s ", get(c, r).shortName());
                    } else {
                        out.format("%s ", get(c, r).shortName());
                    }
                }
            }
        } else {
            for (char r : rows) {
                out.format("%s", r);
                for (char c : cols) {
                    if (c == 'e') {
                        out.format("%s\n", get(c, r).shortName());
                    } else if (c == 'a') {
                        out.format("  %s ", get(c, r).shortName());
                    } else {
                        out.format("%s ", get(c, r).shortName());
                    }
                }
            }
            for (char c : cols) {
                if (c == 'a') {
                    out.format("   %s ", c);
                } else if (c == 'e') {
                    out.format("%s", c);
                } else {
                    out.format("%s ", c);
                }
            }
        }
        return out.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Board) {
            return this.toString().equals(obj.toString())
                    && this.whoseMove() == ((Board) obj).whoseMove()
                    && this.gameOver() == ((Board) obj).gameOver();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /** Return true iff there is a move for the current player. */
    private boolean isMove() {
        return !getMoves().isEmpty();
    }


    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Set true when game ends. */
    private boolean _gameOver;

    /** Convenience value giving values of pieces at each ordinal position. */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /** One cannot create arrays of ArrayList<Move>, so we introduce
     *  a specialized private list type for this purpose. */
    private static class MoveList extends ArrayList<Move> {
    }

    /** A read-only view of a Board. */
    private class ConstantBoard extends Board implements Observer {
        /** A constant view of this Board. */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move) {
            assert false;
        }

        /** Undo the last move. */
        @Override
        void undo() {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }
}
