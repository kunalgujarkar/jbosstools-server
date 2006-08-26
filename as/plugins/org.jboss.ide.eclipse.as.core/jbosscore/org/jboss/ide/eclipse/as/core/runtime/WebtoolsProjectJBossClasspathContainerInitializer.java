/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ide.eclipse.as.core.runtime;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.core.server.JBossServer;
import org.jboss.ide.eclipse.as.core.server.runtime.JBossProjectRuntime;
import org.jboss.ide.eclipse.as.core.server.runtime.JBossServerRuntime;
import org.jboss.ide.eclipse.as.core.util.ASDebug;

public class WebtoolsProjectJBossClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	private static final IProjectFacet JST_JAVA_FACET = ProjectFacetsManager.getProjectFacet("jst.java");
	private static final IProjectFacet WEB_FACET = ProjectFacetsManager.getProjectFacet("jst.web");
	private static final IProjectFacet EJB_FACET = ProjectFacetsManager.getProjectFacet("jst.ejb");
	private static final IProjectFacet EAR_FACET = ProjectFacetsManager.getProjectFacet("jst.ear");
	private static final IProjectFacet UTILITY_FACET = ProjectFacetsManager.getProjectFacet("jst.utility");
	private static final IProjectFacet CONNECTOR_FACET = ProjectFacetsManager.getProjectFacet("jst.connector");
	private static final IProjectFacet APP_CLIENT_FACET = ProjectFacetsManager.getProjectFacet("jst.appclient");


	public WebtoolsProjectJBossClasspathContainerInitializer() {
		// TODO Auto-generated constructor stub
	}

	public void initialize(IPath containerPath, IJavaProject project)
			throws CoreException {
		WebtoolsProjectJBossClasspathContainer container = new WebtoolsProjectJBossClasspathContainer(containerPath);
		
		JavaCore.setClasspathContainer(containerPath, 
				new IJavaProject[] {project}, new IClasspathContainer[] {container}, null);
	}
	
	
	
	
	
	
	public static class WebtoolsProjectJBossClasspathContainer implements IClasspathContainer {
		private IPath path;
		private IClasspathEntry[] entries = null;

		public WebtoolsProjectJBossClasspathContainer(IPath path) {
			this.path = path;
		}
				
		public String getDescription() {
			if( path.segmentCount() < 4 ) return "JBoss Runtimes";
			return "JBoss Runtimes (" + path.segment(2) + " : " + path.segment(3) + ")";
		}

		public int getKind() {
			return IClasspathContainer.K_APPLICATION;
		}

		public IPath getPath() {
			return path;
		}
		
		
		
		public IClasspathEntry[] getClasspathEntries() {
			if( entries == null ) {
				loadClasspathEntries();
				if( entries == null ) 
					return new IClasspathEntry[0];
			}
			return entries;
		}
		
		private void loadClasspathEntries() {
			if( path.segmentCount() < 4 ) return;
			String runtimeId = path.segment(1);
			String facetId = path.segment(2);
			String facetVersion = path.segment(3);
			
			
			if( runtimeId == null ) return;
			
			IRuntime runtime = ServerCore.findRuntime(runtimeId);
			Object projectRuntime = runtime.loadAdapter(JBossProjectRuntime.class, null);

			if( projectRuntime == null ) return;
			JBossProjectRuntime  projRun = (JBossProjectRuntime)projectRuntime;
			String serverId = projRun.getServerId();
			
			if( serverId == null ) return;
			
			IServer server = ServerCore.findServer(serverId);
			JBossServer jbServer = server.loadAdapter(JBossServer.class, null) == null ? null : (JBossServer)server.getAdapter(JBossServer.class);
			if( jbServer == null ) return;
			
			
			String serverHome = server.getRuntime().getLocation().toOSString();
			String configName = jbServer.getAttributeHelper().getJbossConfiguration();
			
			ASDebug.p("runtime,config,version", this);
			JBossServerRuntime jbsRuntime = server.getRuntime().loadAdapter(JBossServerRuntime.class,new NullProgressMonitor()) == null ? null : (JBossServerRuntime)server.getRuntime().getAdapter(JBossServerRuntime.class);
			
			if( jbsRuntime == null ) return;
			String jbossVersion = jbsRuntime.getVersionDelegate().getId();
			
			entries = loadClasspathEntries2(runtimeId, facetId, facetVersion, serverHome, configName, jbossVersion, jbsRuntime);
		}

		protected IClasspathEntry[] loadClasspathEntries2(String runtimeId, String facetId, 
				String facetVersion, String serverHome, String configName, String jbVersion, 
				JBossServerRuntime jbsRuntime) {
			if( facetId.equals(JST_JAVA_FACET.getId())) {
				return loadJREClasspathEntries(jbsRuntime);
			} else if( jbVersion.equals("4.0"))
				return loadClasspathEntries40(runtimeId, facetId, facetVersion, serverHome, configName);
			if( jbVersion.equals("3.2")) 
				return loadClasspathEntries32(runtimeId, facetId, facetVersion, serverHome, configName);
			return loadClasspathEntriesDefault(runtimeId, facetId, facetVersion, serverHome, configName);
		}
		
		protected IClasspathEntry[] loadJREClasspathEntries(JBossServerRuntime jbsRuntime) {
			IVMInstall vmInstall = jbsRuntime.getVM();
			if (vmInstall != null) {
				String name = vmInstall.getName();
				String typeId = vmInstall.getVMInstallType().getId();
				return new IClasspathEntry[] { JavaCore.newContainerEntry(new Path(JavaRuntime.JRE_CONTAINER).append(typeId).append(name)) };
			}
			return null;
		}
		
		protected IClasspathEntry[] loadClasspathEntries40(String runtimeId, String facetId, String facetVersion, String serverHome, String configName) {
			IPath homePath = new Path(serverHome);
			IPath configPath = homePath.append("server").append(configName);
			ArrayList list = new ArrayList();
			if (facetId.equals(WEB_FACET.getId())) {
				list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(configPath.append("lib").append("javax.servlet.jsp.jar")).getClasspathEntry());
				list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(homePath.append("client").append("javax.servlet.jar")).getClasspathEntry());
			} else if( facetId.equals(EJB_FACET.getId()) || facetId.equals(EAR_FACET.getId())) {
				list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(homePath.append("client").append("jboss-j2ee.jar")).getClasspathEntry());
			} else if( facetId.equals(APP_CLIENT_FACET.getId())) {
				list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(homePath.append("client").append("jbossall-client.jar")).getClasspathEntry());
			}
			// || pf.equals(UTILITY_FACET) || pf.equals(CONNECTOR_FACET) 
			
			
			return (IClasspathEntry[]) list.toArray(new IClasspathEntry[list.size()]);
		}
		protected IClasspathEntry[] loadClasspathEntries32(String runtimeId, String facetId, String facetVersion, String serverHome, String configName) {
			IPath homePath = new Path(serverHome);
			IPath configPath = homePath.append("server").append(configName);
			ArrayList list = new ArrayList();
			if (facetId.equals(WEB_FACET.getId())) {
				IPath p = configPath.append("deploy").append("jbossweb-tomcat50.sar");
				list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(p.append("jsp-api.jar")).getClasspathEntry());
				list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(p.append("servlet-api.jar")).getClasspathEntry());
			} else if( facetId.equals(EJB_FACET.getId()) || facetId.equals(EAR_FACET.getId()) ) {
				list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(homePath.append("client").append("jboss-j2ee.jar")).getClasspathEntry());
			} else if( facetId.equals(APP_CLIENT_FACET.getId())) {
				list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(homePath.append("client").append("jbossall-client.jar")).getClasspathEntry());
			}
			return (IClasspathEntry[]) list.toArray(new IClasspathEntry[list.size()]);
		}
		protected IClasspathEntry[] loadClasspathEntriesDefault(String runtimeId, String facetId, String facetVersion, String serverHome, String configName) {
			return new IClasspathEntry[0];
		}

		
	}

}
