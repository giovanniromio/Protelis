/*******************************************************************************
 * Copyright (C) 2010, 2015, Danilo Pianini and contributors
 * listed in the project's build.gradle or pom.xml file.
 *
 * This file is part of Protelis, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE.txt in this project's top directory.
 *******************************************************************************/
package org.protelis.vm.impl;

import java.util.Random;
import java8.util.stream.IntStreams;

import org.protelis.lang.datatype.DatatypeFactory;
import org.protelis.lang.datatype.DeviceUID;
import org.protelis.lang.datatype.Field;

/**
 * A dummy Protelis VM to be used for testing.
 *
 */
public final class DummyContext extends AbstractExecutionContext {

    private final Random rng = new Random(0);
    private static final DeviceUID DUMMYUID = new DeviceUID() {
        private static final long serialVersionUID = 2306021805006825289L;
        @Override
        public String toString() {
            return "DummyUID";
        };
    };

    /**
     *
     */
    public DummyContext() {
        super(new SimpleExecutionEnvironment(), new DummyNetworkManager());
    }

    @Override
    public DeviceUID getDeviceUID() {
        return DUMMYUID;
    }

    @Override
    public Number getCurrentTime() {
        return System.currentTimeMillis() / 1000d;
    }

    @Override
    public double nextRandomDouble() {
        return rng.nextDouble();
    }

    @Override
    protected AbstractExecutionContext instance() {
        return new DummyContext();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + hashCode();
    }

    /**
     * Test utility.
     * 
     * @return a field with populated with numbers from 0 to 99
     */
    public static Field makeTestField() {
        final Field res = DatatypeFactory.createField(100);
        IntStreams.range(0, 100).forEach(n -> res.addSample(
                new DeviceUID() {
                    private static final long serialVersionUID = 1L;
                }, (double) n));
        return res;
    }

}
