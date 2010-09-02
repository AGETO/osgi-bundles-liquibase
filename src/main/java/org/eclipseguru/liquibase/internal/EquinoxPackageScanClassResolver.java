/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipseguru.liquibase.internal;

import java.io.IOException;
import java.net.URL;

import liquibase.servicelocator.DefaultPackageScanClassResolver;

import org.eclipse.core.runtime.FileLocator;

/**
 * Package scan resolver that works with Equinox.
 */
public class EquinoxPackageScanClassResolver extends DefaultPackageScanClassResolver {

	@Override
	protected URL customResourceLocator(final URL url) throws IOException {
		return FileLocator.toFileURL(url);
	}
}
