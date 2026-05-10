@import org.noteplus.noteplus.entity.User
@import java.util.List

@param List<User> users
@param String error

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Learning Path — NotePlus</title>
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
            max-width: 560px;
            margin: 0 auto;
            padding: 32px;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
        }
        h1 { font-size: 22px; margin: 0 0 24px; color: #1a1a1a; }
        label {
            display: block;
            font-size: 14px;
            font-weight: 600;
            color: #333;
            margin-top: 16px;
            margin-bottom: 4px;
        }
        input, select, textarea {
            width: 100%;
            padding: 8px 12px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 14px;
            color: #1a1a1a;
            font-family: inherit;
        }
        input:focus, select:focus, textarea:focus {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
        }
        textarea { min-height: 100px; resize: vertical; }
        .error {
            background: #fef2f2;
            color: #dc2626;
            padding: 10px 14px;
            border-radius: 6px;
            font-size: 14px;
            margin-bottom: 16px;
        }
        .hint { font-size: 12px; color: #9ca3af; margin-top: 4px; }
        .actions { display: flex; gap: 12px; margin-top: 28px; }
        button[type=submit] {
            padding: 10px 24px;
            background: #2563eb;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
        }
        button[type=submit]:hover { background: #1d4ed8; }
        a.cancel {
            padding: 10px 24px;
            color: #6b7280;
            text-decoration: none;
            font-size: 14px;
            line-height: 1.6;
        }
    </style>
</head>
<body>
<div class="card">
    <h1>Create Learning Path</h1>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    <form method="post" action="/learning-paths">
        <label for="title">Title *</label>
        <input type="text" id="title" name="title" required maxlength="255"
               placeholder="e.g. Java Backend Development">

        <label for="description">Description</label>
        <textarea id="description" name="description"
                  placeholder="Describe the goal of this learning path..."></textarea>

        <label for="studentId">Assign to Student *</label>
        <select id="studentId" name="studentId" required>
            <option value="">— select a student —</option>
            @for(User user : users)
                <option value="${user.getId()}">${user.getUsername()} (${user.getEmail()})</option>
            @endfor
        </select>
        <p class="hint">The student this learning path is designed for.</p>

        <label for="coachId">Assign Coach *</label>
        <select id="coachId" name="coachId" required>
            <option value="">— select a coach —</option>
            @for(User user : users)
                <option value="${user.getId()}">${user.getUsername()} (${user.getEmail()})</option>
            @endfor
        </select>
        <p class="hint">The coach responsible for guiding this learning path.</p>

        <div class="actions">
            <button type="submit">Create Path</button>
            <a class="cancel" href="/learning-paths">Cancel</a>
        </div>
    </form>
</div>
</body>
</html>
