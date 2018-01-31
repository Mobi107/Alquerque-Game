package qirkat;

import ucb.gui2.TopLevel;
import ucb.gui2.LayoutSpec;

import java.util.EmptyStackException;
import java.util.Observable;
import java.util.Observer;

import java.io.Writer;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;


/** The GUI for the Qirkat game.
 *  @author Mudabbir Khan
 */
class GUI extends TopLevel implements Observer, Reporter {

    /* The implementation strategy applied here is to make it as
     * unnecessary as possible for the rest of the program to know that it
     * is interacting with a GUI as opposed to a terminal.
     *
     * To this end, we first have made Board observable, so that the
     * GUI gets notified of changes to a Game's board and can interrogate
     * it as needed, while the Game and Board themselves need not be aware
     * that it is being watched.
     *
     * Second, instead of creating a new API by which the GUI communicates
     * with a Game, we instead simply arrange to make the GUI's input look
     * like that from a terminal, so that we can reuse all the machinery
     * in the rest of the program to interpret and execute commands.  The
     * GUI simply composes commands (such as "start" or "clear") and
     * writes them to a Writer that (using the Java library's PipedReader
     * and PipedWriter classes) provides input to the Game using exactly the
     * same API as would be used to read from a terminal. Thus, a simple
     * Manual player can handle all commands and moves from the GUI.
     *
     * See also Main.java for how this might get set up.
     */

    /** Minimum size of board in pixels. */
    private static final int MIN_SIZE = 300;

    /** A new display observing MODEL, with TITLE as its window title.
     *  It uses OUTCOMMANDS to send commands to a game instance, using the
     *  same commands as the text format for Qirkat. */
    GUI(String title, Board model, Writer outCommands) {
        super(title, true);
        addMenuButton("Game->New", this::doNew);
        addMenuButton("Game->Start", this::start);
        addMenuButton("Game->Restart", this::restart);
        addMenuButton("Game->Undo", this::doUndo);
        addMenuButton("Game->Quit", this::quit);
        addMenuCheckBox("Game->Set Pieces", false, this::setPieces);
        addMenuButton("Options->Seed...", this::setSeed);
        addSeparator("Options->Players");
        addMenuButton("Info->Help", this::help);
        addMenuButton("Info->Rules", this::rules);
        addMenuRadioButton("Options->Players->White AI",
                "White", false, this::setWhiteAI);
        addMenuRadioButton("Options->Players->White Manual",
                "White", true, this::setWhiteManual);
        addMenuRadioButton("Options->Players->Black AI",
                "Black", true, this::setBlackAI);
        addMenuRadioButton("Options->Players->Black Manual",
                "Black", false, this::setBlackManual);
        addLabel("", "Message", new LayoutSpec("y", 2));
        _model = model;
        _widget = new BoardWidget(model);
        _out = new PrintWriter(outCommands, true);
        add(_widget,
            new LayoutSpec("height", "1",
                           "width", "REMAINDER",
                           "ileft", 5, "itop", 5, "iright", 5,
                           "ibottom", 5));
        setMinimumSize(MIN_SIZE, MIN_SIZE);
        _widget.addObserver(this);
        _model.addObserver(this);
        moveMsg("%s to move", _model.whoseMove());
        start("");
    }

    /** Comment. */
    private synchronized void doNew(String unused) {
        _out.printf("clear%n");
    }

    /** Comment. */
    private synchronized void start(String unused) {
        _out.printf("start%n");
    }

    /** Comment. */
    private synchronized void restart(String unused) {
        _out.printf("clear%n");
        _out.printf("start%n");
    }

    /** Comment. */
    private synchronized void doUndo(String unused) {
        try {
            _model.undo();
            moveMsg("%s to move", _model.whoseMove());

        } catch (NullPointerException e1) {
            return;
        } catch (EmptyStackException e2) {
            return;
        }
    }

    /** Comment. */
    private synchronized void setPieces(String unused) {
        BoardWidget.activateSetup();
        moveMsg("Setting pieces");
    }

    /** Comment. */
    private synchronized void setWhiteAI(String unused) {
        _out.printf("auto white%n");
    }

    /** Comment. */
    private synchronized void setWhiteManual(String unused) {
        _out.printf("manual white%n");
    }

    /** Comment. */
    private synchronized void setBlackAI(String unused) {
        _out.printf("auto black%n");
    }

    /** Comment. */
    private synchronized void setBlackManual(String unused) {
        _out.printf("manual black%n");
    }

