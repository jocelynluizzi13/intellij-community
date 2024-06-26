// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.resolve;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode;
import org.jetbrains.kotlin.idea.base.test.TestRoot;
import org.jetbrains.kotlin.idea.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.idea.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

/**
 * This class is generated by {@link org.jetbrains.kotlin.testGenerator.generator.TestGenerator}.
 * DO NOT MODIFY MANUALLY.
 */
@SuppressWarnings("all")
@TestRoot("idea/tests")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("testData/resolve/referenceInJava/binaryAndSource")
public class ReferenceToCompiledKotlinResolveInJavaTestGenerated extends AbstractReferenceToCompiledKotlinResolveInJavaTest {
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public final KotlinPluginMode getPluginMode() {
        return KotlinPluginMode.K1;
    }

    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("Class.java")
    public void testClass() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/Class.java");
    }

    @TestMetadata("ClassObjectField.java")
    public void testClassObjectField() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/ClassObjectField.java");
    }

    @TestMetadata("Constructor.java")
    public void testConstructor() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/Constructor.java");
    }

    @TestMetadata("EnumEntry.java")
    public void testEnumEntry() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/EnumEntry.java");
    }

    @TestMetadata("Field.java")
    public void testField() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/Field.java");
    }

    @TestMetadata("FileFacade.java")
    public void testFileFacade() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/FileFacade.java");
    }

    @TestMetadata("Getter.java")
    public void testGetter() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/Getter.java");
    }

    @TestMetadata("Method.java")
    public void testMethod() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/Method.java");
    }

    @TestMetadata("MethodOfDeeplyNested.java")
    public void testMethodOfDeeplyNested() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/MethodOfDeeplyNested.java");
    }

    @TestMetadata("MethodWithParameters.java")
    public void testMethodWithParameters() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/MethodWithParameters.java");
    }

    @TestMetadata("MultifileFacade1.java")
    public void testMultifileFacade1() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/MultifileFacade1.java");
    }

    @TestMetadata("MultifileFacade2.java")
    public void testMultifileFacade2() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/MultifileFacade2.java");
    }

    @TestMetadata("ObjectInstance.java")
    public void testObjectInstance() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/ObjectInstance.java");
    }

    @TestMetadata("PlatformStaticFun.java")
    public void testPlatformStaticFun() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/PlatformStaticFun.java");
    }

    @TestMetadata("SingleFileMultifileFacade.java")
    public void testSingleFileMultifileFacade() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/SingleFileMultifileFacade.java");
    }

    @TestMetadata("TopLevelFunction.java")
    public void testTopLevelFunction() throws Exception {
        runTest("testData/resolve/referenceInJava/binaryAndSource/TopLevelFunction.java");
    }
}
