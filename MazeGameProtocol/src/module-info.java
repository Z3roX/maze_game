module MazeGameProtocol {
	requires transitive CommandHandler;
    requires GameStatusModel;
    requires javafx.controls;
    
    exports de.uni_koblenz.ptsd.foxtrot.protocol;
}