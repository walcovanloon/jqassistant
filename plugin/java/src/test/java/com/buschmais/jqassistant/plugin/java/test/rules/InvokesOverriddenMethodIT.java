package com.buschmais.jqassistant.plugin.java.test.rules;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import com.buschmais.jqassistant.core.analysis.api.AnalysisException;
import org.junit.Test;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.buschmais.jqassistant.plugin.java.test.matcher.TypeDescriptorMatcher;
import com.buschmais.jqassistant.plugin.java.test.set.rules.java.ClassType;
import com.buschmais.jqassistant.plugin.java.test.set.rules.java.InterfaceType;
import com.buschmais.jqassistant.plugin.java.test.set.rules.java.InvokeClient;
import com.buschmais.jqassistant.plugin.java.test.set.rules.java.SubClassType;

/**
 * Tests for the concept java:InvokeOverriddenMethod.
 */
public class InvokesOverriddenMethodIT extends AbstractPluginIT {

    /**
     * Verifies the concept "java:InvokeOverriddenMethod" for a class
     * implementing an interface.
     * 
     * @throws java.io.IOException
     *             If the test fails.
     * @throws com.buschmais.jqassistant.core.analysis.api.AnalysisException
     *             If the test fails.
     */
    @Test
    public void invokeInterfaceMethod() throws IOException, AnalysisException, NoSuchMethodException {
        scanClasses(ClassType.class, InterfaceType.class, InvokeClient.class);
        applyConcept("java:InvokesOverriddenMethod");
        store.beginTransaction();
        List<Object> classes = query(
                "MATCH (client:TYPE)-[:DECLARES]->(clientMethod:METHOD)-[:INVOKES]->(invokedMethod:METHOD)<-[:DECLARES]-(type:TYPE) WHERE client.NAME='InvokeClient' and clientMethod.NAME='invokeInterfaceTypeMethod' RETURN type")
                .getColumn("type");
        assertThat(classes, hasItem(TypeDescriptorMatcher.typeDescriptor(InterfaceType.class)));
        assertThat(classes, hasItem(TypeDescriptorMatcher.typeDescriptor(ClassType.class)));
        store.commitTransaction();
    }

    /**
     * Verifies the concept "java:InvokeOverriddenMethod" for a sub class
     * extending a class.
     * 
     * @throws java.io.IOException
     *             If the test fails.
     * @throws com.buschmais.jqassistant.core.analysis.api.AnalysisException
     *             If the test fails.
     */
    @Test
    public void invokeClassMethod() throws IOException, AnalysisException, NoSuchMethodException {
        scanClasses(ClassType.class, SubClassType.class, InvokeClient.class);
        applyConcept("java:InvokesOverriddenMethod");
        store.beginTransaction();
        List<Object> classes = query(
                "MATCH (client:TYPE)-[:DECLARES]->(clientMethod:METHOD)-[:INVOKES]->(invokedMethod:METHOD)<-[:DECLARES]-(type:TYPE) WHERE client.NAME='InvokeClient' and clientMethod.NAME='invokeClassTypeMethod' RETURN type")
                .getColumn("type");
        assertThat(classes, hasItem(TypeDescriptorMatcher.typeDescriptor(ClassType.class)));
        assertThat(classes, hasItem(TypeDescriptorMatcher.typeDescriptor(SubClassType.class)));
        store.commitTransaction();
    }

}