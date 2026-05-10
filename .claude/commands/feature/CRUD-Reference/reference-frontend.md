---
description: Adds a References section to the Note edit view in JTE — allows adding text/URL references and uploading files within a note. Run only after reference-backend.md is complete and all Swagger tests pass.
allowed-tools: Read, Write, Bash(./mvnw compile:*), Glob, Grep
---

# Reference Frontend — NotePlus (JTE)

## What this adds

The reference frontend is NOT a separate page.
It extends the existing note views with a references section:

- `notes/list.jte` → shows reference count per note (badge)
- `notes/edit.jte` → new page: edit a note AND manage its references + file uploads

## Step 0 — Read context first

Before writing any code, read:

```
src/main/java/org/noteplus/noteplus/dto/response/ReferenceResponse.java
src/main/java/org/noteplus/noteplus/dto/response/FileAttachmentResponse.java
src/main/java/org/noteplus/noteplus/dto/response/NoteResponse.java
src/main/java/org/noteplus/noteplus/service/NoteService.java
src/main/java/org/noteplus/noteplus/service/ReferenceService.java
src/main/jte/notes/list.jte         ← update this
src/main/jte/notes/form.jte         ← read for style reference only, do not modify
src/main/resources/application.properties
```

## Step 1 — Add edit endpoint to NoteViewController

Read `NoteViewController.java` first to understand the existing methods.
Then add these two methods to the existing controller — do NOT replace the file, only ADD:

```java
// Show the edit page for a single note with its references
@GetMapping("/{id}/edit")
public String showEdit(@PathVariable Long id, Model model, Authentication auth) {
    try {
        NoteResponse note = noteService.getById(id, auth.getName());
        List<ReferenceResponse> references = referenceService.getAllForNote(id, auth.getName());
        model.addAttribute("note", note);
        model.addAttribute("references", references);
        model.addAttribute("error", null);
        return "notes/edit";
    } catch (Exception e) {
        return "redirect:/notes";
    }
}

// Handle note update from the edit form
@PostMapping("/{id}/edit")
public String handleUpdate(
        @PathVariable Long id,
        @ModelAttribute UpdateNoteRequest request,
        Authentication auth,
        Model model) {
    try {
        noteService.update(id, request, auth.getName());
        return "redirect:/notes/" + id + "/edit?saved=true";
    } catch (Exception e) {
        model.addAttribute("error", e.getMessage());
        // Reload note and references to re-render the page
        try {
            model.addAttribute("note", noteService.getById(id, auth.getName()));
            model.addAttribute("references", referenceService.getAllForNote(id, auth.getName()));
        } catch (Exception ignored) {}
        return "notes/edit";
    }
}
```

Add `ReferenceService` injection to `NoteViewController`:
```java
private final ReferenceService referenceService;
```

Also add the required imports for `ReferenceResponse`, `ReferenceService`, and `UpdateNoteRequest`.

## Step 2 — Update notes/list.jte

Read `notes/list.jte` first to understand the existing structure.
Find the table row for each note and add a reference count column.

Add a new `<th>References</th>` column header.
In the table row, add:
```html
<td>
    @if(note.referenceCount() != null && note.referenceCount() > 0)
        <span class="badge">${note.referenceCount()}</span>
    @else
        —
    @endif
</td>
```

IMPORTANT: Check if `NoteResponse` has a `referenceCount()` field.
If not, you have two options:
- Option A: Add `int referenceCount` to NoteResponse and populate it in NoteServiceImpl.toResponse()
- Option B: Skip the count column and just add an "Edit" link per row

Preferred option: Add an "Edit / Manage" link in the last column of each row:
```html
<td>
    <a href="/notes/${note.id()}/edit" style="font-size:13px;color:#2563eb;text-decoration:none;">
        Edit &amp; References →
    </a>
</td>
```

This is simpler and more useful than a count badge.

## Step 3 — Create notes/edit.jte

`src/main/jte/notes/edit.jte`:

This page has two sections:
1. Edit note title, content, category (top)
2. References list + add reference form + file upload (bottom)

