module CommandHandler {
    requires transitive GameStatusModel;
    requires transitive javafx.base;
    requires transitive javafx.graphics; // for javafx.application.Platform

    exports de.uni_koblenz.ptsd.foxtrot.commandhandler;
    exports de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;
}
