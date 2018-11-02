package org.nhindirect.config.manager;

import java.util.Collection;

import org.nhind.config.rest.SettingService;
import org.nhindirect.common.tooling.Command;
import org.nhindirect.common.tooling.StringArrayUtil;
import org.nhindirect.config.manager.printers.RecordPrinter;
import org.nhindirect.config.manager.printers.SettingRecordPrinter;
import org.nhindirect.config.model.Setting;

public class SettingsCommands 
{
    private static final String LIST_SETTINGS_USAGE = "Lists all settings in the system";
    
    private static final String ADD_SETTING_USAGE = "Adds a name/value setting to the system." +
    		"\r\n  name value " +
            "\r\n\t name: The name of the new setting." +
            "\r\n\t value: The name of the new setting.  If the value has spaces, include the setting in quotes";
    
    private static final String DELETE_SETTING_USAGE = "Deletes a setting from the system" +
    		"\r\n  name " +
            "\r\n\t name: The name of the setting to delete.";
    
	protected SettingService proxy;
    
	protected RecordPrinter<Setting> settingsPrinter;
    
	public SettingsCommands(SettingService proxy)
	{
		this.proxy = proxy;
		
		this.settingsPrinter = new SettingRecordPrinter();
	}  
	
	@Command(name = "ListSettings", usage = LIST_SETTINGS_USAGE)
    public void listCerts(String[] args)
	{
		try
		{
			final Collection<Setting> settings = proxy.getSettings();
			if (settings == null || settings.size() == 0)
				System.out.println("No settings found");
			else
			{
				settingsPrinter.printRecords(settings);
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup certificates: " + e.getMessage());
		}

	}	
	
	@Command(name = "AddSetting", usage = ADD_SETTING_USAGE)
    public void addSetting(String[] args)
	{
		final String name = StringArrayUtil.getRequiredValue(args, 0);
		final String value = StringArrayUtil.getRequiredValue(args, 1);
		
		try
		{
			final Setting existingName = proxy.getSetting(name);
			if (existingName != null)
			{
				System.out.println("Setting " + name + " already exists");
				return;
			}
			
			proxy.addSetting(name, value);
			
			System.out.println("Setting " + name + " added successfully");
		}
		catch (Exception e)
		{
			System.out.println("Failed to add setting: " + e.getMessage());
		}
	}
	
	@Command(name = "DeleteSetting", usage = DELETE_SETTING_USAGE)
    public void deleteSetting(String[] args)
	{
		final String name = StringArrayUtil.getRequiredValue(args, 0);
		
		try
		{
			final Setting existingName = proxy.getSetting(name);
			if (existingName == null)
			{
				System.out.println("Setting " + name + " does not exists");
				return;
			}
			
			proxy.deleteSetting(name);
			
			System.out.println("Setting " + name + " deleted successfully");
		}
		catch (Exception e)
		{
			System.out.println("Failed to delete setting: " + e.getMessage());
		}
	}
}
