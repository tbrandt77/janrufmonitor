package de.janrufmonitor.framework.installer.rules;

import java.util.Properties;

import de.janrufmonitor.framework.installer.InstallerConst;

public class ModuleArchitectureRule extends AbstractRule {

	public void validate(Properties descriptor) throws InstallerRuleException {
		super.validate(descriptor);
		
    	String reqiredModuleArchitecture = descriptor.getProperty(InstallerConst.DESCRIPTOR_REQUIRED_ARCHITECTURE);
    	
    	// ignore architecture if not set in descriptor
    	if (reqiredModuleArchitecture==null) return;
    	
    	String javaVmArch = System.getProperty("sun.arch.data.model", "32");
    	
    	if (reqiredModuleArchitecture.equalsIgnoreCase(javaVmArch)){
    		return;
    	} else {
    	   	throw new InstallerRuleException(toString().toLowerCase(), "The new module cannot be installed, because it requires program architecture "+reqiredModuleArchitecture+"-bit, but the current Java VM is running on "+javaVmArch+"-bit.");
    	}
	}

	public String toString() {
		return "ModuleArchitectureRule";
	}
}
