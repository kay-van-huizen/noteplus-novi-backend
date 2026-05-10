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
        a.link-back { font-size: 13px; color: #6b7280; text-decoration: none; line-height: 2.2; }

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

                        @if(ref.fileAttachment() != null)
                            <div class="ref-file">
                                <span class="badge-file">FILE</span>
                                <a href="/api/notes/${note.id()}/references/${ref.id()}/attachment">
                                    ${ref.fileAttachment().fileName()}
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
