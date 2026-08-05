package com.finance.PaymentProcessing.exception;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(NotFoundException.class) ResponseEntity<Map<String,Object>> notFound(NotFoundException e){return error(HttpStatus.NOT_FOUND,e.getErrorCode(),e.getMessage());}
 @ExceptionHandler(BadRequestException.class) ResponseEntity<Map<String,Object>> bad(BadRequestException e){return error(HttpStatus.BAD_REQUEST,e.getErrorCode(),e.getMessage());}
 @ExceptionHandler(ConflictException.class) ResponseEntity<Map<String,Object>> conflict(ConflictException e){return error(HttpStatus.CONFLICT,e.getErrorCode(),e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e){String m=e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+": "+x.getDefaultMessage()).orElse("Invalid request");return error(HttpStatus.BAD_REQUEST,"VALIDATION_FAILED",m);}
 private ResponseEntity<Map<String,Object>> error(HttpStatus s,String c,String m){return ResponseEntity.status(s).body(Map.of("timestamp",Instant.now().toString(),"status",s.value(),"errorCode",c,"message",m));}
}
