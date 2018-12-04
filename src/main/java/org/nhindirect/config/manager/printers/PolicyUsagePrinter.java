package org.nhindirect.config.manager.printers;

import java.util.ArrayList;
import java.util.Collection;

import org.nhindirect.config.model.CertPolicyGroupUse;

public class PolicyUsagePrinter extends AbstractRecordPrinter<CertPolicyGroupUse>
{
	protected static final Collection<ReportColumn> REPORT_COLS;
	
	protected static final String POLICY_NAME_COL = "Policy Name";
	protected static final String POLICY_LEXICON_COL = "Lexicon";	
	protected static final String POLICY_USAGE_COL = "Usage";		
	protected static final String INCOMING_COL = "Incoming";
	protected static final String OUTGOING_COL = "Outgoing";
			
	static
	{
		REPORT_COLS = new ArrayList<ReportColumn>();
		
		REPORT_COLS.add(new ReportColumn(POLICY_NAME_COL, 40, "PolicyName"));
		REPORT_COLS.add(new ReportColumn(POLICY_LEXICON_COL, 20, "Lexicon"));
		REPORT_COLS.add(new ReportColumn(POLICY_USAGE_COL, 20, "PolicyUse"));		
		REPORT_COLS.add(new ReportColumn(INCOMING_COL, 12, "Incoming"));
		REPORT_COLS.add(new ReportColumn(OUTGOING_COL, 12, "Outgoing"));
	}
	
	public PolicyUsagePrinter()
	{
		super(110, REPORT_COLS);
	}
	
	@Override
	protected String getColumnValue(ReportColumn column, CertPolicyGroupUse use)
	{
		try
		{
			if (column.header.equals(POLICY_NAME_COL))	
			{
				return use.getPolicy().getPolicyName();
			}
			else if (column.header.equals(POLICY_LEXICON_COL))	
			{
				return use.getPolicy().getLexicon().toString();
			}	
			else if (column.header.equals(INCOMING_COL))	
			{
				return Boolean.toString(use.isIncoming());
			}	
			else if (column.header.equals(OUTGOING_COL))	
			{
				return Boolean.toString(use.isOutgoing());
			}				
			else
				return super.getColumnValue(column, use);
		}
		catch (Exception e)
		{
			return "ERROR: " + e.getMessage();
		}
	}
}
