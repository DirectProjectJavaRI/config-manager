package org.nhindirect.config.manager;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.nhind.config.rest.CertificateService;
import org.nhindirect.common.crypto.CryptoExtensions;
import org.nhindirect.common.tooling.Command;
import org.nhindirect.common.tooling.StringArrayUtil;
import org.nhindirect.config.manager.printers.CertRecordPrinter;
import org.nhindirect.config.manager.printers.RecordPrinter;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.stagent.cert.X509CertificateEx;


public class CertCommands 
{
    private static final String LIST_CERTIFICATES_USAGE = "Lists certificates in the system";

    private static final String LIST_EMAIL_CERTIFICATES_USAGE = "Lists certificates by a given email address or domain" +
            "\r\n address" +
    		"\r\n\t address: The email address or domain to search for.  Certificates are searched on the subject alternative name field of legacy email address of the certificate";

    private static final String EXPORT_EMAIL_CERTIFICATES_USAGE = "Exports certificates by a given email address or domain" +
            "\r\n address" +
    		"\r\n\t address: The email address or domain to search for.  Certificates are searched on the subject alternative name field of legacy email address of the certificate";
    
    private static final String IMPORT_PUBLIC_CERT_USAGE = "Imports a certificate that does not contain private key information" +
            "\r\n  certfile" +
            "\r\n\t certfile: Fully qualified path and file name of the X509 certificate file.  Place the file name in quotes (\"\") if there are spaces in the path or name.";
    
    private static final String IMPORT_PRIVATE_CERT_USAGE = "Imports a certificate with a private key and an optional passphrase. \r\n" +
            "Files should be in pkcs12 format." +
    		"\r\n  certfile [passphrase]" +
            "\r\n\t certfile: Fully qualified path and file name of the pkcs12 certificate file.  Place the file name in quotes (\"\") if there are spaces in the path or name." +
            "\r\n\t [passphrase]: Optional passphrase to decrypt the pkcs12 file.";   

    private static final String IMPORT_PRIVATE_CERT_W_WRAPPEDKEY_USAGE = "Imports a certificate with a wrapped private key. \r\n" +
            "The wrapped private key is assumed to have been wrapped by an external module and will on be unwrapped by the same module.  No password is needed" +
    		"\r\n  certfile privateKeyfile" +
            "\r\n\t certfile: Fully qualified path and file name of the certificate file in DER format.  Place the file name in quotes (\"\") if there are spaces in the path or name." +
            "\r\n\t privateKeyfile: Fully qualified path and file name of the wrapped private key file.  Place the file name in quotes (\"\") if there are spaces in the path or name.";
    
    private static final String ADD_IPKIX_CERT_USAGE = "Add an IPKIX record with a subject and URL. \r\n" +
    		"\r\n  subject URL" +
    		"\r\n  subject: email address or domain name" +
            "\r\n\t URL: Fully qualified URL to certificate";  
    
    private static final String REMOVED_CERTIFICATE_USAGE = "Removes a certifacte from the system by owner." +
            "\r\n  owner" +
            "\r\n\t owner: owner or URL of the certificate to be removed";    
    
    private static final String REMOVED_CERTIFICATE_TP_USAGE = "Removes a certifacte from the system by thumbprint." +
            "\r\n  thumbprint" +
            "\r\n\t thumbprint: The thumbprint of the certificate to be removed";    
    
    
	protected CertificateService certService;
    
	protected RecordPrinter<Certificate> certPrinter;
    
	public CertCommands(CertificateService certService)
	{
		this.certService = certService;
		
		this.certPrinter = new CertRecordPrinter();
	}    
	
