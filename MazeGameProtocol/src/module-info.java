module MazeGameProtocol {
	requires transitive CommandHandler;
    requires transitive GameStatusModel;
    requires javafx.controls;
    
    exports de.uni_koblenz.ptsd.foxtrot.protocol;
}