package de.janrufmonitor.ui.jface.wizards.pages;

import org.eclipse.swt.widgets.Composite;

import de.janrufmonitor.runtime.IRuntime;

public abstract class InitVariantPage extends AbstractPage {

	public static String NAMESPACE = "ui.jface.wizards.pages.InitVariantPage";

	public InitVariantPage() {
		super(InitVariantPage.class.getName());
	}

	public abstract boolean performFinish();
	
	
	public void createControl(Composite parent) {

	}
	
	public abstract IRuntime getRuntime();


	public String getNamespace() {
		return NAMESPACE;
	}
}
