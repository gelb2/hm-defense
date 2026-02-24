package fr.mesabloo.heavymachdefense

/**
 * `true` if the build should render debug information, else `false`.
 * Active for both `dev` and `inhouse` builds.
 */
const val DEBUG: Boolean = !BuildConfig.RELEASE

/**
 * `true` only for `dev` builds (not `inhouse`, not `release`).
 */
const val DEV: Boolean = !BuildConfig.RELEASE && !BuildConfig.INHOUSE

/**
 * `true` only for `inhouse` builds.
 */
const val INHOUSE: Boolean = BuildConfig.INHOUSE

/**
 * Execute an action only when the constant [DEBUG] is `true` (dev + inhouse).
 */
inline fun ifDebug(action: () -> Unit) = if (DEBUG) action() else Unit

/**
 * Execute an action only for `dev` builds.
 */
inline fun ifDev(action: () -> Unit) = if (DEV) action() else Unit