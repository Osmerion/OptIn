/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
import com.osmerion.optin.tools.apt.OptInProcessor;

import javax.annotation.processing.Processor;

module com.osmerion.optin.tools.apt {

    requires com.osmerion.optin;

    requires java.compiler;
    requires jdk.compiler;

    provides Processor with OptInProcessor;

}