package pers.z950.common.api

/**
 * handler need environment, may call async function
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION, AnnotationTarget.LOCAL_VARIABLE)
annotation class EnvironmentInit(val of: String = "")

/**
 * handler must init router
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.EXPRESSION)
annotation class RouterInit

/**
 * [point] is a path begin at root. "/" means mount at root.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class MountPoint(val point: String)

/**
 * route not found
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
annotation class NotFound

/**
 * router error
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
annotation class RouterError

/**
 * [cause] of error
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION, AnnotationTarget.LOCAL_VARIABLE)
annotation class Error(val code: Int, val cause: String)

/**
 * [status] of success, "ok" if not special
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
annotation class Success(val status: String = "ok")

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class SubResource(val name: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class RequestObject

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Controller
