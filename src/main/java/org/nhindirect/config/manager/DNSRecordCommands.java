/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Greg Meyer      gm2552@cerner.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.nhindirect.config.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.nhind.config.rest.DNSService;
import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.common.tooling.Command;
import org.nhindirect.common.tooling.StringArrayUtil;
import org.nhindirect.config.manager.printers.DNSRecordPrinter;
import org.nhindirect.config.manager.printers.DefaultDNSRecordPrinter;
import org.nhindirect.config.model.DNSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

/**
 * Command definition and logic for managing DNS records.  Commands are case-insensitive.
 * @author Greg Meyer
 *
 * @since 1.0
 */
public class DNSRecordCommands 
{
    private static final String IMPORT_MX_USAGE = "Import a new MX dns record from a binary file." +
    	"\r\n\tfilepath " +
        "\r\n\t filePath: path to the MX record binary file. Can have any (or no extension)";

    private static final String IMPORT_SOA_USAGE = "Import a new SOA dns record from a binary file." +
        "\r\n\tfilepath " +
        "\r\n\t filePath: path to the SOA record binary file. Can have any (or no extension)";

    private static final String IMPORT_ADDRESS_USAGE = "Import a new A dns record from a binary file." +
        "\r\n\tfilepath " +
        "\r\n\t filePath: path to the A record binary file. Can have any (or no extension)";

    private static final String ADD_MX_USAGE = "Add a new MX dns record." +
    	"\r\n" + DNSRecordParser.PARSE_MX_USAGE;

    private static final String ENSURE_MX_USAGE = "Adds a new MX dns record if an identical one does't already exist. " +
        "\r\n" + DNSRecordParser.PARSE_MX_USAGE;

    private static final String ADD_NS_USAGE = "Add a new NS dns record." +
        	"\r\n" + DNSRecordParser.PARSE_NS_USAGE;

    private static final String ENSURE_NS_USAGE = "Adds a new NS dns record if an identical one does't already exist. " +
            "\r\n" + DNSRecordParser.PARSE_NS_USAGE;
    
    private static final String ADD_TXT_USAGE = "Add a new TXT dns record." +
        	"\r\n" + DNSRecordParser.PARSE_TXT_USAGE;

    private static final String ENSURE_TXT_USAGE = "Adds a new TXT dns record if an identical one does't already exist. " +
            "\r\n" + DNSRecordParser.PARSE_TXT_USAGE;
    
    private static final String ADD_CNAME_USAGE = "Add a new CNAME dns record." +
        	"\r\n" + DNSRecordParser.PARSE_CNAME_USAGE;

    private static final String ENSURE_CNAME_USAGE = "Adds a new CNAME dns record if an identical one does't already exist. " +
            "\r\n" + DNSRecordParser.PARSE_CNAME_USAGE;
    
    private static final String ADD_SOA_USAGE = "Add a new SOA dns record." +
        "\r\n" + DNSRecordParser.PARSE_SOA_USAGE;

    private static final String ENSURE_SOA_USAGE = "Add a new SOA dns record if an identical one does not exist." +
        "\r\n" + DNSRecordParser.PARSE_SOA_USAGE;
      
    private static final String ADD_ANAME_USAGE  = "Add a new ANAME dns record." +
        "\r\n" + DNSRecordParser.PARSE_ANAME_USAGE;

    private static final String ENSURE_ANAME_USAGE = "Add a new ANAME dns record if an identical one does not exist." +
        "\r\n" + DNSRecordParser.PARSE_ANAME_USAGE;

    private static final String REMOVE_MX_USAGE = "Remove an existing MX record by ID." +
        "\r\n\trecordid" +
        "\r\n\t recordid: record id to be removed from the database";


    private static final String REMOVE_SOA_USAGE = "Remove an existing SOA record by ID." +
        "\r\n\trecordid" +
        "\r\nt\t recordid: record id to be removed from the database";


    private static final String REMOVE_ANAME_USAGE = "Remove an existing ANAME record by ID." +
        "\r\n\trecordid" +
        "\r\n\t recordid: record id to be removed from the database";

    private static final String GET_ALL_USAGE = "Gets all records in the DNS store.";
    
    private static final String GET_SOA_CONTACTS = "Gets a list of all the different SOA contacts.";
    
    private DNSRecordPrinter printer;
    private DNSRecordParser parser;
    private DNSService proxy;
    
