package org.nhindirect.config.manager.printers;

import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.nhindirect.config.model.TrustBundleAnchor;
import org.nhindirect.stagent.cert.Thumbprint;

public class BundleAnchorRecordPrinter extends AbstractRecordPrinter<TrustBundleAnchor>
{
	protected static final String ANCHOR_NAME_COL = "Anchor Name";
	protected static final String TP_NAME_COL = "Thumbprint";
	protected static final String EXPIRES_COL = "Expires";
	
	protected static final Collection<ReportColumn> REPORT_COLS;
	
	protected final DateFormat dtFormat;
	
	static
	{
		REPORT_COLS = new ArrayList<ReportColumn>();

		REPORT_COLS.add(new ReportColumn(ANCHOR_NAME_COL, 50, "AnchorAsX509Certificate"));
		REPORT_COLS.add(new ReportColumn(TP_NAME_COL, 44, "AnchorAsX509Certificate"));	
		REPORT_COLS.add(new ReportColumn(EXPIRES_COL, 20, "Expires"));			
		
	}
	
	public BundleAnchorRecordPrinter()
	{
		super(170, REPORT_COLS);
		
		dtFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
	}
	
	@Override
	protected String getColumnValue(ReportColumn column, TrustBundleAnchor record)
	{
		try
		{
			final X509Certificate anchor = record.getAsX509Certificate();
			if (column.header.equals(ANCHOR_NAME_COL))
			{
				final X500Name principal = new X500Name(anchor.getSubjectX500Principal().getName(X500Principal.RFC2253));
				final RDN[] values = principal.getRDNs(BCStyle.CN);
				final String cn = values[0].getFirst().getValue().toString();
				
				return cn;
			}
			else if (column.header.equals(TP_NAME_COL))
				return Thumbprint.toThumbprint(anchor).toString();
			else if (column.header.equals(EXPIRES_COL))
			{
				return dtFormat.format(record.getValidEndDate().getTime());
			}
			else
				return super.getColumnValue(column, record);
		}
		catch (Exception e)
		{
			return "ERROR: " + e.getMessage();
		}
	}	
}
