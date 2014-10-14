package debugger.shapes.model;

public class ModelAndText {
	public ModelElement model;
	public String text;
	public ModelAndText(ModelElement model, String text){
		this.model=model;
		this.text=text;
	}
	String shortcut;
	public String toString() {
		if (shortcut==null){
			int i=text.indexOf("\n");
			if (i!=-1)
				shortcut=text.substring(0,i);
			else
				shortcut=text;
		}
		return shortcut;
	}
}
