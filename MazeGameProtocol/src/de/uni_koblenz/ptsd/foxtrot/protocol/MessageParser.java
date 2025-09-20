package de.uni_koblenz.ptsd.foxtrot.protocol;

import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.BaitPosCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.Command;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.InfoCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.JoinCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.LeaveCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.MazeCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.PlayerPosCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.PlayerScoreCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.QuitCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.ReadyCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.ServerVersionCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.TerminateCommand;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.WelcomeCommand;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitEvent;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.BaitType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.Direction;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.PlayerEvent;


/**
 * The {@code MessageParser} class is responsible for converting
 * raw protocol messages received from the MazeGame server into
 * executable {@link Command} objects.
 * <p>
 * It supports all message types defined by the MazeGame protocol
 * specification. Each message string is parsed into a concrete
 * command that can be executed to update the {@link GameStatusModel}.
 * </p>
 * 
 * <p>
 * <b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.
 * </p>
 * 
 */
public class MessageParser {

    /**
     * Parses a single-line protocol message into a {@link Command}.
     *
     * @param line the raw message string
     * @return a {@link Command} instance, or {@code null} if the
     *         message is invalid or not recognized
     */
    public Command parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split(";");
        String tmp = parts[0];

        try {
            return switch (tmp) {
                case "BPOS" -> new BaitPosCommand(
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        BaitType.valueOf(parts[3].toUpperCase()),
                        BaitEvent.valueOf(parts[4].toUpperCase())
                );
                case "JOIN" -> new JoinCommand(Integer.parseInt(parts[1]), parts[2]);
                case "LEAV" -> new LeaveCommand(Integer.parseInt(parts[1]));
                case "INFO" -> new InfoCommand(Integer.parseInt(parts[1]));
                case "PPOS" -> new PlayerPosCommand(
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]),
                        Direction.valueOf(parts[4].toUpperCase()),
                        PlayerEvent.valueOf(parts[5].toUpperCase())
                );
                case "PSCO" -> new PlayerScoreCommand(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                case "RDY." -> new ReadyCommand();
                case "QUIT" -> new QuitCommand();
                case "MSRV" -> new ServerVersionCommand(Integer.parseInt(parts[1]));
                case "TERM" -> new TerminateCommand();
                case "WELC" -> new WelcomeCommand(Integer.parseInt(parts[1]));
                case "MAZE" -> throw new IllegalStateException("MAZE header must be handled in MazeGameProtocol");
                default -> {
                    System.err.println("[MessageParser] Unknown message: " + line);
                    yield null;
                }
            };
        } catch (Exception e) {
            System.err.println("[MessageParser] Failed to parse message: " + line + " (" + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * Parses a multi-line {@code MAZE} message into a {@link MazeCommand}.
     *
     * @param width the width of the maze
     * @param height the height of the maze
     * @param rows the maze rows as strings
     * @return a {@link MazeCommand} containing the parsed cell data
     */
    public Command parseMaze(int width, int height, String[] rows) {
        CellType[][] cells = new CellType[height][width];
        for (int y = 0; y < height; y++) {
            String row = rows[y];
            for (int x = 0; x < width; x++) {
                cells[y][x] = this.charToCell(row.charAt(x));
            }
        }
        return new MazeCommand(width, height, cells);
    }

    /**
     * Converts a maze character into a {@link CellType}.
     *
     * @param c the character representing the cell
     * @return the corresponding {@link CellType}
     */
    private CellType charToCell(char c) {
        return switch (c) {
            case '#' -> CellType.WALL;
            case '~' -> CellType.WATER;
            case '.' -> CellType.PATH;
            default -> CellType.UNKNOWN;
        };
    }
}