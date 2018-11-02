package org.nhindirect.config.manager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.DomainService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.nhindirect.common.tooling.Command;
import org.nhindirect.common.tooling.StringArrayUtil;
import org.nhindirect.config.manager.printers.PolicyGroupPrinter;
import org.nhindirect.config.manager.printers.PolicyPrinter;
import org.nhindirect.config.manager.printers.PolicyUsagePrinter;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.model.CertPolicyGroupUse;
import org.nhindirect.config.model.CertPolicyUse;
import org.nhindirect.config.model.Domain;
import org.nhindirect.policy.PolicyLexicon;
import org.nhindirect.policy.PolicyLexiconParser;
import org.nhindirect.policy.PolicyLexiconParserFactory;
import org.nhindirect.policy.PolicyParseException;


public class PolicyCommands 
{
    private static final String LIST_POLICIES_USAGE = "Lists policies in the system";
    
    private static final String IMPORT_POLICY_USAGE = "Imports a policy from a file with an optional lexicon definition." +
    		"\r\n  policyName policyDefFile [lexicon]" +
            "\r\n\t policyName: Name of the policy.  Place the policy name in quotes (\"\") if there are spaces in the name."  +   
            "\r\n\t policyDefFile: Fully qualified path and file name of the policy definition file.  Place the file name in quotes (\"\") if there are spaces in the path or name." +
            "\r\n\t [lexicon]: Optional lexicon of the policy definition.  Default to SIMPLE_TEXT_V1 if not supplied.";   
   
    private static final String DELETE_POLICY_USAGE = "Deletes a policy from the system by policy name." +
    		"\r\n  policyName " +
            "\r\n\t policyName: Name of the policy.  Place the policy name in quotes (\"\") if there are spaces in the name.";
    
    private static final String LIST_POLICY_GROUPS_USAGE = "Lists policy groups in the system";
    
    private static final String ADD_POLICY_GROUP_USAGE = "Adds policy group to the system" +
    		"\r\n  groupName " +
            "\r\n\t groupName: Name of the policy group.  Place the policy group name in quotes (\"\") if there are spaces in the name.";
    
    private static final String DELETE_POLICY_GROUP_USAGE = "Deletes a policy group from the system by policy group name." +
    		"\r\n  groupName " +
            "\r\n\t groupName: Name of the policy group.  Place the policy group name in quotes (\"\") if there are spaces in the name.";
    
    private static final String LIST_GROUP_POLICIES_USAGE = "List policies and usage within a policy group." +
    		"\r\n  groupName " +
            "\r\n\t groupName: Name of the policy group.  Place the policy group name in quotes (\"\") if there are spaces in the name.";
   
    private static final String ADD_POLICY_TO_GROUP_USAGE = "Adds an existing policy to a group with a provided usage." +
    		"\r\n  policyName groupNames policyUse incoming outgoing" +
            "\r\n\t policyName: Name of the policy to add to the group.  Place the policy name in quotes (\"\") if there are spaces in the name." +
    		"\r\n\t groupName: Name of the policy group to add the policy to.  Place the policy group name in quotes (\"\") if there are spaces in the name." +
    		"\r\n\t policyUse: Usage name of the policy in the group.  Must be one of the following values: TRUST, PRIVATE_RESOLVER, PUBLIC_RESOLVER." +
    		"\r\n\t incoming: Indicates if policy is used for incoming messages.  Must be one of the following values: true, false" +
    		"\r\n\t outgoing: Indicates if policy is used for outgoing messages.  Must be one of the following values: true, false";
    
    private static final String DELETE_POLICY_FROM_GROUP_USAGE = "Deletes an existing policy from a group." +
    		"\r\n  policyName groupName" +
            "\r\n\t policyName: Name of the policy to delete from the group.  Place the policy name in quotes (\"\") if there are spaces in the name." +
    		"\r\n\t groupName: Name of the policy group to delete the policy from.  Place the policy group name in quotes (\"\") if there are spaces in the name.";
    
    private static final String LIST_DOMAIN_POLICY_GROUPS = "List policy groups within a domain" +
    		"\r\n  domainName" +
    		"\r\n\t domainName: Name of the domain.";
    
