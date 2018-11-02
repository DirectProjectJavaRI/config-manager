package org.nhindirect.config.manager;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.DomainService;
import org.nhindirect.common.crypto.CryptoExtensions;
import org.nhindirect.common.tooling.Command;
import org.nhindirect.common.tooling.StringArrayUtil;
import org.nhindirect.config.manager.printers.AnchorRecordPrinter;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.utils.CertUtils;

public class AnchorCommands
{
	private static final String LIST_ANCHORS_USAGE = "Lists all anchors in the system";
	
    private static final String IMPORT_ANCHOR_USAGE = "Imports a trust anchor certificate file into the system and associates it to a domain." +
            "\r\n  anchor domainName incoming outgoing" +
            "\r\n\t anchor: Fully qualified path and file name of the X509 certificate anchor file.  " +
            "Place the file name in quotes (\"\") if there are spaces in the path or name." +
            "\r\n\t domainName: Name of the domain that the anchor will be associated with" +
            "\r\n\t incoming: Indicates if the anchor should be used to trust incoming messages.  Valid values are true or false" +
            "\r\n\t outgoing: Indicates if the anchor should be used to trust outgoing messages.  Valid values are true or false";
            
    private static final String DELETE_ANCHOR_USAGE = "Deletes an anchor from the system by id." +
            "\r\n  id" +
            "\r\n\t id: Id of the anchor to be deleted";
    
    private static final String EXPORT_ANCHOR_USAGE = "Exports an anchor to a DER encoded file." +
            "\r\n  owner thumbprint " +
            "\r\n\t owner: domain owner of the anchor" + 
            "\r\n\t thumbprint: thumbprint of the anchor";
	
	protected AnchorService anchorService;
	protected DomainService domainService;
	
	protected final AnchorRecordPrinter anchorPrinter;
	
	public AnchorCommands(AnchorService anchorService, DomainService domainService)
	{
		this.anchorService = anchorService;
		this.domainService = domainService;
		
		this.anchorPrinter = new AnchorRecordPrinter();
	}
	
	@Command(name = "ListAnchors", usage = LIST_ANCHORS_USAGE)
    public void listAncors(String[] args)
	{
		try
		{
			final Collection<Anchor> anchors = anchorService.getAnchors();
			
			if (anchors == null || anchors.size() == 0)
				System.out.println("No anchors found");
			else
			{
				anchorPrinter.printRecords(anchors);
			}
		}
		catch (Exception e)
		{
			System.err.println("Failed to get anchors: " + e.getMessage());
		}
	}
	
	@Command(name = "ImportAnchor", usage = IMPORT_ANCHOR_USAGE)
    public void importAnchor(String[] args)
	{
		final String fileLoc = StringArrayUtil.getRequiredValue(args, 0);
		final String domainName = StringArrayUtil.getRequiredValue(args, 1);
		final boolean incoming = Boolean.parseBoolean(StringArrayUtil.getRequiredValue(args, 2));
		final boolean outgoing = Boolean.parseBoolean(StringArrayUtil.getRequiredValue(args, 3));
		
		try
		{
			// makes sure the domain exists
			final Domain exDomain = domainService.getDomain(domainName);
			
			if (exDomain == null)
			{
				System.out.println("The domain " + domainName + " does not exists in the system");
				return;
			}
			
			
			byte[] certBytes = FileUtils.readFileToByteArray(new File(fileLoc));
			
			if (certBytes != null)
			{
				Anchor anchor = new Anchor();
				anchor.setCertificateData(certBytes);
				anchor.setIncoming(incoming);
				anchor.setOutgoing(outgoing);
				anchor.setOwner(exDomain.getDomainName());
				anchor.setStatus(EntityStatus.ENABLED);
				
				anchorService.addAnchor(anchor);

				System.out.println("Successfully imported trust anchor.");
			}
			
		}
		catch (IOException e)
		{
			System.out.println("Error reading file " + fileLoc + " : " + e.getMessage());
		}
		catch (Exception e)
		{
			System.out.println("Error importing trust anchor " + fileLoc + " : " + e.getMessage());
		}
	}

	
	@Command(name = "ExportAnchor", usage = EXPORT_ANCHOR_USAGE)
    public void exportAnchor(String[] args)
	{	
		final String owner = StringArrayUtil.getRequiredValue(args, 0);
		final String thumbprint = StringArrayUtil.getRequiredValue(args, 1);
		
		try
		{
			final Collection<Anchor> anchors = anchorService.getAnchorsForOwner(owner, true, true, thumbprint);
			// make sure the anchor exists
			
			if (anchors == null || anchors.size() == 0)
			{
				System.out.println("Anchor does not exists.");
				return;
			}
			else
			{
				for (Anchor anchor : anchors)
				{

					final X509Certificate cert = CertUtils.toX509Certificate(anchor.getCertificateData());
					final String certFileHold = CryptoExtensions.getSubjectAddress(cert) + ".der";
						
					File certFile = new File(certFileHold);
					if (certFile.exists())
						certFile.delete();
					
					System.out.println("Writing anchor file: " + certFile.getAbsolutePath());
					
					try 
					{
						FileUtils.writeByteArrayToFile(certFile, cert.getEncoded());
					} 
					catch (Exception e) 
					{
						System.err.println("Failed to write anchor to file: " + e.getMessage());
					}
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Error exporting anchor: " + e.getMessage());
		}
	}

	@Command(name = "DeleteAnchor", usage = DELETE_ANCHOR_USAGE)
    public void deleteUnmagedCert(String[] args)
	{
		final String id = StringArrayUtil.getRequiredValue(args, 0);

		try
		{

			anchorService.deleteAnchorsByIds(Arrays.asList(Long.parseLong(id)));
			System.out.println("Anchor with id " + id + " removed");
			
		}
		catch (Exception e)
		{
			System.out.println("Error deleting anchor: " + e.getMessage());
		}	
	}
}
