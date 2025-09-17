package de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;

/** Holds an info code from the server; no-op for model. */
public class InfoCommand implements Command {
    private final int infoCode;

    public InfoCommand(int infoCode) {
        this.infoCode = infoCode;
    }

    public int getInfoCode() {
        return infoCode;
    }

    @Override
    public void execute() {
        // Intentionally left blank: GameStatusModel has no error/info storage yet.
    }
}

