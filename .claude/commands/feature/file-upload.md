---
description: Implementeert de file upload en download endpoints voor FileAttachment. Dit is een verplichte kernfunctionaliteit per schoolopdracht. Geen argumenten nodig.
allowed-tools: Read, Write, Glob, Grep
---

# File Upload & Download Feature

Dit is een **verplichte kernfunctionaliteit** (schoolopdracht eis).
FileAttachment heeft al een one-to-one relatie met Reference — nu implementeren we de endpoints.

## Stap 1 — Lees de bestaande entiteiten

Lees:
- `src/main/java/org/noteplus/noteplus/entity/FileAttachment.java`
- `src/main/java/org/noteplus/noteplus/entity/Reference.java`
- `src/main/java/org/noteplus/noteplus/repository/FileAttachmentRepository.java`

## Stap 2 — FileStorageService aanmaken

`src/main/java/org/noteplus/noteplus/service/FileStorageService.java`

```java
public interface FileStorageService {
    String store(MultipartFile file);
    Resource load(String filename);
    void delete(String filename);
}
```

`src/main/java/org/noteplus/noteplus/service/impl/FileStorageServiceImpl.java`

```java
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path uploadDir = Paths.get("uploads");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(uploadDir);
    }

    @Override
    public String store(MultipartFile file) {
        String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + filename, e);
        }
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = uploadDir.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new ResourceNotFoundException("File not found: " + filename);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }
}
```

## Stap 3 — ReferenceController endpoints toevoegen

```java
// Upload
@PostMapping("/{id}/attachment")
@Operation(summary = "Upload een bestand bij een reference")
@ApiResponse(responseCode = "201", description = "Bestand geüpload")
public ResponseEntity<FileAttachmentResponse> uploadFile(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(referenceService.uploadAttachment(id, file, auth.getName()));
}

// Download
@GetMapping("/{id}/attachment")
@Operation(summary = "Download het bestand van een reference")
public ResponseEntity<Resource> downloadFile(
        @PathVariable Long id,
        Authentication auth) {
    var result = referenceService.downloadAttachment(id, auth.getName());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
        .contentType(MediaType.parseMediaType(result.contentType()))
        .body(result.resource());
}
```

## Stap 4 — Controleer

- [ ] Upload slaat bestand op in `/uploads/` directory
- [ ] Filename wordt opgeslagen in `FileAttachment.filename`
- [ ] Download leest bestand terug en stuurt als stream
- [ ] Eigenaarcontrole aanwezig
- [ ] `uploads/` staat in `.gitignore`

Voeg toe aan `.gitignore`:
```
uploads/
```