    /**
     * Constructor that takes a reference to the configuration service proxy.
     * @param proxy Configuration service proxy for accessing the configuration service.
     * 
     * @since 1.0
     */
	public DNSRecordCommands(DNSService proxy)
	{
	    parser = new DNSRecordParser();
	    printer = new DefaultDNSRecordPrinter();
	    this.proxy = proxy;
	}
	
	/*
	 * Convert a dnsjava record to a DnsRecord for use with the proxy.
	 */
	private DNSRecord fromRecord(Record rec)
	{
	    DNSRecord retVal = new DNSRecord();
	    retVal.setData(rec.rdataToWireCanonical());
	    retVal.setDclass(rec.getDClass());
	    retVal.setName(rec.getName().toString());
	    retVal.setTtl(rec.getTTL());
	    retVal.setType(rec.getType());
	    
	    return retVal;
	}
	
	/*
	 * Loads a record from a file.  Records are stored in raw wire format.
	 */
	private DNSRecord loadAndVerifyDnsRecordFromBin(String path)
	{
	    File recFile = new File(path);
	    if (!recFile.exists())
	    	throw new IllegalArgumentException("Record file " + recFile.getAbsolutePath() + " not found");
	    
	    Record rec = null;
	    try
	    {
		    byte[] wire = FileUtils.readFileToByteArray(recFile);
	
		    rec = Record.fromWire(wire, Section.ANSWER);
	    }
	    catch (Exception e)
	    {
	    	throw new RuntimeException("Error reading file " + recFile.getAbsolutePath() + " : " + e.getMessage(), e);
	    }
	    
	    return (rec != null) ? fromRecord(rec) : null;
	}
	
	/*
	 * Adds a DNS record to the configuration service.
	 */
	private void addDNS(DNSRecord dnsRecord)
	{
		try
		{
			proxy.addDNSRecord(dnsRecord);
			System.out.println("Record added successfully.");
		}		
		catch (ServiceException e)
		{
			throw new RuntimeException("Error adding DNS record: " + e.getMessage(), e);
		}
	
	}

	/*
	 * Removed a DNS record from the service
	 */
	private void removeDNS(long recordId)
	{
		try
		{
			proxy.deleteDNSRecordsByIds(Arrays.asList(recordId));
			System.out.println("Record removed successfully.");
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error accessing configuration service: " + e.getMessage(), e);
		}
	}
	
	/*
	 * Imports a specific DNS record type from a file.
	 */
	private void importRecord(String path, int type)
	{
		DNSRecord dnsRecord = loadAndVerifyDnsRecordFromBin(path);

		if (dnsRecord.getType() != type)
		{
			throw new IllegalArgumentException("File " + path + " does not contain the requested record type");
		}
		
		addDNS(dnsRecord);
	}
	
	/**
	 * Imports an MX record from a file.  The file contains the record in raw DNS wire format.
	 * @param args The first entry in the array contains the file path (required).
	 * 
     * @since 1.0
	 */
	@Command(name = "Dns_MX_Import", usage = IMPORT_MX_USAGE)
	public void mXImport(String[] args)
	{
	    String path = StringArrayUtil.getRequiredValue(args, 0);
	    importRecord(path, Type.MX);
	}
		
	/**
	 * Imports an SOA record from a file.  The file contains the record in raw DNS wire format.
	 * @param args The first entry in the array contains the file path (required).
	 * 
     * @since 1.0
	 */
	@Command(name = "Dns_SOA_Import", usage = IMPORT_SOA_USAGE)
	public void sOAImport(String[] args)
	{
	    String path = StringArrayUtil.getRequiredValue(args, 0);
	    importRecord(path, Type.SOA);
	}
	
	/**
	 * Imports an A record from a file.  The file contains the record in raw DNS wire format.
	 * @param args The first entry in the array contains the file path (required).
	 * 
     * @since 1.0
	 */
	@Command(name = "Dns_ANAME_Import", usage = IMPORT_ADDRESS_USAGE)
	public void importAddress(String[] args)
	{
	    String path = StringArrayUtil.getRequiredValue(args, 0);
	    importRecord(path, Type.A);
	}       
	
	/**
	 * Adds an MX records to the configuration service.
	 * @param args Contains the MX record attributes.
	 * 
	 * @since 1.0
	 */
	@Command(name = "Dns_MX_Add", usage = ADD_MX_USAGE)
	public void addMX(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseMX(args));
		