	@Command(name = "ListCerts", usage = LIST_CERTIFICATES_USAGE)
    public void listCerts(String[] args)
	{
		try
		{
			final Collection<Certificate> certs = certService.getAllCertificates();
			if (certs == null || certs.size() == 0)
				System.out.println("No certificates found");
			else
			{
				certPrinter.printRecords(certs);
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup certificates: " + e.getMessage());
		}

	}	
	
	@Command(name = "ListCertsByAddress", usage = LIST_EMAIL_CERTIFICATES_USAGE)
    public void listCertsByAddress(String[] args)
	{
		String owner = StringArrayUtil.getRequiredValue(args, 0);
		
		try
		{		
			final Collection<Certificate> certs = certService.getCertificatesByOwner(owner);
			
			if (certs == null || certs.size() == 0)
				System.out.println("No certificates found");
			else
			{
				certPrinter.printRecords(certs);
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup certificates: " + e.getMessage());
		}
	}	
	
	
	@Command(name = "ExportCertByAddress", usage = EXPORT_EMAIL_CERTIFICATES_USAGE)
    public void exportCertByAddress(String[] args)
	{
		String owner = StringArrayUtil.getRequiredValue(args, 0);
		String keyStorePass = StringArrayUtil.getOptionalValue(args, 1, "");
		String privKeyPass = StringArrayUtil.getOptionalValue(args, 2, "");
		
		try
		{		
			final Collection<Certificate> certs = certService.getCertificatesByOwner(owner);
			
			if (certs == null || certs.size() == 0)
				System.out.println("No certificates found");
			else
			{
				writeCertsToFiles(certs, keyStorePass, privKeyPass);
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup certificates: " + e.getMessage());
		}
	}
	
	@Command(name = "AddPublicCert", usage = IMPORT_PUBLIC_CERT_USAGE)
    public void importPublicCert(String[] args)
	{
		final String fileLoc = StringArrayUtil.getRequiredValue(args, 0);
		try
		{
			final X509Certificate cert = new CertUtils().certFromFile(fileLoc);
			

			final Certificate addCert = new Certificate();
			addCert.setData(cert.getEncoded());
			addCert.setOwner(CryptoExtensions.getSubjectAddress(cert));
			addCert.setPrivateKey(false);
			addCert.setStatus(EntityStatus.ENABLED);

			certService.addCertificate(addCert);
			System.out.println("Successfully imported public certificate.");
			
		}
		///CLOVER:OFF
		catch (Exception e)
		{
			System.out.println("Error importing certificate " + fileLoc + " : " + e.getMessage());
		}
		///CLOVER:ON
		
	}	
	
	
	@Command(name = "AddPrivateCertWithWrappedKey", usage = IMPORT_PRIVATE_CERT_W_WRAPPEDKEY_USAGE)
    public void importPrivateCertWithWrappedKey(String[] args)
	{
		final String certFileLoc = StringArrayUtil.getRequiredValue(args, 0);
		final String keyFileLoc = StringArrayUtil.getRequiredValue(args, 1);

		try
		{
			final byte[] certFileBytes = FileUtils.readFileToByteArray(new File(certFileLoc));
			final byte[] keyFileBytes = FileUtils.readFileToByteArray(new File(keyFileLoc));
			
			final X509Certificate cert = CertUtils.toX509Certificate(certFileBytes);
			
			byte[] certBytes = CertUtils.certAndWrappedKeyToRawByteFormat(keyFileBytes, cert);
			
			Certificate addCert = new Certificate();
			addCert.setData(certBytes);
			addCert.setOwner(CryptoExtensions.getSubjectAddress(cert));
			addCert.setPrivateKey(cert instanceof X509CertificateEx);
			addCert.setStatus(EntityStatus.ENABLED);

			certService.addCertificate(addCert);
			System.out.println("Successfully imported certificate.");
			
		}
		catch (IOException e)
		{
			System.out.println("Error reading file: " + e.getMessage());
			return;
		}
		catch (Exception e)
		{
			System.out.println("Error importing certificate " + e.getMessage());
		}	
	}
	
	@Command(name = "AddPrivateCert", usage = IMPORT_PRIVATE_CERT_USAGE)
    public void importPrivateCert(String[] args)
	{
		final String fileLoc = StringArrayUtil.getRequiredValue(args, 0);
		final String passPhrase = StringArrayUtil.getOptionalValue(args, 1, "");
		try
		{
			
			final byte[] certBytes = FileUtils.readFileToByteArray(new File(fileLoc));
			
			final byte[] insertBytes = (passPhrase == null || passPhrase.isEmpty()) ?
					certBytes : CertUtils.pkcs12ToStrippedPkcs12(certBytes, passPhrase);
			
			final X509Certificate cert = CertUtils.toX509Certificate(insertBytes);
			
			Certificate addCert = new Certificate();
			addCert.setData(certBytes);
			addCert.setOwner(CryptoExtensions.getSubjectAddress(cert));
			addCert.setPrivateKey(cert instanceof X509CertificateEx);
			addCert.setStatus(EntityStatus.ENABLED);

			certService.addCertificate(addCert);
			System.out.println("Successfully imported private certificate.");
			
		}
		catch (IOException e)
		{
			System.out.println("Error reading file " + fileLoc + " : " + e.getMessage());
			return;
		}
		catch (Exception e)
		{
			System.out.println("Error importing certificate " + fileLoc + " : " + e.getMessage());
		}	
	}	
	
	@Command(name = "AddIPKIXCert", usage = ADD_IPKIX_CERT_USAGE)
    public void addIPKIXCert(String[] args)
	{
		final String owner = StringArrayUtil.getRequiredValue(args, 0);
		final String URL = StringArrayUtil.getRequiredValue(args, 1);
		
		try
		{

				Certificate addCert = new Certificate();
				addCert.setData(URL.getBytes());
				addCert.setOwner(owner);
				addCert.setPrivateKey(false);
				addCert.setStatus(EntityStatus.ENABLED);

				certService.addCertificate(addCert);
				System.out.println("Successfully added IPKIX certificate URL.");

			
		}
		catch (Exception e)
		{
			System.out.println("Error add IPKIX URL: " + e.getMessage());
		}	
	}	
	
	@Command(name = "RemoveCert", usage = REMOVED_CERTIFICATE_USAGE)
    public void removeCert(String[] args)
	{
		final String owner = StringArrayUtil.getRequiredValue(args, 0);

		try
		{
			certService.deleteCertificateByOwner(owner);
			System.out.println("Successfully removed certificate for owner." + owner);
		}
		catch (Exception e)
		{
			System.out.println("Error removing certificate for owner " + owner + " : " + e.getMessage());
		}	
	}
	
	
	@Command(name = "RemoveCertByTP", usage = REMOVED_CERTIFICATE_TP_USAGE)
    public void removeCertByTP(String[] args)
	{
		final String tp = StringArrayUtil.getRequiredValue(args, 0);

		try
		{
			final Certificate cert = certService.getCertificatesByOwnerAndThumbprint(" ", tp);
			
			if (cert == null)
			{
				System.out.println("Certificate with thumbprint " + tp + " cannot be found in the system");
				return;
			}
			
			certService.deleteCertificatesByIds(Arrays.asList(cert.getId()));
			System.out.println("Successfully removed certificate for thumbprint." + tp);
		}
		catch (Exception e)
		{
			System.out.println("Error removing certificate for thumbprint " + tp + " : " + e.getMessage());
		}	
	}

	public void setRecordPrinter(RecordPrinter<Certificate> printer)
	{
		this.certPrinter = printer; 
	}	
	
	
	protected void writeCertsToFiles(Collection<Certificate> certs, String keyStorePass, String privKeyPass) throws IOException
	{
		int idx = 1;
		for (Certificate cert : certs)
		{
			CertUtils.CertContainer cont = CertUtils.toCertContainer(cert.getData(), keyStorePass.toCharArray(), privKeyPass.toCharArray());
			X509Certificate transCert = cont.getCert();
			
			String certFileName= "";
			
			if (cont.getKey() != null)
				transCert = X509CertificateEx.fromX509Certificate(transCert, (PrivateKey)cont.getKey());
			
			String extension = (transCert instanceof X509CertificateEx ) ? ".p12" : ".der";
			String certFileHold = CryptoExtensions.getSubjectAddress(transCert) + extension;
			if (certs.size() > 1)
			{
				int index = certFileHold.lastIndexOf(".");
				if (index < 0)
					certFileHold += "(" + idx + ")";
				else
				{
					certFileName = certFileHold.substring(0, index - 1) + "(" + idx + ")" + certFileHold.substring(index);
				}
						
			}
			else
				certFileName = certFileHold;
			
			File certFile = new File(certFileName);
			if (certFile.exists())
				certFile.delete();
			
			
			System.out.println("Writing cert file: " + certFile.getAbsolutePath());
			
			try
			{
				if (extension.equals("der"))
					FileUtils.writeByteArrayToFile(certFile, transCert.getEncoded());		
				else
				{
					FileOutputStream outStr = null;
					KeyStore localKeyStore = KeyStore.getInstance("PKCS12", CryptoExtensions.getJCEProviderName());
					localKeyStore.load(null, null);
					
					char[] array = "".toCharArray();
					
					localKeyStore.setKeyEntry("privCert", cont.getKey(), array,  new java.security.cert.Certificate[] {cont.getCert()});

					outStr = new FileOutputStream(certFile);
					localKeyStore.store(outStr, array);	
				}
			}
			catch (Exception e)
			{
				System.out.println("Failed to write cert file: " + certFile.getAbsolutePath()  + " :" +  e.getMessage());
			}
			++idx;
		}
	}
}
