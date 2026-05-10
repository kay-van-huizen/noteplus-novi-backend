@import org.noteplus.noteplus.entity.CategoryColor
@import org.noteplus.noteplus.entity.CategoryStatus
@import org.noteplus.noteplus.dto.response.CategoryResponse
@import java.util.List

@param List<CategoryResponse> parentOptions
@param String error

<!DOCTYPE html>
<html lang="nl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Categorie aanmaken — NotePlus</title>
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
            max-width: 480px;
            margin: 0 auto;
            padding: 32px;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
        }
        h1 { font-size: 22px; margin: 0 0 24px; color: #1a1a1a; }
        label { display: block; font-size: 14px; font-weight: 600; color: #333; margin-top: 16px; margin-bottom: 4px; }
        input, select, textarea {
            width: 100%;
            padding: 8px 12px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 14px;
            color: #1a1a1a;
        }
        textarea { min-height: 80px; resize: vertical; }
        input:focus, select:focus, textarea:focus {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
        }
        .error {
            background: #fef2f2;
            color: #dc2626;
            padding: 10px 14px;
            border-radius: 6px;
            font-size: 14px;
            margin-bottom: 16px;
        }
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
    <h1>Categorie aanmaken</h1>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    <form method="post" action="/categories">
        <label for="title">Titel *</label>
        <input type="text" id="title" name="title" required maxlength="100" placeholder="Bijv. Wiskunde">

        <label for="description">Omschrijving</label>
        <textarea id="description" name="description" placeholder="Optionele beschrijving"></textarea>

        <label for="color">Kleur</label>
        <select id="color" name="color">
            @for(CategoryColor c : CategoryColor.values())
                <option value="${c.name()}">${c.name()}</option>
            @endfor
        </select>

        <label for="status">Status</label>
        <select id="status" name="status">
            @for(CategoryStatus s : CategoryStatus.values())
                <option value="${s.name()}">${s.name()}</option>
            @endfor
        </select>

        <label for="parentId">Bovenliggende categorie (optioneel)</label>
        <select id="parentId" name="parentId">
            <option value="">— geen parent —</option>
            @for(CategoryResponse parent : parentOptions)
                <option value="${parent.id()}">${parent.title()}</option>
            @endfor
        </select>

        <div class="actions">
            <button type="submit">Aanmaken</button>
            <a class="cancel" href="/categories">Annuleren</a>
        </div>
    </form>
</div>
</body>
</html>