    /** Comment. */
    private synchronized void help(String unused) {
        showMessage("Moves.\n    To make a move, "
                + "click on the piece "
                + "you wish to move and then on each\n    "
                + "of the squares "
                + "you wish it to move in sequence.\n\n"
                + "Setting Player Types.\n"
                + "    Select menu item Options->Players "
                + "to set the white and "
                + "black players\n    to be either AIs or "
                + "manual players.\n\n"
                + "Starting and Restarting.\n    Selecting "
                + "menu item Game->Start"
                + " ends set-up mode and starts play.\n    "
                + "Selecting item "
                + "Game->New clears the board and goes back "
                + "to set-up\n    mode."
                + " Game->Restart has the same effect as "
                + "Clear followed by Start.\n\n"
                + "Setting the Random Seed.\n    Use "
                + "Options->Seed to restart the"
                + " random-number generator with a new "
                + "seed\n    value. The AI will"
                + " respond identically with identical "
                + "seeds, allowing\n    "
                + "re-running a game.\n\nManually Setting "
                + "Pieces.\n    "
                + "To set up an arbitrary position, click "
                + "Game->Set Pieces to "
                + "toggle\n    piece-setting mode on. When "
                + "this box is checked, "
                + "clicking on a piece\n    changes its "
                + "color in the sequence "
                + "Empty->White->Black->Empty....\n    The "
                + "last piece color to set"
                + " becomes the next player to move when\n   "
                + "play is resumed or "
                + "piece-setting mode is toggled off.",
                "Help", "answer");
    }

    /** The rules. */
    private synchronized void rules(String unused) {
        showMessage("Moves.\n    To make a move, "
                        + "click on the piece "
                        + "you wish to move and then on each\n    "
                        + "of the squares "
                        + "you wish it to move in sequence.\n\n"
                        + "Setting Player Types.\n"
                        + "    Select menu item Options->Players "
                        + "to set the white and "
                        + "black players\n    to be either AIs or "
                        + "manual players.\n\n"
                        + "Starting and Restarting.\n    Selecting "
                        + "menu item Game->Start"
                        + " ends set-up mode and starts play.\n    "
                        + "Selecting item "
                        + "Game->New clears the board and goes back "
                        + "to set-up\n    mode."
                        + " Game->Restart has the same effect as "
                        + "Clear followed by Start.\n\n"
                        + "Setting the Random Seed.\n    Use "
                        + "Options->Seed to restart the"
                        + " random-number generator with a new "
                        + "seed\n    value. The AI will"
                        + " respond identically with identical "
                        + "seeds, allowing\n    "
                        + "re-running a game.\n\nManually Setting "
                        + "Pieces.\n    "
                        + "To set up an arbitrary position, click "
                        + "Game->Set Pieces to "
                        + "toggle\n    piece-setting mode on. When "
                        + "this box is checked, "
                        + "clicking on a piece\n    changes its "
                        + "color in the sequence "
                        + "Empty->White->Black->Empty....\n    The "
                        + "last piece color to set"
                        + " becomes the next player to move when\n   "
                        + "play is resumed or "
                        + "piece-setting mode is toggled off.",
                "Help", "answer");
    }



    /** Execute the "Quit" button function. */
    private synchronized void quit(String unused) {
        _out.printf("quit%n");
    }

    /** Execute Seed... command. */
    private synchronized void setSeed(String unused) {
        String resp =
            getTextInput("Random Seed", "Get Seed", "question", "");
        if (resp == null) {
            return;
        }
        try {
            long s = Long.parseLong(resp);
            _out.printf("seed %d%n", s);
        } catch (NumberFormatException excp) {
            return;
        }
    }

    /** Display text in file NAME in a box titled TITLE. */
    private void displayText(String name, String title) {
        InputStream input =
            Game.class.getClassLoader().getResourceAsStream(name);
        if (input != null) {
            try {
                BufferedReader r
                    = new BufferedReader(new InputStreamReader(input));
                char[] buffer = new char[1 << 15];
                int len = r.read(buffer);
                showMessage(new String(buffer, 0, len), title, "plain");
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }



    @Override
    public void errMsg(String format, Object... args) {
        setLabel("Message", String.format(format, args));
    }

    @Override
    public void outcomeMsg(String format, Object... args) {
        setLabel("Message", String.format(format, args));
    }

    @Override
    public void moveMsg(String format, Object... args) {
        setLabel("Message", String.format(format, args));
    }

    @Override
    public void update(Observable obs, Object arg) {
        if (obs == _model) {
            _widget.update(_model, arg);
            moveMsg("%s to move", _model.whoseMove());
        } else if (obs == _widget) {
            _widget.update(_widget, arg);
        }
    }

    /** Respond to a click on SQ. */
    private void movePiece(String sq) {
        selectMove(Move.parseMove(sq));
    }

    /** Make MOV the user-selected move (no move if null). */
    private void selectMove(Move mov) {
        _selectedMove = mov;
        _widget.indicateMove(mov);
    }

    /** Contains the drawing logic for the Qirkat model. */
    private BoardWidget _widget;
    /** The model of the game. */
    private Board _model;
    /** Output sink for sending commands to a game. */
    private PrintWriter _out;
    /** Move selected by clicking. */
    private Move _selectedMove;
}
