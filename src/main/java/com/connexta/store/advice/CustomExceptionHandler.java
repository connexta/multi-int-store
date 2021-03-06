/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.store.advice;

import com.connexta.store.exceptions.common.DetailedErrorAttributes;
import java.util.Map;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
@AllArgsConstructor
public class CustomExceptionHandler {

  @NotNull private final DetailedErrorAttributes detailedErrorAttributes;

  @ExceptionHandler(ValidationException.class)
  public final ResponseEntity<Object> handleConstraintViolationException(
      final ValidationException e, final WebRequest request) {
    final HttpStatus status = HttpStatus.BAD_REQUEST;
    log.warn("Request is invalid. Returning {}.", status, e);
    return getErrorResponse(request, status);
  }

  @ExceptionHandler(UnsupportedOperationException.class)
  public final ResponseEntity<Object> handleUnsupportedOperationException(
      final UnsupportedOperationException e, final WebRequest request) {
    final HttpStatus status = HttpStatus.NOT_IMPLEMENTED;
    log.warn("Request is not supported. Returning {}.", status, e);
    return getErrorResponse(request, status);
  }

  private ResponseEntity<Object> getErrorResponse(
      @NotNull final WebRequest request, @NotNull final HttpStatus status) {
    final Map<String, Object> errorAttributes =
        detailedErrorAttributes.getErrorAttributes(request, false);
    errorAttributes.put("status", status.value());
    errorAttributes.put("error", status.getReasonPhrase());
    errorAttributes.put(
        "path",
        request.getAttribute(
            "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping",
            RequestAttributes.SCOPE_REQUEST));
    return ResponseEntity.status(status).body(errorAttributes);
  }
}