```html
@import org.noteplus.noteplus.dto.response.NoteResponse
@import org.noteplus.noteplus.dto.response.ReferenceResponse
@import org.noteplus.noteplus.dto.response.FileAttachmentResponse
@import java.util.List

@param NoteResponse note
@param List<ReferenceResponse> references
@param String error
@param(default = "") String saved

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Note — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            padding: 40px 16px;
        }
        .container { max-width: 700px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; }
        .card {
            background: white;
            padding: 28px 32px;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
        }
        h2 { font-size: 18px; margin: 0 0 20px; color: #1a1a1a; }
        h3 { font-size: 15px; margin: 0 0 14px; color: #374151; }
        label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #555;
            margin-top: 14px;
            margin-bottom: 3px;
        }
        input, textarea {
            width: 100%;
            padding: 8px 12px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 14px;
            font-family: inherit;
        }
        textarea { min-height: 140px; resize: vertical; }
        input:focus, textarea:focus {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
        }
        .error { background: #fef2f2; color: #dc2626; padding: 10px 14px; border-radius: 6px; font-size: 14px; margin-bottom: 14px; }
        .success { background: #f0fdf4; color: #16a34a; padding: 10px 14px; border-radius: 6px; font-size: 14px; margin-bottom: 14px; }
        .actions { display: flex; gap: 10px; margin-top: 20px; }
        .btn-primary {
            padding: 9px 20px; background: #2563eb; color: white;
            border: none; border-radius: 6px; font-size: 14px; font-weight: 600; cursor: pointer;
        }
        .btn-primary:hover { background: #1d4ed8; }
        .btn-secondary {
            padding: 9px 20px; background: white; color: #6b7280;
            border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; cursor: pointer;
        }
        a.link-back { font-size: 13px; color: #6b7280; text-decoration: none; line-height: 2.2; }

        /* References section */
        .ref-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
        .ref-item {
            border: 1px solid #e5e7eb;
            border-radius: 6px;
            padding: 14px 16px;
        }
        .ref-title { font-size: 14px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
        .ref-desc { font-size: 13px; color: #6b7280; margin-bottom: 6px; }
        .ref-url a { font-size: 12px; color: #2563eb; word-break: break-all; }
        .ref-file { margin-top: 8px; padding: 8px; background: #f9fafb; border-radius: 4px; font-size: 12px; }
        .ref-file a { color: #2563eb; }
        .badge-file { background: #dcfce7; color: #16a34a; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; }
        .no-refs { font-size: 13px; color: #9ca3af; font-style: italic; margin-bottom: 16px; }
        .divider { border: none; border-top: 1px solid #f3f4f6; margin: 20px 0; }

        /* Upload form inside reference */
        .upload-form { display: flex; gap: 8px; margin-top: 8px; align-items: center; }
        .upload-form input[type=file] { padding: 4px; font-size: 12px; }
        .btn-sm {
            padding: 5px 12px; font-size: 12px; font-weight: 600;
            border: none; border-radius: 4px; cursor: pointer;
        }
        .btn-upload { background: #2563eb; color: white; }
        .btn-delete { background: #fee2e2; color: #dc2626; }
    </style>
</head>
<body>
<div class="container">

    <!--- Section 1: Edit note --->
    <div class="card">
        <h2>Edit Note</h2>

        @if(error != null)
            <div class="error">${error}</div>
        @endif

        @if(saved.equals("true"))
            <div class="success">Note saved successfully.</div>
        @endif

        <form method="post" action="/notes/${note.id()}/edit">
            <label for="title">Title *</label>
            <input type="text" id="title" name="title" required maxlength="255" value="${note.title()}">

            <label for="content">Content *</label>
            <textarea id="content" name="content" required>${note.content()}</textarea>

            <div class="actions">
                <button class="btn-primary" type="submit">Save Changes</button>
                <a class="link-back" href="/notes">← Back to notes</a>
            </div>
        </form>
    </div>

    <!--- Section 2: References --->
    <div class="card">
        <h2>References</h2>

        @if(references.isEmpty())
            <div class="no-refs">No references added yet.</div>
        @else
            <div class="ref-list">
                @for(ReferenceResponse ref : references)
                    <div class="ref-item">
                        <div class="ref-title">${ref.title()}</div>

                        @if(ref.description() != null && !ref.description().isEmpty())
                            <div class="ref-desc">${ref.description()}</div>
                        @endif

                        @if(ref.url() != null && !ref.url().isEmpty())
                            <div class="ref-url">
                                <a href="${ref.url()}" target="_blank" rel="noopener">${ref.url()}</a>
                            </div>
                        @endif

                        <!--- File section --->
                        @if(ref.fileAttachment() != null)
                            <div class="ref-file">
                                <span class="badge-file">FILE</span>
                                <a href="/api/notes/${note.id()}/references/${ref.id()}/attachment">
                                    ${ref.fileAttachment().originalFilename()}
                                </a>
                                — ${ref.fileAttachment().contentType()}
                                <form method="post"
                                      action="/api/notes/${note.id()}/references/${ref.id()}/attachment/delete"
                                      style="display:inline;">
                                    <button class="btn-sm btn-delete" type="submit">Remove file</button>
                                </form>
                            </div>
                        @else
                            <form class="upload-form"
                                  method="post"
                                  action="/api/notes/${note.id()}/references/${ref.id()}/attachment"
                                  enctype="multipart/form-data">
                                <input type="file" name="file">
                                <button class="btn-sm btn-upload" type="submit">Upload file</button>
                            </form>
                        @endif
                    </div>
                @endfor
            </div>
        @endif

        <hr class="divider">
        <h3>Add Reference</h3>

        <form method="post" action="/api/notes/${note.id()}/references">
            <label for="refTitle">Title *</label>
            <input type="text" id="refTitle" name="title" required maxlength="150"
                   placeholder="e.g. Spring Boot documentation">

            <label for="refDesc">Description (optional)</label>
            <textarea id="refDesc" name="description" style="min-height:80px;"
                      placeholder="Notes about this reference..."></textarea>

            <label for="refUrl">URL (optional)</label>
            <input type="url" id="refUrl" name="url" maxlength="500"
                   placeholder="https://...">

            <div class="actions">
                <button class="btn-primary" type="submit">Add Reference</button>
            </div>
        </form>
    </div>

</div>
</body>
</html>
```

