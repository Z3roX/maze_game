package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

/** Represents a unit of work to mutate the GameStatusModel. */
public interface Command {
    void execute();
}
