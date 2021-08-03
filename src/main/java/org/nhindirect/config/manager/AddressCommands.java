package org.nhindirect.config.manager;

import java.util.Collection;

import org.nhind.config.rest.AddressService;
import org.nhindirect.common.tooling.Command;
import org.nhindirect.common.tooling.StringArrayUtil;
import org.nhindirect.config.manager.printers.AddressPrinter;
import org.nhindirect.config.manager.printers.RecordPrinter;
import org.nhindirect.config.model.Address;

public class AddressCommands     
{
    private static final String LIST_ADDRESSES_BY_DOMAIN = "Lists all addresses in domain";
    
	protected AddressService addressService;
    
	protected RecordPrinter<Address> addressPrinter;
    
	public AddressCommands(AddressService addressService)
	{
		this.addressService = addressService;
		
		this.addressPrinter = new AddressPrinter();
	}  
	
	@Command(name = "ListAddressesByDomain", usage = LIST_ADDRESSES_BY_DOMAIN)
    public void listAddresesByDomain(String[] args)
	{
		
		String domain = StringArrayUtil.getRequiredValue(args, 0);
		try
		{
			final Collection<Address> addrs = addressService.getAddressesByDomain(domain);
			
			if (addrs == null || addrs.size() == 0)
			{
				System.out.println("No addresses found");
				return;
			}
			System.out.println("Addresses in domain " + domain);
				
			addressPrinter.printRecords(addrs);
				
			System.out.println("\r\n");
		
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup addresses: " + e.getMessage());
		}

	}		
}
