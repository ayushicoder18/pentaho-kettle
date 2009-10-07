 /* Copyright (c) 2007 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * Data Integration.  The Initial Developer is samatar Hassan.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.*/
 
package org.pentaho.di.trans.steps.delay;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Delay input row.
 * 
 * @author Samatar
 * @since 27-06-2008
 */
public class Delay extends BaseStep implements StepInterface
{
	private static Class<?> PKG = DelayMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private DelayMeta meta;
	private DelayData data;
	
	public Delay(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(DelayMeta)smi;
		data=(DelayData)sdi;

		Object[] r=getRow();    // get row, set busy!
		
		if (r==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
		
		if(first)
		{
			first=false;
			
			String msgScale;
			switch (meta.getScaleTimeCode())
			{
			    case 0:
			        msgScale = BaseMessages.getString(PKG, "DelayDialog.MSScaleTime.Label");
			        data.Multiple = 1;
			        break;			
			    case 1:
			        msgScale = BaseMessages.getString(PKG, "DelayDialog.SScaleTime.Label");
			        data.Multiple = 1000;
			        break;
                case 2:
                    msgScale = BaseMessages.getString(PKG, "DelayDialog.MnScaleTime.Label");
                    data.Multiple = 60000;
                    break;
                case 3:
                    msgScale = BaseMessages.getString(PKG, "DelayDialog.HrScaleTime.Label");
                    data.Multiple = 3600000;
                    break;
                default:
                    msgScale = "Unknown Scale";
                    data.Multiple = 1;
			}
			
			String timeOut=environmentSubstitute(meta.getTimeOut());
			data.timeout =Const.toInt(timeOut, 0);  
			
			if(log.isDebug()) logDebug(BaseMessages.getString(PKG, "Delay.Log.TimeOut",""+data.timeout,msgScale));
		}

		if ((data.Multiple<1000) && (data.timeout>0)) {
			// handle the milliseconds delays here
			try {
				Thread.sleep(data.timeout);
			} catch (Exception e) {
				// nothing
			}
		} else {
			 // starttime (in seconds ,Minutes or Hours)
			long timeStart = System.currentTimeMillis();

			boolean continueLoop = true;

			while (continueLoop && !isStopped()) {
				// Update Time value
				long now = System.currentTimeMillis();

				// Let's check the limit time
				if (now >= (timeStart + (data.timeout * data.Multiple))) {
					// We have reached the time limit
					continueLoop = false;
				} else {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						// handling this exception would be kind of silly.
					}
				}
			}
		}
		if (log.isDebug()) logDebug(BaseMessages.getString(PKG, "Delay.WaitTimeIsElapsed.Label"));
	     
		putRow(getInputRowMeta(), r);     // copy row to possible alternate rowset(s).
		  
		if (checkFeedback(getLinesRead())) 
		{
			if(log.isDetailed()) logDetailed(BaseMessages.getString(PKG, "Delay.Log.LineNumber",""+getLinesRead())); //$NON-NLS-1$
		}
		
		return true;
	}


	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(DelayMeta)smi;
		data=(DelayData)sdi;
		
		if (super.init(smi, sdi))
		{
		    // Add init code here.
		    return true;
		}
		return false;
	}
	
	//
	// Run is were the action happens!
	public void run()
	{
    	BaseStep.runStepThread(this, meta, data);
	}
}