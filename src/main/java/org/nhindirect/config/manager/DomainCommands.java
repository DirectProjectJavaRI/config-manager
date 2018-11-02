package org.nhindirect.config.manager;

import java.util.Collection;

import org.nhind.config.rest.DomainService;
import org.nhindirect.common.tooling.Command;
import org.nhindirect.common.tooling.StringArrayUtil;
import org.nhindirect.config.manager.printers.DomainPrinter;
import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;

public class DomainCommands
{
    private static final String LIST_DOMAINS_USAGE = "Lists domains in the system";
	
    private static final String ADD_DOMAIN_USAGE = "Adds a domain to the system." +
    		"\r\n  domainName postmasterEmail " +
            "\r\n\t domainName: The name of the new domain." +
            "\r\n\t postmasterEmail: The email address of the postmaster of the domain.";
    
    private static final String DELETE_DOMAIN_USAGE = "Deletes a domain from the system." +
    		"\r\n  name " +
            "\r\n\t name: The name of the domain.";
    
	protected DomainService domainService;
	
	protected final DomainPrinter domainPrinter;
	
	DomainCommands(DomainService domainService)
	{
		this.domainService = domainService;	
		
		this.domainPrinter = new DomainPrinter();
	}  
	
	@Command(name = "ListDomains", usage = LIST_DOMAINS_USAGE)
    public void listDomains(String[] args)
    {
		// get them all
		try
		{
			final Collection<Domain> domains = domainService.searchDomains("", null);
			if (domains == null || domains.size() == 0)
			{
				System.out.println("No domains have been created.");
				return;
			}
			
			domainPrinter.printRecords(domains);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("Failed to retrieve domains: " + e.getMessage());
		}

    }
	
	@Command(name = "AddDomain", usage = ADD_DOMAIN_USAGE)
    public void addDomain(String[] args)
    {
		final String domainName = StringArrayUtil.getRequiredValue(args, 0);
		final String postmasterEmail = StringArrayUtil.getRequiredValue(args, 1);
		
		try
		{
			// make sure this domain name doesn't already exist
			final Domain exDomain = domainService.getDomain(domainName);
			if (exDomain != null)
			{
				System.out.println("The domain " + domainName + " already exists in the system");
				return;
			}
			
			final Address postmasterAddress = new Address();
			postmasterAddress.setDomainName(domainName);
			postmasterAddress.setEmailAddress(postmasterEmail);
			
			final Domain newDomain = new Domain();

			newDomain.setDomainName(domainName);
			newDomain.setPostmasterAddress(postmasterAddress);
			newDomain.setStatus(EntityStatus.ENABLED);
			
			domainService.addDomain(newDomain);
			
			System.out.println("Domain " + domainName + " successfully added.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("Failed to add new domain: " + e.getMessage());
		}
    }
	
	@Command(name = "DeleteDomain", usage = DELETE_DOMAIN_USAGE)
    public void deleteDomain(String[] args)
    {
		final String domainName = StringArrayUtil.getRequiredValue(args, 0);
		
		try
		{
			// make sure this domain actually exists
			final Domain exDomain = domainService.getDomain(domainName);
			
			if (exDomain == null)
			{
				System.out.println("The domain " + domainName + " does not exists in the system");
				return;
			}
			
			domainService.deleteDomain(domainName);
			
			System.out.println("Domain " + domainName + " successfully removed.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("Failed to delete domain: " + e.getMessage());
		}
    }
}