    private static final String ADD_GROUP_TO_DOMAIN_USAGE = "Adds an existing policy group to an existing domain." +
    		"\r\n  groupName domainName" +
    		"\r\n\t groupName: Name of the policy group to add to the domain.  Place the policy group name in quotes (\"\") if there are spaces in the name." +
			"\r\n\t domainName: Name of the domain to add the group to."; 
    
    private static final String DELETE_GROUP_FROM_DOMAIN_USAGE = "Deletes an existing policy group from a domain." +
    		"\r\n  groupName domainName " +
            "\r\n\t groupName: Name of the policy group to delete from the domain.  Place the policy group name in quotes (\"\") if there are spaces in the name." +
    		"\r\n\t domainName: Name of the domain to delete the policy group from.";
    
	protected CertPolicyService certPolService;
	protected DomainService domainService;
	
	protected final PolicyPrinter policyPrinter;
	protected final PolicyGroupPrinter groupPrinter;
	protected final PolicyUsagePrinter policyUsagePrinter;
	
	public PolicyCommands(CertPolicyService certPolService, DomainService domainService)
	{
		this.certPolService = certPolService;
		this.domainService = domainService;
		
		policyPrinter = new PolicyPrinter();
		groupPrinter = new PolicyGroupPrinter();
		policyUsagePrinter = new PolicyUsagePrinter();
	}    
	
