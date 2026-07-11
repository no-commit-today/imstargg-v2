package com.imstargg.core.api.error

import com.example.template.core.error.ErrorLogger
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Clock
import java.time.LocalDateTime

@RestControllerAdvice
class ApiExceptionHandler(
    private val clock: Clock,
) : ResponseEntityExceptionHandler() {
    private val errorTypeKey = "errorType"
    private val timeStampKey: String = "timestamp"
    private val fieldsKey: String = "fields"
    private val errorLogger = ErrorLogger.of(LoggerFactory.getLogger(javaClass))

    @ExceptionHandler(CoreException::class)
    fun handleCoreException(ex: CoreException): ResponseEntity<ProblemDetail> {
        errorLogger.log(ex)
        return ResponseEntity
            .status(ex.errorType.status)
            .body(createProblemDetail(ex.errorType))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ProblemDetail> {
        errorLogger.log(ex, ErrorType.INVALID_REQUEST)
        val problemDetail = createProblemDetail(ErrorType.INVALID_REQUEST)
        val invalidFields =
            ex.constraintViolations.map { violation ->
                FieldErrorResponse(
                    field = violation.propertyPath.toString(),
                    value = violation.invalidValue,
                    reason = violation.message,
                )
            }
        problemDetail.setProperty(fieldsKey, invalidFields)
        return ResponseEntity
            .status(ErrorType.INVALID_REQUEST.status)
            .body(problemDetail)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ProblemDetail> {
        errorLogger.log(ex)
        val errorType = ErrorType.DEFAULT_ERROR
        return ResponseEntity
            .status(errorType.status)
            .body(createProblemDetail(errorType))
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val invalidFields =
            ex.valueResults.flatMap { result ->
                result.resolvableErrors.map { error ->
                    FieldErrorResponse(
                        field = result.methodParameter.parameterName,
                        value = result.argument,
                        reason = error.defaultMessage,
                    )
                }
            } +
                ex.beanResults.flatMap { result ->
                    result.fieldErrors.map { fieldError ->
                        FieldErrorResponse(
                            field = fieldError.field,
                            value = fieldError.rejectedValue,
                            reason = fieldError.defaultMessage,
                        )
                    }
                }

        val problemDetail = createProblemDetail(ErrorType.INVALID_REQUEST)
        problemDetail.setProperty(fieldsKey, invalidFields)
        return createResponseEntity(problemDetail, headers, status, request)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val invalidFields = mutableListOf<FieldErrorResponse>()

        ex.bindingResult.fieldErrors.forEach { fieldError ->
            invalidFields.add(
                FieldErrorResponse(
                    field = fieldError.field,
                    value = fieldError.rejectedValue,
                    reason = fieldError.defaultMessage,
                ),
            )
        }

        ex.bindingResult.globalErrors.forEach { globalError ->
            invalidFields.add(
                FieldErrorResponse(
                    field = globalError.objectName,
                    value = null,
                    reason = globalError.defaultMessage,
                ),
            )
        }

        val problemDetail = createProblemDetail(ErrorType.INVALID_REQUEST)
        problemDetail.setProperty(fieldsKey, invalidFields)

        return createResponseEntity(problemDetail, headers, status, request)
    }

    private fun createProblemDetail(errorType: ErrorType): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(errorType.status, errorType.message)
        problemDetail.setProperty(timeStampKey, LocalDateTime.now(clock))
        problemDetail.setProperty(errorTypeKey, errorType)
        return problemDetail
    }

    override fun createProblemDetail(
        ex: Exception,
        status: HttpStatusCode,
        defaultDetail: String,
        detailMessageCode: String?,
        detailMessageArguments: Array<Any>?,
        request: WebRequest,
    ): ProblemDetail {
        val problemDetail =
            super.createProblemDetail(
                ex,
                status,
                defaultDetail,
                detailMessageCode,
                detailMessageArguments,
                request,
            )
        problemDetail.setProperty(timeStampKey, LocalDateTime.now(clock))
        return problemDetail
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        logger.debug("handleExceptionInternal", ex)
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }
}
