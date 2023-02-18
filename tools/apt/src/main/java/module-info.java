/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
import com.osmerion.optin.tools.apt.OptInProcessor;

import javax.annotation.processing.Processor;

/**
 * Defines an annotation processor to validate opt-in marker annotations and
 * their usages.
 */
module com.osmerion.optin.tools.apt {

    requires com.osmerion.optin;

    requires java.compiler;
    requires jdk.compiler;

    requires static jsr305;

    provides Processor with OptInProcessor;

}