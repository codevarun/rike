/**
 * Copyright (c) 2010 arago AG, http://www.arago.de/
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * 
 */
package de.arago.rike.task.action;

import java.util.Date;

import de.arago.portlet.Action;

import de.arago.data.IDataWrapper;
import de.arago.rike.util.TaskHelper;
import de.arago.rike.data.DataHelperRike;
import de.arago.rike.data.Milestone;
import de.arago.rike.data.Task;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

public class ViewEvaluateTask implements Action {

	public void execute(IDataWrapper data) throws Exception {

		if (data.getRequestAttribute("id") != null) {

			Task task = TaskHelper.getTask(data.getRequestAttribute("id"));
			
			if (task.getStatusEnum() == Task.Status.UNKNOWN || task.getStatusEnum() == Task.Status.OPEN)
			{
				DataHelperRike<Milestone> stoneHelper = new DataHelperRike<Milestone>(Milestone.class);
				data.setSessionAttribute("task", task);
				data.setSessionAttribute("targetView", "viewEvaluate");
				data.setSessionAttribute("milestones", stoneHelper.list(stoneHelper.filter().add(Restrictions.or(Restrictions.gt("dueDate", new Date()), Restrictions.isNull("dueDate"))).addOrder(Order.asc("dueDate")).addOrder(Order.asc("title"))));
			}
		}
	}
}
