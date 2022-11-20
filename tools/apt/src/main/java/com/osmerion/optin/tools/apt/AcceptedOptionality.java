/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import javax.lang.model.element.Element;

public record AcceptedOptionality(
    String fqMarkerName,
    Element binder
) {

}