		addDNS(record);
	}
	
	/**
	 * Adds an MX records to the configuration service only if the record does not exist.
	 * @param args Contains the MX record attributes.
	 * 
	 * @since 1.0
	 */	
	@Command(name = "Dns_MX_Ensure", usage = ENSURE_MX_USAGE)
	public void ensureMX(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseMX(args));
	    if (!verifyIsUnique(record, false))
	    {
	        return;
	    }
	    
		
		addDNS(record);
	}
	
	/**
	 * Adds a TXT records to the configuration service.
	 * @param args Contains the TXT record attributes.
	 * 
	 * @since 1.3
	 */
	@Command(name = "Dns_TXT_Add", usage = ADD_TXT_USAGE)
	public void addTXT(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseTXT(args));
		
		addDNS(record);
	}	
	
	/**
	 * Adds a TXT records to the configuration service only if the record does not exist.
	 * @param args Contains the TXT record attributes.
	 * 
	 * @since 1.3
	 */	
	@Command(name = "Dns_TXT_Ensure", usage = ENSURE_TXT_USAGE)
	public void ensureTXT(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseTXT(args));
	    if (!verifyIsUnique(record, false))
	    {
	        return;
	    }
	    
		
		addDNS(record);
	}
	
	/**
	 * Adds a CNAME records to the configuration service.
	 * @param args Contains the CNAME record attributes.
	 * 
	 * @since 1.3
	 */
	@Command(name = "Dns_CNAME_Add", usage = ADD_CNAME_USAGE)
	public void addCNAME(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseCNAME(args));
		
		addDNS(record);
	}	
	
	/**
	 * Adds a CNAME records to the configuration service only if the record does not exist.
	 * @param args Contains the CNAME record attributes.
	 * 
	 * @since 1.3
	 */	
	@Command(name = "Dns_CNAME_Ensure", usage = ENSURE_CNAME_USAGE)
	public void ensureCNAME(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseCNAME(args));
	    if (!verifyIsUnique(record, false))
	    {
	        return;
	    }
	    
		
		addDNS(record);
	}
	
	/**
	 * Adds an NS records to the configuration service.
	 * @param args Contains the NS record attributes.
	 * 
	 * @since 1.3
	 */
	@Command(name = "Dns_NS_Add", usage = ADD_NS_USAGE)
	public void addNS(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseNS(args));
		
		addDNS(record);
	}	
	
	/**
	 * Adds an NS records to the configuration service only if the record does not exist.
	 * @param args Contains the NS record attributes.
	 * 
	 * @since 1.3
	 */	
	@Command(name = "Dns_NS_Ensure", usage = ENSURE_NS_USAGE)
	public void ensureNS(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseNS(args));
	    if (!verifyIsUnique(record, false))
	    {
	        return;
	    }
	    
		
		addDNS(record);
	}	
	
	
	/**
	 * Adds an SOA records to the configuration service.
	 * @param args Contains the SOA record attributes.
	 * 
	 * @since 1.0
	 */		
	@Command(name = "Dns_SOA_Add", usage = ADD_SOA_USAGE)
	public void addSOA(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseSOA(args));
		
		addDNS(record);
	}
	
	/**
	 * Adds an SOA records to the configuration service only if the record does not exist.
	 * @param args Contains the SOA record attributes.
	 * 
	 * @since 1.0
	 */	
	@Command(name = "Dns_SOA_Ensure", usage = ENSURE_SOA_USAGE)
	public void ensureSOA(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseSOA(args));
	    if (!verifyIsUnique(record, false))
	    {
	        return;
	    }
	    
		addDNS(record);
	}
	
	/**
	 * Adds an A records to the configuration service.
	 * @param args Contains the A record attributes.
	 * 
	 * @since 1.0
	 */
	@Command(name = "Dns_ANAME_Add", usage = ADD_ANAME_USAGE)
	public void addANAME(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseANAME(args));
		addDNS(record);
	}
	
	
	/**
	 * Adds an A records to the configuration service only if the record does not exist.
	 * @param args Contains the A record attributes.
	 * 
	 * @since 1.0
	 */		
	@Command(name = "Dns_ANAME_Ensure", usage = ENSURE_ANAME_USAGE)
	public void ensureANAME(String[] args)
	{
	    DNSRecord record = fromRecord(parser.parseANAME(args));
	    if (!verifyIsUnique(record, false))
	    {
	        return;
	    }
	    
		addDNS(record);
	}
	
	/**
	 * Removes an MX record from the configuration service by record id.
	 * @param args The first entry in the array contains the record id (required).
	 * 
	 * @since 1.0
	 */
	@Command(name = "Dns_MX_Remove", usage = REMOVE_MX_USAGE)
	public void removeMX(String[] args)
	{
	    long recordID = Long.parseLong(StringArrayUtil.getRequiredValue(args, 0));
	    removeDNS(recordID);
	}
	
	/**
	 * Removes an SOA record from the configuration service by record id.
	 * @param args The first entry in the array contains the record id (required).
	 * 
	 * @since 1.0
	 */
	@Command(name = "Dns_SOA_Remove", usage = REMOVE_SOA_USAGE)
	public void removeSOA(String[] args)
	{
	    long recordID = Long.parseLong(StringArrayUtil.getRequiredValue(args, 0));
	    removeDNS(recordID);
	}
	
	/**
	 * Removes an A record from the configuration service by record id.
	 * @param args The first entry in the array contains the record id (required).
	 * 
	 * @since 1.0
	 */	
	@Command(name = "Dns_ANAME_Remove", usage = REMOVE_ANAME_USAGE)
	public void removeANAME(String[] args)
	{
	    long recordID = Long.parseLong(StringArrayUtil.getRequiredValue(args, 0));
	    removeDNS(recordID);
	}
	

	/**
	 * Retrieves and prints all records in the configuration store. 
	 * @param args Empty
	 * 
	 * @since 1.0
	 */		
	@Command(name= "Dns_Get_All", usage = GET_ALL_USAGE)
	public void getAll(String[] args)
	{
	    Collection<DNSRecord> records = null;
	    try
	    {
	    	records = proxy.getDNSRecord(Type.ANY, "");
	    }
		catch (Exception e)
		{
			throw new RuntimeException("Error accessing configuration service: " + e.getMessage(), e);
		}
		
	    if (records == null || records.isEmpty())
	    {
	    	System.out.println("No records found");
	    }
	    else
	    	print(records);
	}
	
	@Command(name= "Dns_Get_SOA_Contacts", usage = GET_SOA_CONTACTS)
	public void getSoaContacts(String[] args)
	{
	    Collection<DNSRecord>records = null;
	    try
	    {
	    	records = proxy.getDNSRecord(Type.SOA, "");
	    }
		catch (Exception e)
		{
			throw new RuntimeException("Error accessing configuration service: " + e.getMessage(), e);
		}
		
	    if (records == null || records.isEmpty())
	    {
	    	System.out.println("No records found");
	    }
	    else
	    {
	    	final Map<String, String> contacts = new HashMap<String, String>();
	    	
	    	for (DNSRecord rec : records)
	    	{
	    		final SOARecord soaRec = (SOARecord)Record.newRecord(nameFromString(rec.getName()), rec.getType(), rec.getDclass(), rec.getTtl(), rec.getData());
	    		if (contacts.get(soaRec.getAdmin().toString()) == null)
	    			contacts.put(soaRec.getAdmin().toString(), soaRec.getAdmin().toString());
	    	}
	    	
	    	for (Entry<String, String> ent : contacts.entrySet())
	    		System.out.println("Contact: " + ent.getKey());
	    }
	}
	
	private Name nameFromString(String str)
	{
		if (!str.endsWith("."))
			str += ".";
	
		try
		{
			return Name.fromString(str);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Invalid DNS name");
		}
	}
	
	/*
	 * Gets and prints a record by record is
	 */
	/*
	private void get(long recordID)
	{
	    DNSRecord record = getRecord(recordID);
	    if (record != null)
	    	printer.print(record);
	}
	*/
	/**
	 * Looks up all records for a given domain and any sub domains.
	 * @param args The first entry in the array contains the domain name (required).
	 * 
	 * @since 1.0
	 */			
	@Command(name = "Dns_Match", usage = "Resolve all records for the given domain")
	public void match(String[] args)
	{
	    String domain = StringArrayUtil.getRequiredValue(args, 0);
	    Collection<DNSRecord> records = null;
	    Pattern pattern = Pattern.compile(domain);
	    ArrayList<DNSRecord> matchedRecords = new ArrayList<DNSRecord>(); 
	    try
	    {
	    	records = proxy.getDNSRecord(Type.ANY, "");
	    }
		catch (Exception e)
		{
			throw new RuntimeException("Error accessing configuration service: " + e.getMessage(), e);
		}
		
	    if (records == null || records.isEmpty())
	    {
	        System.out.println("No records found");
	        return;
	    }
	    else
	    {
	    	for (DNSRecord record : records)
	    	{
	    		Matcher matcher = pattern.matcher(record.getName());
	    		if (matcher.find())
	    		{
	    			matchedRecords.add(record);
	    		}
	    	}
	    }
	    
	    if (matchedRecords.size() == 0)
	    {
	        System.out.println("No records found");
	        return;
	    }	    
	    
	    print(matchedRecords);
	}
	
	/**
	 * Looks up SOA records for a given domain.
	 * @param args The first entry in the array contains the domain name (required).
	 * 
	 * @since 1.0
	 */	
	@Command(name = "Dns_SOA_Match", usage = "Resolve SOA records for the given domain")
	public void matchSOA(String[] args)
	{
	    match(StringArrayUtil.getRequiredValue(args, 0), Type.SOA);
	}
	
	/**
	 * Looks up A records for a given host name.
	 * @param args The first entry in the array contains the domain name (required).
	 * 
	 * @since 1.0
	 */		
	@Command(name = "Dns_ANAME_Match", usage = "Resolve Address records for the given domain")
	public void matchAName(String[] args)
	{
	    match(StringArrayUtil.getRequiredValue(args, 0), Type.A);
	}
	
	/**
	 * Looks up MX records for a given domain.
	 * @param args The first entry in the array contains the domain name (required).
	 * 
	 * @since 1.0
	 */		
	@Command(name = "Dns_MX_Match", usage = "Resolve MX records for the given domain")
	public void matchMX(String[] args)
	{
	    match(StringArrayUtil.getRequiredValue(args, 0), Type.MX);
	}
	
	/*
	 * gets records for a domain name and sub domains for a specific type of record
	 */
	private void match(String domain, int type)
	{
	    Collection<DNSRecord> records = getRecords(domain, type);
	    if (records != null && !records.isEmpty())
	    	print(records);
	}
	
	/*
	 * gets records by name and type
	 */
	private Collection<DNSRecord> getRecords(String domain, int type)
	{
		if (!domain.endsWith("."))
			domain += ".";
		
	    Collection<DNSRecord> records = null;
	    try
	    {
	    	records = proxy.getDNSRecord(type, domain);
	    }
		catch (Exception e)
		{
			throw new RuntimeException("Error accessing configuration service: " + e.getMessage(), e);
		}
		
	    if (records == null || records.isEmpty())
	    {
	    	System.out.println("No records found");
	    }
	    return records;
	}
	
	/*
	 * ensures that a record is unique in the configuration service
	 */
	private boolean verifyIsUnique(DNSRecord record, boolean details)
	{
	    DNSRecord existing = find(record);
	    if (existing != null)
	    {
	        System.out.println("Record already exists");

            print(existing);

	        return false;
	    }
	    
	    return true;
	}
	        
	/*
	 * finds a specific record by name and type
	 */
	private DNSRecord find(DNSRecord record)
	{
	    Collection<DNSRecord> existingRecords = null;
	    try
	    {
	    	existingRecords = proxy.getDNSRecord(record.getType(), record.getName());
	    }
		catch (Exception e)
		{
			throw new RuntimeException("Error accessing configuration service: " + e.getMessage(), e);
		}
		
	    if (existingRecords == null || existingRecords.isEmpty())
	    {
	        return null;
	    }
	    	    
	    for (DNSRecord existingRecord : existingRecords)
	    	if (org.bouncycastle.util.Arrays.areEqual(record.getData(), existingRecord.getData()))
	    		return existingRecord;
	    
	    return null;
	}
	
	/*
	 * prints the contents of an array of records
	 */
	private void print(Collection<DNSRecord> records)
	{
		if (records != null)
		{
		    for(DNSRecord record : records)
		    {
		        print(record);
		        System.out.println("\r\n-------------------------------------------");
		    }
		}
	}
	
	/*
	 * prints the contents of a specific record
	 */
	private void print(DNSRecord dnsRecord)
	{
	    System.out.println("RecordID: " + dnsRecord.getId());                        

	
	    printer.print(dnsRecord);

	}
    
	/**
	 * Sets the printer that will be used to print record query responses.
	 * @param printer The printer that will be used to print record query responses.
	 */
	public void setRecordPrinter(DNSRecordPrinter printer)
	{
		this.printer = printer; 
	}
	
	/**
	 * Sets the printer that will be used to print record query responses.
	 * @param printer The printer that will be used to print record query responses.
	 */
	public void setConfigurationProxy(DNSService proxy)
	{
		this.proxy = proxy; 
	}	
}
