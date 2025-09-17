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

public class MessageParser {

    public Command parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split(";");
        String tmp = parts[0];

        try {
            return switch (tmp) {
            case "BPOS" -> new BaitPosCommand(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    BaitType.valueOf(parts[3].toUpperCase()), BaitEvent.valueOf(parts[4].toUpperCase()));
            case "JOIN" -> new JoinCommand(Integer.parseInt(parts[1]), parts[2]);
            case "LEAV" -> new LeaveCommand(Integer.parseInt(parts[1]));
            case "INFO" -> new InfoCommand(Integer.parseInt(parts[1]));
            case "PPOS" ->
                new PlayerPosCommand(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                        Direction.valueOf(parts[4].toUpperCase()), PlayerEvent.valueOf(parts[5].toUpperCase()));
            case "PSCO" -> new PlayerScoreCommand(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            case "RDY." -> new ReadyCommand();
            case "QUIT" -> new QuitCommand();
            case "MSRV" -> new ServerVersionCommand(Integer.parseInt(parts[1]));
            case "TERM" -> new TerminateCommand();
            case "WELC" -> new WelcomeCommand(Integer.parseInt(parts[1]));
            case "MAZE" -> throw new IllegalStateException("MAZE header must be handled in MazeGameProtocol");
            default -> {
                System.out.println("Unknown message: " + line);
                yield null;
            }
            };
        } catch (Exception e) {
            System.err.println("Error while parsing message: " + line);
            e.printStackTrace();
            return null;
        }
    }

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

    private CellType charToCell(char c) {
        return switch (c) {
        case '#' -> CellType.WALL;
        case '~' -> CellType.WATER;
        case '.' -> CellType.PATH;
        default -> CellType.UNKNOWN;
        };
    }
}

