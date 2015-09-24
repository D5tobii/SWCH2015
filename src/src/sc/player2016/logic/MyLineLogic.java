package sc.player2016.logic;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sc.player2016.Starter;
import sc.plugin2016.Board;
import sc.plugin2016.Field;
import sc.plugin2016.FieldType;
import sc.plugin2016.GameState;
import sc.plugin2016.IGameHandler;
import sc.plugin2016.Move;
import sc.plugin2016.Player;
import sc.plugin2016.PlayerColor;
import sc.shared.GameResult;

public class MyLineLogic implements IGameHandler {

    private Starter client;
    private GameState gameState;
    private Player currentPlayer;
    private PlayerColor color;
    private String start = "";

    /*
     * Klassenweit verfuegbarer Zufallsgenerator der beim Laden der klasse
     * einmalig erzeugt wird und dann immer zur Verfuegung steht.
     */
    private static final Random rand = new SecureRandom();

    /**
     * Erzeugt ein neues Strategieobjekt.
     * 
     * @param client
     *            Der Zugrundeliegende Client der mit dem Spielserver
     *            kommunizieren kann.
     */
    public MyLineLogic(Starter client) {
	this.client = client;
	this.color = client.getMyColor();
	System.out.println("My Color has been set to " + color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gameEnded(GameResult data, PlayerColor color, String errorMessage) {

	System.out.println("*** Das Spiel ist beendet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO in der Mitte starten, gucken wo der gegner startet :)
    public void onRequestAction() {
	// Konsolendokumentation
	System.out.println("*** Es wurde ein Zug angefordert");
	// Beziehe alle möglichen Züge
	List<Move> possibleMoves = gameState.getPossibleMoves();
	// Konsolendokumentation
	System.out.println("*** sende zug: ");

	// ---Setze meine Color, falls nicht schon geschehen
	setMyColor();

	// ---Hole aktuelles Spielbrett
	Board gameBoard = gameState.getBoard();
	// ---Schreibe alle meine Felder in ein Array
	ArrayList<Field> myFields = new ArrayList<>();
	for (int i = 0; i < gameBoard.getFields().length; i++) {
	    for (int j = 0; j < gameBoard.getFields().length; j++) {
		if (gameBoard.getOwner(i, j) == color) {
		    myFields.add(gameBoard.getField(i, j));
		}
	    }
	}
	// ---Konsolendokumentation
	for (Field field : myFields) {
	    System.out.println(field);
	}
	// ---Erstelle Listen aller möglichen StartMoves, in der eigenen "Zone"
	ArrayList<Move> startMoves = collectStartMoves(possibleMoves, gameBoard);

	// ---Überprufe wo im ersten Zug bekommen wurde
	checkStart(myFields);

	// ---Sammele alle lineMoves
	ArrayList<Move> lineMoves = collectLineMoves(possibleMoves, myFields);

	Move selection = possibleMoves.get(rand.nextInt(possibleMoves.size()));
	Move selectionFirst = startMoves.get(rand.nextInt(startMoves.size()));
	if (myFields.isEmpty()) {
	    sendAction(selectionFirst);
	    System.out.println("*** setze Strommast auf x=" + selectionFirst.getX() + ", y=" + selectionFirst.getY());
	} else {
	    if (!lineMoves.isEmpty()) {
		Move selectionLine = findBestMove(lineMoves);
		sendAction(selectionLine);
		System.out.println("*** setze Strommast auf x=" + selectionLine.getX() + ", y=" + selectionLine.getY());

	    } else {
		sendAction(selection);
		System.out.println("*** setze Strommast auf x=" + selection.getX() + ", y=" + selection.getY());
	    }
	}
    }

    private ArrayList<Move> collectStartMoves(List<Move> possibleMoves, Board gameBoard) {
	ArrayList<Move> startMoves = new ArrayList<>();
	for (Move move : possibleMoves) {
	    if (color == PlayerColor.BLUE) {
		if (gameBoard.getField(move.getX(), move.getY()).getType() == FieldType.BLUE) {
		    if (move.getY() >= 10 && move.getY() <= 14) {
			startMoves.add(move);
		    }
		}
	    }
	    if (color == PlayerColor.RED) {
		if (gameBoard.getField(move.getX(), move.getY()).getType() == FieldType.RED) {
		    if (move.getX() >= 10 && move.getX() <= 14) {
			startMoves.add(move);
		    }
		}
	    }
	}
	return startMoves;
    }

    private ArrayList<Move> collectLineMoves(List<Move> possibleMoves, ArrayList<Field> myFields) {
	ArrayList<Move> lineMoves = new ArrayList<>();
	for (Field field : myFields) {
	    int x1 = field.getX();
	    int y1 = field.getY();
	    int x2 = -1;
	    int y2 = -1;
	    for (Move move : possibleMoves) {
		x2 = move.getX();
		y2 = move.getY();
		if (checkPossibleWire(x1, y1, x2, y2)) {
		    lineMoves.add(move);
		}
	    }
	}
	return lineMoves;
    }

    private void setMyColor() {
	if (color == null) {
	    color = currentPlayer.getPlayerColor();
	    System.out.println("Color set manually to " + currentPlayer.getPlayerColor() + " :(");
	}
    }

    private void checkStart(ArrayList<Field> myFields) {
	if (start.equals("") && (!myFields.isEmpty())) {
	    Field firstField = myFields.get(0);
	    if (firstField.getY() == 0) {
		start = "LEFT";
	    }
	    if (firstField.getY() == gameState.getBoard().getFields().length - 1) {
		start = "RIGHT";
	    }
	    if (firstField.getX() == 0) {
		start = "TOP";
	    }
	    if (firstField.getX() == gameState.getBoard().getFields().length - 1) {
		start = "BOTTOM";
	    }
	    System.out.println("Start has been made " + start);
	}
    }

    private Move findBestMove(ArrayList<Move> moves) {
	Move move = null;
	switch (start) {
	case "LEFT":
	    for (Move m : moves) {
		if (move == null) {
		    move = m;
		} else {
		    if (m.getY() > move.getY()) {
			move = m;
		    }
		}
	    }
	    System.out.println("Best Move has been picked at x=" + move.getX() + ", y=" + move.getY());
	    return move;
	case "RIGHT":
	    for (Move m : moves) {
		if (move == null) {
		    move = m;
		} else {
		    if (m.getY() < move.getY()) {
			move = m;
		    }
		}
	    }
	    System.out.println("Best Move has been picked at x=" + move.getX() + ", y=" + move.getY());
	    return move;
	case "BOTTOM":
	    for (Move m : moves) {
		if (move == null) {
		    move = m;
		} else {
		    if (m.getX() < move.getX()) {
			move = m;
		    }
		}
	    }
	    System.out.println("Best Move has been picked at x=" + move.getX() + ", y=" + move.getY());
	    return move;
	case "TOP":
	    for (Move m : moves) {
		if (move == null) {
		    move = m;
		} else {
		    if (m.getX() > move.getX()) {
			move = m;
		    }
		}
	    }
	    System.out.println("Best Move has been picked at x=" + move.getX() + ", y=" + move.getY());
	    return move;
	}

	return null;
    }

    public boolean checkPossibleWire(int x1, int y1, int x2, int y2) {
	return ((Math.abs(x1 - x2) == 2 && Math.abs(y1 - y2) == 1) || (Math.abs(x1 - x2) == 1 && Math.abs(y1 - y2) == 2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(Player player, Player otherPlayer) {
	currentPlayer = player;

	System.out.println("*** Spielerwechsel: " + player.getPlayerColor());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(GameState gameState) {
	this.gameState = gameState;
	currentPlayer = gameState.getCurrentPlayer();

	System.out.print("*** Das Spiel geht vorran: Zug = " + gameState.getTurn());
	System.out.println(", Spieler = " + currentPlayer.getPlayerColor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendAction(Move move) {
	client.sendMove(move);
    }

}
