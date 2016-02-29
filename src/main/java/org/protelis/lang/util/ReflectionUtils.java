/*******************************************************************************
 * Copyright (C) 2014, 2015, Danilo Pianini and contributors
 * listed in the project's build.gradle or pom.xml file.
 *
 * This file is part of Protelis, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE.txt in this project's top directory.
 *******************************************************************************/
package org.protelis.lang.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java8.util.Objects;

import java8.util.J8Arrays;
import java8.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java8.util.stream.Collectors;
import java8.util.stream.IntStreams;
import java8.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;
import org.protelis.lang.datatype.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static java8.util.stream.StreamSupport.stream;

/**
 * Utilities that make easier to cope with Java Reflection.
 */
public final class ReflectionUtils {

    private static final Logger L = LoggerFactory.getLogger(ReflectionUtils.class);
    private static final int CACHE_MAX_SIZE = 1000;
    private static final LoadingCache<Triple<Class<?>, String, List<Class<?>>>, Method> METHOD_CACHE = CacheBuilder
            .newBuilder().maximumSize(CACHE_MAX_SIZE).expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<Triple<Class<?>, String, List<Class<?>>>, Method>() {
                @Override
                public Method load(final Triple<Class<?>, String, List<Class<?>>> key) {
                    final List<Class<?>> al = key.getRight();
                    final Class<?>[] args = new Class<?>[al.size()];
                    return loadBestMethod(key.getLeft(), key.getMiddle(), al.toArray(args));
                }
            });

    private ReflectionUtils() {
    }

    /**
     * @param methodName
     *            the method to be invoked
     * @param target
     *            the target object. It can not be null
     * @param args
     *            the arguments for the method
     * @return the result of the invocation, or an {@link IllegalStateException}
     *         if something goes wrong.
     */
    public static Object invokeBestNotStatic(final Object target, final String methodName, final Object[] args) {
        Objects.requireNonNull(target);
        return invokeBestMethod(target.getClass(), methodName, target, args);
    }

    /**
     * @param clazz
     *            the class where to search for suitable methods
     * @param methodName
     *            the method to be invoked
     * @param args
     *            the arguments for the method
     * @return the result of the invocation, or an {@link IllegalStateException}
     *         if something goes wrong.
     */
    public static Object invokeBestStatic(final Class<?> clazz, final String methodName, final Object... args) {
        return invokeBestMethod(clazz, methodName, null, args);
    }

    /**
     * @param clazz
     *            the class where to search for suitable methods
     * @param methodName
     *            the method to be invoked
     * @param args
     *            the arguments for the method
     * @return the result of the invocation, or an {@link IllegalStateException}
     *         if something goes wrong.
     */
    public static Method searchBestMethod(final Class<?> clazz, final String methodName, final Object... args) {
        return searchBestMethod(clazz, methodName, Arrays.asList(args));
    }

    /**
     * @param clazz
     *            the class where to search for suitable methods
     * @param methodName
     *            the method to be invoked
     * @param args
     *            the arguments for the method
     * @return the result of the invocation, or an {@link IllegalStateException}
     *         if something goes wrong.
     */
    public static Method searchBestMethod(final Class<?> clazz, final String methodName, final List<Object> args) {
        final List<Class<?>> argClass = Arrays.asList(stream(args)
                .map(Object::getClass)
                .toArray(length -> new Class<?>[length]));
        try {
            return METHOD_CACHE.get(new ImmutableTriple<>(clazz, methodName, argClass));
        } catch (ExecutionException e) {
            throw new NoSuchMethodError(methodName + "/" + args.size() + argClass + " does not exist in " + clazz
                    + ". You tried to invoke it with arguments " + args);
        }
    }

    private static Method loadBestMethod(final Class<?> clazz, final String methodName, final Class<?>[] argClass) {
        Objects.requireNonNull(clazz, "The class on which the method will be invoked can not be null.");
        Objects.requireNonNull(methodName, "Method name can not be null.");
        Objects.requireNonNull(argClass, "Method arguments can not be null.");
        /*
         * If there is a matching method, return it
         */
        try {
            return clazz.getMethod(methodName, argClass);
        } catch (NoSuchMethodException | SecurityException e) {
            /*
             * Look it up on the cache
             */
            /*
             * Deal with Java method overloading scoring methods
             */
            final Method[] candidates = clazz.getMethods();
            final List<Pair<Integer, Method>> lm = new ArrayList<>(candidates.length);
            for (final Method m : candidates) {
            	//OLD m.getParameterCount()
                if (m.getParameterTypes().length == argClass.length && methodName.equals(m.getName())) {
                    final Class<?>[] params = m.getParameterTypes();
                    int p = 0;
                    boolean compatible = true;
                    for (int i = 0; compatible && i < argClass.length; i++) {
                        final Class<?> expected = params[i];
                        if (expected.isAssignableFrom(argClass[i])) {
                            /*
                             * No downcast required, there is compatibility
                             */
                            p++;
                        } else if (!PrimitiveUtilsBackport.classIsNumber(expected)) {
                            compatible = false;
                        }
                    }
                    if (compatible) {
                        lm.add(new Pair<>(p, m));
                    }
                }
            }
            /*
             * Find best
             */
            final Optional<Method> best = stream(lm).max((pm1, pm2) -> pm1.getFirst().compareTo(pm2.getFirst()))
                    .map(Pair::getSecond);
            if (best.isPresent()) {
                return best.get();
            }
        }
        final String argType = J8Arrays.stream(argClass).collect(Collectors.toList()).toString();
        throw new NoSuchMethodError(methodName + "/" + argClass.length + argType + " does not exist in " + clazz + ".");
    }

    /**
     * @param clazz
     *            the class where to search for suitable methods
     * @param methodName
     *            the method to be invoked
     * @param target
     *            the target object. It can be null, if the method which is
     *            being invoked is static
     * @param args
     *            the arguments for the method
     * @return the result of the invocation, or an {@link IllegalStateException}
     *         if something goes wrong.
     */
    public static Object invokeBestMethod(
            final Class<?> clazz,
            final String methodName,
            final Object target,
            final Object[] args) {
        return invokeMethod(searchBestMethod(clazz, methodName, args), target, args);
    }

    /**
     * Invokes a method. If there are fields involved, field operations are
     * applied
     * 
     * @param clazz
     *            the class to search for a method
     * @param methodName
     *            the name of the method
     * @param target
     *            the target object (can be null in case of static invocation)
     * @param args
     *            the arguments for the method
     * @return the result of the method invocation
     */
    public static Object invokeFieldable(
            final Class<?> clazz,
            final String methodName,
            final Object target,
            final Object[] args) {
        if (Field.class.isAssignableFrom(clazz) && target instanceof Field) {
            return invokeFieldable(
                    ((Field) target).valIterator().iterator().next().getClass(),
                    methodName,
                    target,
                    args);
        }
        return invokeFieldable(searchBestMethod(clazz, methodName, args), target, args);
    }

    /**
     * Invokes a method. If there are fields involved, field operations are
     * applied
     * 
     * @param toInvoke
     *            the method to be invoked
     * @param target
     *            the target object (can be null in case of static invocation)
     * @param args
     *            the arguments for the method
     * @return the result of the method invocation
     */
    public static Object invokeFieldable(
            final Method toInvoke,
            final Object target,
            final Object[] args) {
        final boolean fieldTarget = target instanceof Field;
        Stream<Object> str = J8Arrays.stream(args).parallel();
        /*
         * Filter the fields
         */
        str = str.filter(o -> Field.class.isAssignableFrom(o.getClass()));
        /*
         * Store their indexes
         */
        final int[] fieldIndexes = str.mapToInt(o -> ArrayUtils.indexOf(args, o)).toArray();
        if (fieldTarget || fieldIndexes.length > 0) {
            return Field.apply(
                    (actualT, actualA) -> ReflectionUtils.invokeMethod(toInvoke, actualT, actualA),
                    fieldTarget,
                    fieldIndexes,
                    target,
                    args);
        }
        return ReflectionUtils.invokeMethod(toInvoke, target, args);
    }

    /**
     * @param method
     *            the methods to invoke
     * @param target
     *            the target object. It can be null, if the method which is
     *            being invoked is static
     * @param args
     *            the arguments for the method
     * @return the result of the invocation, or an {@link IllegalStateException}
     *         if something goes wrong.
     */
    public static Object invokeMethod(final Method method, final Object target, final Object[] args) {
        try {
            return method.invoke(target, args);
        } catch (Exception exc) {
            /*
             * Failure: maybe some cast was required?
             */
            final Class<?>[] params = method.getParameterTypes();
            final Object[] actualArgs = IntStreams.range(0, args.length).parallel().mapToObj(i -> {
                final Class<?> expected = params[i];
                final Object actual = args[i];
                if (!expected.isAssignableFrom(actual.getClass()) && PrimitiveUtilsBackport.classIsNumber(expected)) {
                    return PrimitiveUtilsBackport.castIfNeeded(expected, (Number) actual).get();
                }
                return actual;
            }).toArray();
            try {
                return method.invoke(target, actualArgs);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                L.error("Error invoking method", e);
                throw new IllegalStateException(
                        "Cannot invoke " + method + " with arguments " + Arrays.toString(args) + " on " + target, e);
            }
        }
    }

}
