/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.egit.core.internal;

import org.jboss.ide.eclipse.as.core.server.internal.v7.DelegatingJBoss7ServerBehavior;
import org.jboss.ide.eclipse.as.egit.core.EgitBehaviourDelegate;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class EGitCoreActivator implements BundleActivator {

	public static final String PLUGIN_ID = "org.jboss.ide.eclipse.as.egit.core";
	
	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		EGitCoreActivator.context = bundleContext;
		DelegatingJBoss7ServerBehavior.addDelegateMapping(EgitBehaviourDelegate.ID, EgitBehaviourDelegate.class);
	}


	public void stop(BundleContext bundleContext) throws Exception {
		EGitCoreActivator.context = null;
	}
}