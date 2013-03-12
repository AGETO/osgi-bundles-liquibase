/**
 * Copyright (c) 2010, 2013 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package net.ageto.liquibase.internal;

import liquibase.servicelocator.ServiceLocator;

/**
 * Equinox {@link ServiceLocator}
 */
public class EquinoxServiceLocator extends ServiceLocator {

	/**
	 * Creates a new instance.
	 */
	public EquinoxServiceLocator() {
		super(new EquinoxPackageScanClassResolver());
	}

}
