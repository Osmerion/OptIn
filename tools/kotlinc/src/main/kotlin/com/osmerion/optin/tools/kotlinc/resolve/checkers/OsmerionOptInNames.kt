/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.resolve.checkers

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object OsmerionOptInNames {

    val REQUIRES_OPT_IN_FQ_NAME = FqName("com.osmerion.optin.RequiresOptIn")
    val REQUIRES_OPT_IN_CLASS_ID = ClassId.topLevel(REQUIRES_OPT_IN_FQ_NAME)

    val OPT_IN_FQ_NAME = FqName("com.osmerion.optin.OptIn")
    val OPT_IN_CLASS_ID = ClassId.topLevel(OPT_IN_FQ_NAME)

    val REQUIRES_OPT_IN_FQ_NAMES = setOf(*OptInNames.REQUIRES_OPT_IN_FQ_NAMES.toTypedArray(), REQUIRES_OPT_IN_FQ_NAME)
    val REQUIRES_OPT_IN_CLASS_IDS = setOf(OptInNames.REQUIRES_OPT_IN_CLASS_ID, REQUIRES_OPT_IN_CLASS_ID)

    val OPT_IN_FQ_NAMES = setOf(*OptInNames.OPT_IN_FQ_NAMES.toTypedArray(), OPT_IN_FQ_NAME)
    val OPT_IN_CLASS_IDS = setOf(OptInNames.OPT_IN_CLASS_ID, OPT_IN_CLASS_ID)

}