	@Command(name = "ListPolicies", usage = LIST_POLICIES_USAGE)
    public void listPolicies(String[] args)
	{
		try
		{
			final Collection<CertPolicy> policies = certPolService.getPolicies();
			if (policies == null || policies.size() == 0)
				System.out.println("No policies found");
			else
			{
				policyPrinter.printRecords(policies);
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policies: " + e.getMessage());
		}

	}		
	
	@SuppressWarnings("deprecation")
	@Command(name = "ImportPolicy", usage = IMPORT_POLICY_USAGE)
    public void importPolicy(String[] args)
	{
		final String policyName = StringArrayUtil.getRequiredValue(args, 0);
		final String fileLoc = StringArrayUtil.getRequiredValue(args, 1);
		final String lexicon = StringArrayUtil.getOptionalValue(args, 2, "");
		
		// check if the policy already exists
		try
		{
			CertPolicy policy = certPolService.getPolicyByName(policyName);
			if (policy != null)
			{
				System.out.println("Policy with name " + policyName + " already exists.");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy: " + e.getMessage());
			return;
		}
		
		PolicyLexicon lex;

		
		if (lexicon.isEmpty())
			lex = PolicyLexicon.SIMPLE_TEXT_V1;
		else
		{
			try
			{
				lex = PolicyLexicon.valueOf(lexicon);
			}
			catch (Exception e)
			{
				System.out.println("Invalid lexicon name.");
				return;
			}
		}
		
		// validate the policy syntax
		final org.nhindirect.policy.PolicyLexicon parseLexicon;
		if (lex.equals(PolicyLexicon.JAVA_SER))
			parseLexicon = org.nhindirect.policy.PolicyLexicon.JAVA_SER;
		else if (lex.equals(PolicyLexicon.SIMPLE_TEXT_V1))
			parseLexicon = org.nhindirect.policy.PolicyLexicon.SIMPLE_TEXT_V1;
		else
			parseLexicon = org.nhindirect.policy.PolicyLexicon.XML;		
		
		byte[] policyBytes;
		InputStream inStr = null;
		try
		{
			policyBytes = FileUtils.readFileToByteArray(new File(fileLoc));
			inStr = new ByteArrayInputStream(policyBytes);
			
			final PolicyLexiconParser parser = PolicyLexiconParserFactory.getInstance(parseLexicon);
			parser.parse(inStr);
		}
		catch (PolicyParseException e)
		{
			System.out.println("Syntax error in policy file " + fileLoc + " : " + e.getMessage());
			return;
		}
		catch (IOException e)
		{
			System.out.println("Error reading file " + fileLoc + " : " + e.getMessage());
			return;
		}
		finally
		{
			IOUtils.closeQuietly(inStr);
		}
		
		
		try
		{			
			CertPolicy addPolicy = new CertPolicy();
			addPolicy.setPolicyData(policyBytes);
			addPolicy.setPolicyName(policyName);
			addPolicy.setLexicon(lex);

			certPolService.addPolicy(addPolicy);
			System.out.println("Successfully imported policy.");
			
		}
		catch (Exception e)
		{
			System.out.println("Error importing certificate " + fileLoc + " : " + e.getMessage());
		}	
	}		
	
	@Command(name = "DeletePolicy", usage = DELETE_POLICY_USAGE)
    public void deletePolicy(String[] args)
	{
		// make sure the policy exists
		final String policyName = StringArrayUtil.getRequiredValue(args, 0);
		CertPolicy policy = null;
		
		try
		{
			policy = certPolService.getPolicyByName(policyName);
			if (policy == null)
			{
				System.out.println("No policy with name " + policyName + " found");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy: " + e.getMessage());
			return;
		}
		
		// now delete the policy
		try
		{
			certPolService.deletePolicy(policy.getPolicyName());
			System.out.println("Policy successfully deleted");
		}
		catch (Exception e)
		{
			System.out.println("Failed to delete policy: " + e.getMessage());
			return;
		}

	}	
	
	@Command(name = "ListPolicyGroups", usage = LIST_POLICY_GROUPS_USAGE)
    public void listPolicyGroups(String[] args)
	{
		try
		{
			Collection<CertPolicyGroup> groups = certPolService.getPolicyGroups();
			if (groups == null || groups.size() == 0)
				System.out.println("No policy groups found");
			else
			{
				groupPrinter.printRecords(groups);
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policies: " + e.getMessage());
		}
	}
	
	@Command(name = "AddPolicyGroup", usage = ADD_POLICY_GROUP_USAGE)
    public void addPolicyGroup(String[] args)
	{
		final String policyGroupName = StringArrayUtil.getRequiredValue(args, 0);
		
		// check if the group already exists
		try
		{
			CertPolicyGroup policyGroup = certPolService.getPolicyGroup(policyGroupName);
			if (policyGroup != null)
			{
				System.out.println("Policy group with name " + policyGroupName + " already exists.");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy: " + e.getMessage());
			return;
		}
		
		// now add the group
		try
		{
			CertPolicyGroup policyGroup = new CertPolicyGroup();
			policyGroup.setPolicyGroupName(policyGroupName);
			
			certPolService.addPolicyGroup(policyGroup);
			
			System.out.println("Successfully added policy group.");
		}
		catch (Exception e)
		{
			System.out.println("Failed to add policy group: " + e.getMessage());
			return;
		}
	}
	
	@Command(name = "DeletePolicyGroup", usage = DELETE_POLICY_GROUP_USAGE)
    public void deletePolicyGroup(String[] args)
	{
		// make sure the group exists
		final String policyGroupName = StringArrayUtil.getRequiredValue(args, 0);
		CertPolicyGroup group = null;
		
		try
		{
			group = certPolService.getPolicyGroup(policyGroupName);
			if (group == null)
			{
				System.out.println("No policy group with name " + policyGroupName + " found");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy group: " + e.getMessage());
			return;
		}
		
		// now delete the policy group
		try
		{
			certPolService.deletePolicyGroup(group.getPolicyGroupName());
			System.out.println("Policy groups successfully deleted");
		}
		catch (Exception e)
		{
			System.out.println("Failed to delete policy group: " + e.getMessage());
			return;
		}

	}
	
	@Command(name = "ListGroupPolicies", usage = LIST_GROUP_POLICIES_USAGE)
    public void listGroupPolicies(String[] args)
	{
		// make sure the group exists
		final String policyGroupName = StringArrayUtil.getRequiredValue(args, 0);
		CertPolicyGroup group = null;
		
		try
		{
			group = certPolService.getPolicyGroup(policyGroupName);
			if (group == null)
			{
				System.out.println("No policy group with name " + policyGroupName + " found");
				return;
			}
			else if (group.getPolicies() == null || group.getPolicies().size() == 0)
			{
				System.out.println("Group has no policies associated with it.");
				return;
			}
			
			policyUsagePrinter.printRecords(group.getPolicies());
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy group: " + e.getMessage());
			return;
		}
	}	
	
	@Command(name = "AddPolicyToGroup", usage = ADD_POLICY_TO_GROUP_USAGE)
    public void addPolicyToGroup(String[] args)
	{
		// make sure the group exists
		final String policyName = StringArrayUtil.getRequiredValue(args, 0);
		final String groupName = StringArrayUtil.getRequiredValue(args, 1);
		final String policyUse = StringArrayUtil.getRequiredValue(args, 2);
		final boolean incoming = Boolean.parseBoolean(StringArrayUtil.getRequiredValue(args, 3));
		final boolean outgoing = Boolean.parseBoolean(StringArrayUtil.getRequiredValue(args, 4));
		
		// make sure the policy exists
		CertPolicy policy = null;
		try
		{
			policy = certPolService.getPolicyByName(policyName);
			if (policy == null)
			{
				System.out.println("No policy with name " + policyName + " found");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy: " + e.getMessage());
			return;
		}
		
		// make sure the group exists
		CertPolicyGroup group = null;
		try
		{
			group = certPolService.getPolicyGroup(groupName);
			if (group == null)
			{
				System.out.println("No policy group with name " + groupName + " found");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy group: " + e.getMessage());
			return;
		}
		
		final CertPolicyUse use = CertPolicyUse.valueOf(policyUse);
		if (use == null)
		{
			System.out.println("Unknow usage type");
			return;
		}
			
		try
		{
			final CertPolicyGroupUse groupUse = new CertPolicyGroupUse();
			groupUse.setIncoming(incoming);
			groupUse.setOutgoing(outgoing);
			groupUse.setPolicyUse(use);
			groupUse.setPolicy(policy);
			
			certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), groupUse);
			System.out.println("Successfully added policy to group.");
		}
		catch (Exception e)
		{
			System.out.println("Failed to add policy to group: " + e.getMessage());
			return;
		}
	}
	
	@Command(name = "DeletePolicyFromGroup", usage = DELETE_POLICY_FROM_GROUP_USAGE)
    public void deletePolicyFromGroup(String[] args)
	{
		// make sure the group exists
		final String policyName = StringArrayUtil.getRequiredValue(args, 0);
		final String groupName = StringArrayUtil.getRequiredValue(args, 1);
		CertPolicyGroupUse policyUse = null;
		
		// make sure the group exists
		CertPolicyGroup group = null;
		try
		{
			group = certPolService.getPolicyGroup(groupName);
			if (group == null)
			{
				System.out.println("No policy group with name " + groupName + " found");
				return;
			}
			else
			{
				if (group.getPolicies() == null || group.getPolicies().size() == 0)
				{
					System.out.println("Policy is not associated with group.");
					return;
				}
				else
				{
					for (CertPolicyGroupUse reltn : group.getPolicies())
					{
						if (reltn.getPolicy().getPolicyName().compareToIgnoreCase(policyName) == 0)
						{
							policyUse = reltn;
							break;
						}
							
					}
					if (policyUse == null)
					{
						System.out.println("Policy is not associated with group.");
						return;
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy group: " + e.getMessage());
			return;
		}
			
		try
		{
			certPolService.removePolicyUseFromGroup(groupName, policyUse); 
			System.out.println("Successfully delete policy from group.");
		}
		catch (Exception e)
		{
			System.out.println("Failed to delete policy from group: " + e.getMessage());
			return;
		}
	}
	
	@Command(name = "ListDomainPolicyGroups", usage = LIST_DOMAIN_POLICY_GROUPS)
    public void listDomainPolicyGroups(String[] args)
	{
		final String domainName = StringArrayUtil.getRequiredValue(args, 0);
		
		// make sure the domain exists
		Collection<Domain> domains;
		try
		{
			domains = domainService.searchDomains(domainName, null);
			if (domains == null || domains.size() == 0)
			{
				System.out.println("No domain with name " + domainName + " found");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup domain: " + e.getMessage());
			return;
		}
		
		try
		{
			final Collection<CertPolicyGroup> groups = certPolService.getPolicyGroupsByDomain(domainName);
			if (groups == null || groups.size() == 0)
			{
				System.out.println("Domain does not have any policy groups associated with it.");
				return;
			}
			
			groupPrinter.printRecords(groups);
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup domain policy groups: " + e.getMessage());
			return;
		}
	}	
	
	@Command(name = "AddPolicyGroupToDomain", usage = ADD_GROUP_TO_DOMAIN_USAGE)
    public void addGroupToDomain(String[] args)
	{
		// make sure the group exists
		final String groupName = StringArrayUtil.getRequiredValue(args, 0);
		final String domainName = StringArrayUtil.getRequiredValue(args, 1);
		
		
		// make sure the group exists
		CertPolicyGroup group = null;
		try
		{
			group = certPolService.getPolicyGroup(groupName);
			if (group == null)
			{
				System.out.println("No policy group with name " + groupName + " found");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup policy group: " + e.getMessage());
			return;
		}
		
		// make sure the domain exists
		Collection<Domain> domains;
		try
		{
			domains = domainService.searchDomains(domainName, null);
			if (domains == null || domains.size() == 0)
			{
				System.out.println("No domain with name " + domainName + " found");
				return;
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup domain: " + e.getMessage());
			return;
		}
		
		// make sure it's not already associated
		try
		{
			final Collection<CertPolicyGroup> groups = certPolService.getPolicyGroupsByDomain(domainName);
			if (groups != null && groups.size() > 0)
			{
				boolean reltnExists = false;
				for (CertPolicyGroup existingGroup : groups)
				{
					if (existingGroup.getPolicyGroupName().compareToIgnoreCase(groupName) == 0)
					{
						reltnExists = true;
						break;
					}
				}
				if (reltnExists)
				{
					System.out.println("Group " + groupName + " already associated with domain " + domainName);
					return;
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup existing group to domain associations: " + e.getMessage());
			return;
		}
		
		// now make the association
		try
		{
			certPolService.associatePolicyGroupToDomain(groupName, domainName);
			System.out.println("Successfully added policy to group.");
		}
		catch (Exception e)
		{
			System.out.println("Failed to add group to domain: " + e.getMessage());
			return;
		}
	}
	
	@Command(name = "DeletePolicyGroupFromDomain", usage = DELETE_GROUP_FROM_DOMAIN_USAGE)
    public void deletePolicyGroupFromDomain(String[] args)
	{
		// make sure the group exists
		final String groupName = StringArrayUtil.getRequiredValue(args, 0);
		final String domainName = StringArrayUtil.getRequiredValue(args, 1);
		CertPolicyGroup existingPolicyGroup = null;
		
		// make sure the domain exists
		Collection<Domain> domains;
		try
		{
			domains = domainService.searchDomains(domainName, null);
			if (domains == null || domains.size() == 0)
			{
				System.out.println("No domain with name " + domainName + " found");
				return;
			}
			
			// make sure it's really associated
			final Collection<CertPolicyGroup> groups = certPolService.getPolicyGroupsByDomain(domainName);
			if (groups == null || groups.size() == 0)
			{
				System.out.println("Policy group is not associated with domain.");
				return;
			}
			else
			{
				for (CertPolicyGroup group : groups)
				{
					if (group.getPolicyGroupName().compareToIgnoreCase(groupName) == 0)
					{
						existingPolicyGroup = group;
						break;
					}
						
				}
				if (existingPolicyGroup == null)
				{
					System.out.println("Policy group is not associated with domain.");
					return;
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to lookup domain: " + e.getMessage());
			return;
		}
			
		try
		{
			certPolService.disassociatePolicyGroupFromDomain(groupName, domainName);
			System.out.println("Successfully delete policy group from domain.");
		}
		catch (Exception e)
		{
			System.out.println("Failed to delete policy group from domain: " + e.getMessage());
			return;
		}
	}
}
