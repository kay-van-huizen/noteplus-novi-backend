@import org.noteplus.noteplus.dto.response.LearningPathResponse
@import org.noteplus.noteplus.dto.response.NoteResponse
@import java.util.List

@param List<LearningPathResponse> paths

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Learning Paths — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            padding: 40px 16px;
        }
        .card {
            background: white;
            max-width: 800px;
            margin: 0 auto;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 24px 28px;
            border-bottom: 1px solid #e5e7eb;
        }
        h1 { font-size: 20px; margin: 0; color: #1a1a1a; }
        a.btn {
            padding: 8px 18px;
            background: #2563eb;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
        }
        .path-row {
            padding: 20px 28px;
            border-bottom: 1px solid #f3f4f6;
        }
        .path-row:last-child { border-bottom: none; }
        .path-title { font-size: 16px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
        .path-desc { font-size: 13px; color: #6b7280; margin-bottom: 10px; }
        .meta { display: flex; gap: 24px; font-size: 12px; color: #6b7280; margin-bottom: 10px; }
        .meta span strong { color: #374151; }
        .notes-label { font-size: 12px; font-weight: 600; color: #6b7280;
                       text-transform: uppercase; letter-spacing: .05em; margin-bottom: 6px; }
        .note-chips { display: flex; flex-wrap: wrap; gap: 6px; }
        .chip {
            display: inline-block;
            padding: 3px 10px;
            background: #e0e7ff;
            color: #3730a3;
            border-radius: 12px;
            font-size: 12px;
        }
        .no-notes { font-size: 12px; color: #d1d5db; font-style: italic; }
        .empty {
            padding: 48px 28px;
            text-align: center;
            color: #9ca3af;
            font-size: 14px;
        }
        .date { font-size: 11px; color: #d1d5db; margin-top: 8px; }
    </style>
</head>
<body>
<div class="card">
    <div class="card-header">
        <h1>Learning Paths</h1>
        <a class="btn" href="/learning-paths/new">+ New Path</a>
    </div>

    @if(paths.isEmpty())
        <div class="empty">No learning paths yet. Create your first one!</div>
    @else
        @for(LearningPathResponse path : paths)
            <div class="path-row">
                <div class="path-title">${path.title()}</div>

                @if(path.description() != null && !path.description().isEmpty())
                    <div class="path-desc">${path.description()}</div>
                @endif

                <div class="meta">
                    <span>Student: <strong>${path.studentUsername()}</strong></span>
                    <span>Coach: <strong>${path.coachUsername()}</strong></span>
                </div>

                <div class="notes-label">Notes (${path.notes().size()})</div>
                @if(path.notes().isEmpty())
                    <div class="no-notes">No notes linked yet.</div>
                @else
                    <div class="note-chips">
                        @for(NoteResponse note : path.notes())
                            <span class="chip">${note.title()}</span>
                        @endfor
                    </div>
                @endif

                <div class="date">Created: ${path.createdAt() != null ? path.createdAt().toLocalDate().toString() : "—"}</div>
            </div>
        @endfor
    @endif
</div>
</body>
</html>
