---
description: Creates GlobalExceptionHandler + missing exception classes for NotePlus.
allowed-tools: Read, Write, Glob
---

# GlobalExceptionHandler — NotePlus

Read all files in `src/main/java/org/noteplus/noteplus/exception/` first.

## Step 1 — Create missing exception classes

Create `exception/ResourceNotFoundException.java`:
```java
package org.noteplus.noteplus.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
```

Rename `exception/AccessDeniedException.java` to `exception/ForbiddenException.java`:
```java
package org.noteplus.noteplus.exception;
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
```

Update any class that imports or throws `AccessDeniedException` from this package to use `ForbiddenException` instead.

## Step 2 — Create ErrorResponse DTO

`dto/response/ErrorResponse.java`:
```java
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {}
```

## Step 3 — Create GlobalExceptionHandler

`exception/GlobalExceptionHandler.java`:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(409)
            .body(new ErrorResponse(409, "Conflict", ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse(404, "Not Found", ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(403)
            .body(new ErrorResponse(403, "Forbidden", ex.getMessage(), LocalDateTime.now()));
    }

    // Catches Spring Security's @PreAuthorize violations
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(403)
            .body(new ErrorResponse(403, "Forbidden", "Access denied", LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(400)
            .body(new ErrorResponse(400, "Bad Request", message, LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(500)
            .body(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred", LocalDateTime.now()));
    }
}
```

Also fix AuthController: change login's @ApiResponse from responseCode "403" to "401".