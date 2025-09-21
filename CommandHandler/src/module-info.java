module CommandHandler {
    requires transitive GameStatusModel;
	requires javafx.controls;
    requires org.junit.jupiter.api;
	
    exports de.uni_koblenz.ptsd.foxtrot.commandhandler;
    exports de.uni_koblenz.ptsd.foxtrot.commandhandler.commands;
}