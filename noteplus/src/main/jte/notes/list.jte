@import org.noteplus.noteplus.dto.response.NoteResponse
@import java.util.List

@param List<NoteResponse> notes

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>My Notes — NotePlus</title>
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
            max-width: 760px;
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
        table { width: 100%; border-collapse: collapse; }
        th {
            text-align: left;
            padding: 12px 28px;
            font-size: 12px;
            font-weight: 600;
            color: #6b7280;
            text-transform: uppercase;
            letter-spacing: .05em;
            border-bottom: 1px solid #e5e7eb;
        }
        td {
            padding: 14px 28px;
            font-size: 14px;
            color: #1a1a1a;
            border-bottom: 1px solid #f3f4f6;
            vertical-align: top;
        }
        tr:last-child td { border-bottom: none; }
        .preview {
            color: #6b7280;
            font-size: 13px;
            margin-top: 2px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 360px;
        }
        .badge {
            display: inline-block;
            padding: 2px 10px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 500;
            background: #e0e7ff;
            color: #3730a3;
        }
        .date { color: #9ca3af; font-size: 12px; }
        .empty {
            padding: 48px 28px;
            text-align: center;
            color: #9ca3af;
            font-size: 14px;
        }
    </style>
</head>
<body>
<div class="card">
    <div class="card-header">
        <h1>My Notes</h1>
        <a class="btn" href="/notes/new">+ New Note</a>
    </div>

    @if(notes.isEmpty())
        <div class="empty">You have no notes yet. Create your first note!</div>
    @else
        <table>
            <thead>
                <tr>
                    <th>Title</th>
                    <th>Category</th>
                    <th>Created</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                @for(NoteResponse note : notes)
                    <tr>
                        <td>
                            <strong>${note.title()}</strong>
                            <div class="preview">${note.content()}</div>
                        </td>
                        <td>
                            @if(note.categoryTitle() != null)
                                <span class="badge">${note.categoryTitle()}</span>
                            @else
                                —
                            @endif
                        </td>
                        <td class="date">
                            ${note.createdAt() != null ? note.createdAt().toLocalDate().toString() : "—"}
                        </td>
                        <td>
                            <a href="/notes/${note.id()}/edit" style="font-size:13px;color:#2563eb;text-decoration:none;">
                                Edit &amp; References →
                            </a>
                        </td>
                    </tr>
                @endfor
            </tbody>
        </table>
    @endif
</div>
</body>
</html>
