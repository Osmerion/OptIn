/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.osmerion.optin.RequiresOptIn;

/**
 * TODO doc
 *
 * @param fqMarkerName
 * @param message
 * @param level
 *
 * @author  Leon Linhart
 */
public record RequiredOptionality(
    String fqMarkerName,
    String message,
    RequiresOptIn.Level level
) {}