## Step 4 — Important: file delete via HTML form

HTML forms only support GET and POST — not DELETE.
The file delete button in the template above uses a POST to a custom path:
`/api/notes/{noteId}/references/{referenceId}/attachment/delete`

Add this extra endpoint to `ReferenceController`:

```java
@PostMapping("/api/notes/{noteId}/references/{referenceId}/attachment/delete")
@Operation(summary = "Delete attachment via HTML form POST (browser workaround)")
public ResponseEntity<Void> deleteFileFromForm(
        @PathVariable Long noteId,
        @PathVariable Long referenceId,
        Authentication auth,
        jakarta.servlet.http.HttpServletResponse response) throws IOException {
    referenceService.deleteFile(referenceId, noteId, auth.getName());
    response.sendRedirect("/notes/" + noteId + "/edit");
    return ResponseEntity.noContent().build();
}
```

This is a browser compatibility workaround — HTML forms cannot send DELETE requests.
The REST DELETE endpoint remains available for API clients (Postman, Swagger).

## Step 5 — Add /notes/** to SecurityConfig

Verify that `/notes/**` is already permitted in SecurityConfig.
The edit page at `/notes/{id}/edit` must be accessible to authenticated users.
If the file upload form posts directly to `/api/...`, the API endpoints must also accept
requests from authenticated browser sessions — the JWT filter handles this via the session cookie
or the Authorization header. For form submissions from a browser (no JS), this may require
the form to pass a token somehow — for the school project, permitting `/api/notes/*/references/**`
in SecurityConfig is acceptable for simplicity.

Add to SecurityConfig permitted paths:
```java
"/api/notes/*/references/**"   // allow form submissions from edit page
```

## Step 6 — Browser test

Browser tests:
- http://localhost:8080/notes → list shows "Edit & References →" link per note
- Click "Edit & References →" → edit page loads with note content and empty references list
- Edit the title, click "Save Changes" → success message appears, title updated
- Fill in reference form (title + URL) → click "Add Reference" → page reloads with reference in list
- Fill in reference form (title + description, no URL) → add → shows description text in list
- Click "Upload file" for a reference → select a PDF → file appears with download link
- Click "Remove file" → file is removed, upload button reappears

If the file upload form does not submit correctly:
- Verify `enctype="multipart/form-data"` is on the upload form
- Verify `spring.servlet.multipart.enabled=true` is in application.properties
- Check the browser network tab for the actual request being sent
