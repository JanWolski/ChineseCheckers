package jw.lab4.checkers;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages flow of the game.
 */
public class Board {

  /** Numbers of player in game. */
  public int startingPlayer = -1;

  /** Numbers of player in game. */
  private int playersNum = 0;

  /** Game turn. */
  private int turn = 0;

  /** Which player has this turn. */
  private int playerTurn = -1;

  /** List of ready players. */
  private int[] readyPlayers;

  /** Transformation between base players and players on board. */
  private int[] playerTrans;

  /** List of winners from first winner to last. */
  public List<Integer> winners = new ArrayList<Integer>();

  /** Fields on board. */
  public Field[] fields;
  /** Fields on board by position. */
  public Field[][] fieldsPos;

  /** If game stareted. */
  public boolean started = false;
  public boolean finished = false;

  /** Field on which pawn finished. */
  public Field nextMove = null;

  /** Width of the board. */
  public int width;
  /** Height of the board. */
  public int height;

  /** Maximum number of players. */
  public final int maxPlayers = 6;

  Board() {
    readyPlayers = new int[maxPlayers];
  }

  public int getPlayer() {
    return playerTurn;
  }

  /**
   * Setup the board.
   */
  public void createBoard() {
    FieldsFactory construct = new FieldsFactory();
    construct.constructStar();
    fields = construct.getFields();
    fieldsPos = construct.getFieldsPos();
    width = construct.width;
    height = construct.height;
  }

  /**
   * Set board for specified number of players.
   * 
   * @param players Number of players.
   * @throws InvalidMove if game started, or invalid number of players given
   */
  public void setPlayers(int players) throws InvalidMove {
    if (started) {
      throw new InvalidMove("Game already started.");
    } else {
      playersNum = players;
      switch (playersNum) {
        case 0:
          activateBases(new int[] { -2 });
          break;
        case 1:
          activateBases(new int[] { 0 });
          break;
        case 2:
          activateBases(new int[] { 0, 3 });
          break;
        case 3:
          activateBases(new int[] { 0, 2, 4 });
          break;
        case 4:
          activateBases(new int[] { 0, 1, 3, 4 });
          break;
        case 5:
          activateBases(new int[] { 0, 1, 3, 4, 5 });
          break;
        case 6:
          activateBases(new int[] { 0, 1, 2, 3, 4, 5 });
          break;
        default:
          throw new InvalidMove("Invalid number of players.");

      }
    }
  }

  /**
   * Rearange bases for specific number of bases.
   * 
   * @param bases Bases to activate.
   */
  private void activateBases(int[] bases) {
    playerTrans = new int[bases.length];
    for (int i = 0; i < bases.length; i++) {
      playerTrans[i] = bases[i];
    }

    for (Field f : fields) {
      int i = 0;
      for (int b : bases) {
        if (f.base == b) {
          f.player = f.base;
          break;
        }
        i++;
      }
      if (i == bases.length) {
        f.player = -1;
      }
    }
  }

  /**
   * Starts the game.
   * 
   * @throws InvalidMove When number of players is not valid.
   */
  public void start() throws InvalidMove {
    if (playersNum == 5 || playersNum > 6 || playersNum <= 1) {
      throw new InvalidMove("Invalid number of players");
    }

    int[] transform = new int[maxPlayers];

    for (int baseNum = 0; baseNum < maxPlayers; baseNum++) {
      int b = 0;
      while (b < playerTrans.length && baseNum != playerTrans[b]) {
        b++;
      }

      if (b < playerTrans.length) {

        int p = 0;
        while (b > 0) {
          if (readyPlayers[p] > 0) {
            b--;
          }
          p++;
        }

        transform[baseNum] = p;
      } else {
        transform[baseNum] = -1;
      }

    }

    for (Field f : fields) {
      if (f.base > -1) {
        f.base = (f.base + maxPlayers / 2) % maxPlayers;
        f.base = transform[f.base];
      }
      if (f.player > -1) {
        f.player = transform[f.player];

      }
    }

    playerTurn = startingPlayer % playersNum;
    started = true;
  }

  /**
   * Set player ready to set to ready status.
   * 
   * @param player Player to set ready.
   */
  public void setReady(int player) throws InvalidMove {
    if (started) {
      throw new InvalidMove("Game already started.");
    } else {
      readyPlayers[player] = 1;
      int count = 0;
      for (int r = 0; r < maxPlayers; r++) {
        count += readyPlayers[r];
      }
      if (count == playersNum) {
        start();
      } else if (count > playersNum) {
        throw new InvalidMove("Too many ready players");
      }
    }
  }

  /**
   * Interprets fields in move instruction to get move direction.
   */
  public int getDir(MoveInstructions instr) throws InvalidMove {
    int f1 = instr.field1;
    int f2 = instr.field2;
    int d;
    for (d = 0; d < fields[0].neighbours.length; d++) {
      if (fields[f1].neighbours[d] == fields[f2]
          || (fields[f1].neighbours[d] != null && fields[f1].neighbours[d].neighbours[d] == fields[f2])) {
        break;
      }
    }

    if (d == fields[0].neighbours.length) {
      throw new InvalidMove("Invalid direction");
    }

    return d;
  }

  /**
   * Interprets which action to take.
   * 
   * @param instr Instruction to execute.
   * @return Received instruction.
   */
  public MoveInstructions interpretMove(MoveInstructions instr) throws InvalidMove {

    if (instr != null) {
      if (instr.player == -1) {
        throw new InvalidMove("Player not set.");
      }
      switch (instr.state) {
        case JOIN:
          setPlayers(instr.player);
          break;
        case PLAY:
          move(instr);
          break;
        case LOAD:
        case READY:
          setReady(instr.player);
          break;
        case NEXT:
          nextTurn();
          break;
        default:
          throw new InvalidMove();
      }
    }

    return instr;
  }

  public boolean checkPlayer(int player, int field) {
    return (fields[field].player == playerTurn && playerTurn == player);
  }

  public MoveInstructions move(MoveInstructions instr) throws InvalidMove {
    if (instr.field1 == -1 || instr.field2 == -1 || instr.player == -1) {
      throw new InvalidMove("Invalid fields.");
    }
    if (started) {
      if (instr.player != playerTurn) {
        throw new InvalidMove("Wrong player");
      }

      if (nextMove != null && (nextMove != fields[instr.field1] || nextMove.jumped == false)) {
        throw new InvalidMove("Other checker already moved.");
      }

      int dir = getDir(instr);

      Field finishField = fields[instr.field1].move(instr.player, dir);

      if (finishField == null) {
        throw new InvalidMove("Illegal move");
      }

      nextMove = finishField;
    } else {
      throw new InvalidMove("Game not started yet.");
    }
    return instr;
  }

  /**
   * Checks which players won.
   */
  private void checkWin() {
    int[] pl = new int[6];
    for (Field f : fields) {
      if (f.base != -1 && f.player == f.base) {
        pl[f.player]++;
      }
    }
    for (int i = 0; i < playersNum; i++) {
      if (!winners.contains(i)) {
        if (pl[i] >= 10) {
          winners.add(i);
          if (winners.size() == playersNum - 1) {
            finished = true;
            checkWin();
          }
        } else if (finished) {
          winners.add(i);
        }
      }
    }
  }

  /**
   * Passes turn to another player.
   */
  public void nextTurn() {
    if (nextMove != null) {
      nextMove.jumped = false;
    }
    nextMove = null;
    checkWin();

    do {
      playerTurn++;
    } while (winners.contains(playerTurn));
    turn++;
    if (playerTurn >= playersNum) {
      playerTurn = 0;
    }
  }

}
