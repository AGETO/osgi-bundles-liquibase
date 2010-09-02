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

import liquibase.servicelocator.PackageScanClassResolver;
import liquibase.servicelocator.ServiceLocator;

/**
 * Equinox {@link ServiceLocator}
 */
public class EquinoxServiceLocator extends ServiceLocator {

	@Override
	protected PackageScanClassResolver createClassResolver() {
		return new EquinoxPackageScanClassResolver();
	}
}
