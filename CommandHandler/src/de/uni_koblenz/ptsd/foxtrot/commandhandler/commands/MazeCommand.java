package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.State;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.enums.CellType;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.Maze;

/**
* Stores/updates the game maze layout in the {@link GameStatusModel}.
* <p>
* The command records layout data (e.g., grid, walls, spawn points) and may
* update dependent values (width, height, passable cells).
* </p>
*
*/

public class MazeCommand implements Command {
    private final int width;
    private final int height;
    private final CellType[][] cells;

    public MazeCommand(int width, int height, CellType[][] cells) {
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    /**
    * Applies the maze layout to the model.
    */

    @Override
    public void execute() {
        Maze maze = new Maze(width, height, cells);
        GameStatusModel.getInstance().setMaze(maze);
        GameStatusModel.getInstance().setState(State.NOTLOGGEDIN);
    }
}

