package de.janrufmonitor.ui.jface.application.tellows.rendering;

public class TellowsImg extends AbstractTellowsImg {

	private static String NAMESPACE = "ui.jface.application.tellows.rendering.TellowsImg";

	public String getID() {
		return "TellowsImg".toLowerCase();
	}
	
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public int getSize() {
		return 90;
	}

}
