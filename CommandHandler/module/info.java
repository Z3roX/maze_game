module CommandHandler {
    requires transitive GameStatusModel;
    requires javafx.base;
    requires javafx.graphics; // for javafx.application.Platform

    exports de.uni_koblenz.ptsd.foxtrot.commandhandler;
    exports de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;
}