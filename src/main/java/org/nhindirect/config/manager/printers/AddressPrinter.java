package org.nhindirect.config.manager.printers;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.nhindirect.config.model.Address;

public class AddressPrinter extends AbstractRecordPrinter<Address>
{
	protected static final String ADDRESS_ID_COL = "ID";
	protected static final String ADDRESS_NAME_COL = "Adress Name";
	protected static final String DIRECT_ADDRESS_COL = "Direct Address";	
	protected static final String ENDPOINT_COL = "Endpoint";
	protected static final String TYPE_COL = "Type";

	
	protected static final Collection<ReportColumn> REPORT_COLS;
	
	static
	{
		REPORT_COLS = new ArrayList<ReportColumn>();

		REPORT_COLS.add(new ReportColumn(ADDRESS_ID_COL, 10, "Id"));
		REPORT_COLS.add(new ReportColumn(ADDRESS_NAME_COL, 35, "DisplayName"));
		REPORT_COLS.add(new ReportColumn(DIRECT_ADDRESS_COL, 50, "EmailAddress"));		
		REPORT_COLS.add(new ReportColumn(ENDPOINT_COL, 60, "Endpoint"));		
		REPORT_COLS.add(new ReportColumn(TYPE_COL, 10, "Type"));		
	}
	
	public AddressPrinter()
	{
		super(170, REPORT_COLS);
	}
	
	@Override
	protected String getColumnValue(ReportColumn column, Address record)
	{
		try
		{
			if (column.header.equals(ENDPOINT_COL))
			{
				if (StringUtils.isEmpty(record.getEndpoint()))
					return "N/A";
			}
			if (column.header.equals(TYPE_COL))
			{
				if (StringUtils.isEmpty(record.getType()))
					return "N/A";
			}			
			return super.getColumnValue(column, record);
		}
		catch (Exception e)
		{
			return "ERROR: " + e.getMessage();
		}
	}	
}
