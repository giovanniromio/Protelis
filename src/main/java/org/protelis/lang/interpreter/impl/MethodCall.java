/*******************************************************************************
 * Copyright (C) 2014, 2015, Danilo Pianini and contributors
 * listed in the project's build.gradle or pom.xml file.
 *
 * This file is part of Protelis, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE.txt in this project's top directory.
 *******************************************************************************/
package org.protelis.lang.interpreter.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.xtext.common.types.JvmOperation;
import org.protelis.lang.interpreter.AnnotatedTree;
import org.protelis.lang.util.ReflectionUtils;
import org.protelis.vm.ExecutionContext;

/**
 * Call an external Java static method.
 */
public class MethodCall extends AbstractAnnotatedTree<Object> {

    private static final long serialVersionUID = -2299070628855971997L;
    private final boolean ztatic;
    private final Class<?> clazz;
    private final String methodName;
    private transient Method method;

    /**
     * @param jvmOp
     *            the method to call
     * @param branch
     *            the Protelis sub-programs
     * @throws ClassNotFoundException
     *             in case the {@link JvmOperation} reference class couldn't be
     *             found in the classpath
     */
    public MethodCall(final JvmOperation jvmOp, final List<AnnotatedTree<?>> branch) {
        super(branch);
        final String classname = jvmOp.getDeclaringType().getQualifiedName();
        try {
            clazz = Class.forName(classname);
        } catch (ClassNotFoundException e) {
           throw new IllegalStateException(e);
        }
        ztatic = jvmOp.isStatic();
        methodName = jvmOp.getSimpleName();
        extractMethod(jvmOp.getParameters().size());
    }

    /**
     * @param clazz
     *            the class where to search for the method
     * @param methodName
     *            the method name
     * @param ztatic
     *            true if the method is static
     * @param branch
     *            method arguments
     */
    public MethodCall(final Class<?> clazz,
            final String methodName,
            final boolean ztatic,
            final List<AnnotatedTree<?>> branch) {
        super(branch);
        this.clazz = clazz;
        this.methodName = methodName;
        this.ztatic = ztatic;
        extractMethod();
    }

    private void extractMethod() {
        extractMethod(getBranchesNumber() + (ztatic ? 0 : 1));
    }

    private void extractMethod(final int parameterCount) {
        Stream<Method> methods = Arrays.stream(clazz.getMethods());
        if (ztatic) {
            methods = methods.filter(m -> Modifier.isStatic(m.getModifiers()));
        } else {
            methods = methods.filter(m -> !Modifier.isStatic(m.getModifiers()));
        }
        /*
         * Same number of arguments
         */
        if (getBranches().size() != parameterCount + (ztatic ? 0 : 1)) {
            throw new IllegalArgumentException(clazz + "." + methodName + " expects " + parameterCount + "arguments."
                    + getBranches() + "was provided instead.");
        }
        final List<Method> matches = methods.filter(m -> m.getParameterCount() == parameterCount)
                .filter(m -> m.getName().equals(methodName)).collect(Collectors.toList());
        /*
         * Same name
         */
        if (matches.isEmpty()) {
            throw new IllegalStateException("No method matches " + clazz + "." + methodName);
        }
        if (matches.size() == 1) {
            method = matches.get(0);
        }
    }

    @Override
    public void eval(final ExecutionContext context) {
        projectAndEval(context);
        // Obtain target and arguments
        final Object target = ztatic ? null : getBranch(0).getAnnotation();
        final Stream<?> s = getBranchesAnnotationStream();
        final Object[] args = ztatic ? s.toArray() : s.skip(1).toArray();
        setAnnotation(method == null
                ? ReflectionUtils.invokeFieldable(clazz, methodName, target, args)
                : ReflectionUtils.invokeFieldable(method, target, args));
    }

    @Override
    public MethodCall copy() {
        return new MethodCall(clazz, methodName, ztatic, deepCopyBranches());
    }

    @Override
    protected void asString(final StringBuilder sb, final int i) {
        sb.append(methodName);
        sb.append('(');
        fillBranches(sb, i, ',');
        sb.append(')');
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        extractMethod();
    }

}
