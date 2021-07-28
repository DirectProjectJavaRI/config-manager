package org.nhindirect.config.manager;


import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.nhind.config.rest.AddressService;
import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DNSService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhind.config.rest.TrustBundleService;
import org.nhindirect.common.tooling.Command;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.TrustBundle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class PerformanceCommands
{
	private static final String STRESS_TEST_USAGE = "Performs a stress test against the configuration service.";
	
	protected AnchorService anchorService;
	
	protected DomainService domainService;
	
	protected CertificateService certService;
	
	protected CertPolicyService certPolService;
	
	protected SettingService settingsService;
	
	protected DNSService dnsService;
	
	protected AddressService addressService;
	
	protected TrustBundleService bundleService;
	
	public PerformanceCommands(AnchorService anchorService, DomainService domainService, CertificateService certService, 
			CertPolicyService certPolService, SettingService settingsService, DNSService dnsService, AddressService addressService,
			TrustBundleService bundleService)
	{
		this.anchorService = anchorService;
		this.domainService = domainService;
		this.certService = certService;
		this.certPolService = certPolService;
		this.settingsService = settingsService;
		this.dnsService = dnsService;
		this.addressService = addressService;
		this.bundleService = bundleService;
	} 
	
	@Command(name = "StressTest", usage = STRESS_TEST_USAGE)
    public void stressTest(String[] args)
	{
		System.out.println("Intiating Stress Test.");
		
		
		
		try
		{
			
			System.out.println("\r\nRunning sequential anchors tests.");
			
			
			/*
			 * Anchor tests 
			 */
			long sequentialAnchorStartTime = System.currentTimeMillis();
			
			final Resource resource = new ClassPathResource("certs/cert-a.der");
			final byte[] certAData = IOUtils.toByteArray(resource.getInputStream());
		
			System.out.println("\tCreating anchors.");
			
			for (int i = 0; i < 500; ++i)
			{
				final Anchor newAnchor = new Anchor();
				newAnchor.setCertificateData(certAData);
				newAnchor.setIncoming(true);
				newAnchor.setOutgoing(true);
				newAnchor.setStatus(EntityStatus.ENABLED);
				newAnchor.setOwner("Stress Test" + i);
				
				try
				{
					anchorService.addAnchor(newAnchor);
				}
				catch (Exception e)
				{
					System.err.println("\tError creating anchor for owner Stress Test" + i);
				}
			}
			System.out.println("\t\tCompleted in " + (System.currentTimeMillis() - sequentialAnchorStartTime) + "ms." );
			
			
			System.out.println("\tReading all anchors.");
			
			long readAnchorsStartTime = System.currentTimeMillis();
			final Collection<Anchor> allAnchors = anchorService.getAnchors();
			System.out.println("\t\tCompleted in " + (System.currentTimeMillis() - readAnchorsStartTime) + "ms." );
			
			
			System.out.println("\tDeleting new anchors.");
			
			long deleteAnchorsStartTime = System.currentTimeMillis();
			
			for (Anchor anchor : allAnchors)
			{
				if (anchor.getOwner().contains("Stress Test"))
					anchorService.deleteAnchorsByIds(Arrays.asList(anchor.getId()));
			}
			System.out.println("\t\tCompleted in " + (System.currentTimeMillis() - deleteAnchorsStartTime) + "ms." );
			
			System.out.println("Completed sequential anchors tests in " + (System.currentTimeMillis() - sequentialAnchorStartTime) + "ms");
			
			
			/*
			 * Trust bundle tests 
			 */
			long sequentialTrustBundleStartTime = System.currentTimeMillis();
			
			System.out.println("\r\nRunning sequential trust bundle tests.");
			
			System.out.println("\tCreating trust bundles.");
			
			for (int i = 0; i < 100; ++i)
			{
				final TrustBundle newBundle = new TrustBundle();
				newBundle.setBundleName("StressTestBundle" + i);
				newBundle.setBundleURL("https://bundles.directtrust.org/bundles/accreditedCommunity.p7b");
				newBundle.setRefreshInterval(1440);
				
				try
				{
					bundleService.addTrustBundle(newBundle);
				}
				catch (Exception e)
				{
					System.err.println("\tError creating anchor for owner Stress Test" + i);
				}
			}
			System.out.println("\t\tCompleted in " + (System.currentTimeMillis() - sequentialTrustBundleStartTime) + "ms." );
			
			System.out.println("\tReading all bundles without anchors.");
			long readBundlesStartTime = System.currentTimeMillis();
			bundleService.getTrustBundles(false);
			System.out.println("\t\tCompleted in " + (System.currentTimeMillis() - readBundlesStartTime) + "ms." );
			
			
			System.out.println("\tWaiting 30 seconds for bundles to download anchors.");
			Thread.sleep(20000);
			
			System.out.println("\tReading all bundles with anchors.");
			readBundlesStartTime = System.currentTimeMillis();
			final Collection<TrustBundle> allBundles = bundleService.getTrustBundles(false);
			System.out.println("\t\tCompleted in " + (System.currentTimeMillis() - readBundlesStartTime) + "ms." );
			
			
			System.out.println("\tDeleting new trust bundles.");
			
			long deleteBundlesStartTime = System.currentTimeMillis();
			
			for (TrustBundle bundle : allBundles)
			{
				if (bundle.getBundleName().contains("StressTestBundle"))
					bundleService.deleteTrustBundle(bundle.getBundleName());
			}
			
			System.out.println("\t\tCompleted in " + (System.currentTimeMillis() - deleteBundlesStartTime) + "ms." );
			
			System.out.println("Completed sequential anchors tests in " + (System.currentTimeMillis() - sequentialTrustBundleStartTime  - 30000) + "ms");
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}	
}
