package plugins.davhelle.cellgraph;

import javax.swing.JList;
import javax.swing.JSeparator;

import plugins.adufour.ezplug.*;

public class CellExport extends EzPlug {


	@Override
	public void clean() { }


	@Override
	protected void execute() { }


	@Override
	protected void initialize()
	{
		//save one complete excel file
		
		getUI().setActionPanelVisible(true);
		String[] data = {"one", "two", "three", "four","Five","Six","Seven","eight"};
		JList myList = new JList(data);
		addComponent(myList);
		addEzComponent(new EzVarFloat("A number", 5.6f, 0, 10, 0.1f));
		addComponent(new JSeparator(JSeparator.VERTICAL));
		addEzComponent(new EzLabel("Oh look, a new column!"));
		addEzComponent(new EzGroup("Groups are still cool", new EzLabel("you get the idea..."), new EzVarBoolean("do you?", false)));
	}    
	

}
