package org.nhindirect.config.manager;

import java.util.Arrays;

import org.nhind.config.rest.AddressService;
import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DNSService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhind.config.rest.TrustBundleService;
import org.nhindirect.common.tooling.Commands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients({"org.nhind.config.rest.feign"})
public class ConfigManager implements CommandLineRunner
{
	@Autowired
	protected DomainService domainService;
	
	@Autowired
	protected SettingService settingService;
	
	@Autowired
	protected DNSService dnsService;	
	
	@Autowired
	protected CertificateService certService;		
	
	@Autowired
	protected AnchorService anchorService;	
	
	@Autowired
	protected CertPolicyService certPolicyService;	
	
	@Autowired
	protected TrustBundleService bundleService;		
	
	@Autowired
	protected AddressService addressService;			
	
	private Commands commands;
	
	private static boolean exitOnEndCommands = true;
	
	/**
	 * Application entry point.
	 * @param args Command line arguments.
	 * 
	 * @since 1.0
	 */
    public static void main(String[] args) 
    {
        SpringApplication.run(ConfigManager.class, args);
    }	
    
    public void run(String... args)
	{
		String[] passArgs = null;
		
		// get the config URL if it exist
		
        for (int i = 0; i < args.length; i++)
        {		
			if ((args.length - i) > 2)
				passArgs = (String[])Arrays.copyOfRange(args, i, args.length);
			else
				passArgs = new String[0];
        }
		

		boolean runCommand = false;

		runCommand = runApp(passArgs);


		if (exitOnEndCommands)
			System.exit(runCommand ? 0 : -1);			
	}	
	
	/**
	 * Constructor with the location of the configuration service.
	 * @param configURL URL containing the locations of the configuration service.
	 * 
     * @since 1.0
	 */
	public ConfigManager()
	{
		
	}
	
	/**
	 * Either executes commands from the command line or runs the manager interactively.
	 * @param args Command arguments.  If the arguments are empty, then the manager runs interactively.
	 * @return True if the command was run successfully.  False otherwise.
	 * 
     * @since 1.0
	 */
	public boolean runApp(String[] args)
	{
		commands = new Commands("Configuration Management Console");

		
		commands.register(new SettingsCommands(settingService));
		
		commands.register(new DNSRecordCommands(dnsService));

		commands.register(new CertCommands(certService));
		
		commands.register(new DomainCommands(domainService));
		
		commands.register(new AnchorCommands(anchorService, domainService));
		
		commands.register(new PolicyCommands(certPolicyService, domainService));
	
		commands.register(new TrustBundleCommands(bundleService, domainService));
	
		commands.register(new AddressCommands(addressService));	
		
        if (args != null && args.length > 0)
        {
            return commands.run(args);
        }
        
        commands.runInteractive();
        System.out.println("Shutting Down Configuration Manager Console");
        return true;		
	}
	
	/**
	 * Determines if the application should exit when command processing is complete.  It may be desirable to set this 
	 * to false if calling from another application context.  The default is true.
	 * @param exit True if the application should terminate on completing processing commands.  False otherwise.
	 */
	public static void setExitOnEndCommands(boolean exit)
	{
		exitOnEndCommands = exit;
	}
}
