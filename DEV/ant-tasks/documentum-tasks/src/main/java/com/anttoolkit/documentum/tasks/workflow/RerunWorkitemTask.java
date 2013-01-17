package com.anttoolkit.documentum.tasks.workflow;

import com.anttoolkit.documentum.common.*;

import com.documentum.fc.common.*;
import com.documentum.fc.client.*;
import com.documentum.services.workflow.inbox.*;

import org.apache.tools.ant.*;

public class RerunWorkitemTask
		extends GenericDocbaseTask
{
	public void setId(String id)
	{
		m_workitem = id;
	}

	public void addText(String text)
	{
		m_workitem = getProject().replaceProperties(text);
	}

	public void doWork()
			throws BuildException
	{
		verify();

		if (m_workitem.trim().length() == 16)
		{
			rerunWorkitem(new DfId(m_workitem));
			return;
		}

		IDfCollection coll = null;

		try
		{
			coll = DqlHelper.executeReadQuery(this.getSession(), m_workitem);
			if (coll == null || !coll.next())
			{
				return;
			}

			do
			{
				IDfValue value = coll.getTypedObject().getValueAt(0);

				IDfId id = null;

				if (value.getDataType() == IDfType.DF_ID)
				{
					id = value.asId();
				}
				else if (value.getDataType() == IDfType.DF_STRING)
				{
					id = new DfId(value.asString());
				}

				if (id != null && id.isObjectId() && !id.isNull())
				{
					rerunWorkitem(id);
				}

			} while (coll.next());
		}
		catch (DfException e)
		{
			throw new BuildException("Failed to execute DQL query", e);
		}
		finally
		{
			DqlHelper.closeCollection(coll);
		}
	}

	private void verify()
			throws BuildException
	{
		if (m_workitem == null || m_workitem.trim().length() == 0)
		{
			throw new BuildException("Workitem id or workitems query should be specified");
		}

		if (m_workitem.length() < 16)
		{
			throw new BuildException("Incorrect workitem syntax: " + m_workitem);
		}

		if (m_workitem.length() == 16)
		{
			IDfId id = new DfId(m_workitem);
			if (id.isNull() || !id.isObjectId())
			{
				throw new BuildException("Incorrect workitem id=" + m_workitem + " specified");
			}

			return;
		}

		if (m_workitem.trim().toLowerCase().indexOf("select") != 0)
		{
			throw new BuildException("Incorrect workitem syntax: " + m_workitem);
		}
	}

	private void rerunWorkitem(IDfId id)
			throws BuildException
	{
		try
		{
			IInbox inboxService = (IInbox)getSession().newService(IInbox.class.getName());
			inboxService.setDocbase(getSession().getDocbaseName());

			IWorkflowTask task = inboxService.getWorkflowTask(id, true);

			task.setRerunMethod(true);
			task.completeTask();
		}
		catch (DfException e)
		{
			throw new BuildException("Failed to rerun workitem: " + id + "\r\n" + e.toString(), e);
		}
	}

	private String m_workitem = null;
}
