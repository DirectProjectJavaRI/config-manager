package org.nhindirect.config.manager.printers;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.nhindirect.common.cert.Thumbprint;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.utils.CertUtils;

public class AnchorRecordPrinter extends AbstractRecordPrinter<Anchor>
{
	
	protected static final String ANCHOR_ID_COL = "ID";
	protected static final String ANCHOR_NAME_COL = "Anchor Name";
	protected static final String TP_NAME_COL = "Thumbprint";
	protected static final String OWNER_COL = "Owner";
	protected static final String INCOMING_COL = "Incoming";	
	protected static final String OUTGOING_COL = "Outgoing";	
	
	protected static final Collection<ReportColumn> REPORT_COLS;
	
	static
	{
		REPORT_COLS = new ArrayList<ReportColumn>();

		REPORT_COLS.add(new ReportColumn(ANCHOR_ID_COL, 14, "Id"));
		REPORT_COLS.add(new ReportColumn(ANCHOR_NAME_COL, 50, "AnchorAsX509Certificate"));
		REPORT_COLS.add(new ReportColumn(TP_NAME_COL, 44, "AnchorAsX509Certificate"));		
		REPORT_COLS.add(new ReportColumn(OWNER_COL, 40, "Owner"));	
		REPORT_COLS.add(new ReportColumn(INCOMING_COL, 10, "Incoming"));	
		REPORT_COLS.add(new ReportColumn(OUTGOING_COL, 10, "Outgoing"));			
	}
	
	public AnchorRecordPrinter()
	{
		super(170, REPORT_COLS);
	}
	
	@Override
	protected String getColumnValue(ReportColumn column, Anchor record)
	{
		try
		{
			final X509Certificate anchor = CertUtils.toX509Certificate(record.getCertificateData());
			if (column.header.equals(ANCHOR_NAME_COL))
			{
				final X500Name principal = new X500Name(anchor.getSubjectX500Principal().getName(X500Principal.RFC2253));
				final RDN[] values = principal.getRDNs(BCStyle.CN);
				final String cn = values[0].getFirst().getValue().toString();
				
				return cn;
			}
			else if (column.header.equals(TP_NAME_COL))
				return Thumbprint.toThumbprint(anchor).toString();
			else if (column.header.equals(INCOMING_COL))
				return 	Boolean.valueOf(record.isIncoming()).toString();
			else if (column.header.equals(OUTGOING_COL))
				return 	Boolean.valueOf(record.isOutgoing()).toString();			
			else
				return super.getColumnValue(column, record);
		}
		catch (Exception e)
		{
			return "ERROR: " + e.getMessage();
		}
	}	
}
