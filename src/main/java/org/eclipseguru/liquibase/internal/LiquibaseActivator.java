package org.eclipseguru.liquibase.internal;

import liquibase.servicelocator.ServiceLocator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class LiquibaseActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		LiquibaseActivator.context = bundleContext;

		// set Equinox service locator
		ServiceLocator.setInstance(new EquinoxServiceLocator());
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		// unset Equinox service locator
		ServiceLocator.setInstance(null);

		LiquibaseActivator.context = null;
	}

}
