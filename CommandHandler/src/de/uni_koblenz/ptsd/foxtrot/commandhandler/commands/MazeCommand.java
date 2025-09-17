package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;

/** Sets the maze in the GameStatusModel and marks state as NOTLOGGEDIN. */
public class MazeCommand implements Command {
    private final int width;
    private final int height;
    private final CellType[][] cells;

    public MazeCommand(int width, int height, CellType[][] cells) {
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    @Override
    public void execute() {
        Maze maze = new Maze(width, height, cells);
        GameStatusModel.getInstance().setMaze(maze);
        GameStatusModel.getInstance().setState(State.NOTLOGGEDIN);
    }
}

