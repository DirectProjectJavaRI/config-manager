package org.nhindirect.config.manager.printers;

import java.util.Collection;

import org.nhindirect.config.model.DNSRecord;

/**
 * Interface for printing DNS records to an output Stream.
 * @author Greg Meyer
 *
 * @since 1.0
 */
public interface DNSRecordPrinter 
{
    /**
     * Prints the contents of a collection of DNS records.
     * @param records A collection of DNS records to print.
     * 
     * @since 1.0
     */
    public void print(Collection<DNSRecord> records);
    
    /**
     * Prints the contents of a single DNS records.
     * @param record DNS records to print.
     * 
     * @since 1.0
     */    
    public void print(DNSRecord record);
}
