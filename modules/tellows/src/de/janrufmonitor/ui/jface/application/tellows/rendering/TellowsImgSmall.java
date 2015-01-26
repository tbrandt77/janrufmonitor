package de.janrufmonitor.ui.jface.application.tellows.rendering;

public class TellowsImgSmall extends AbstractTellowsImg {

	private static String NAMESPACE = "ui.jface.application.tellows.rendering.TellowsImgSmall";

	public String getID() {
		return "TellowsImgSmall".toLowerCase();
	}
	
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public int getSize() {
		return 60;
	}
}
