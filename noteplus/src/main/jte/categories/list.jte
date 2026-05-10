@import org.noteplus.noteplus.dto.response.CategoryResponse
@import java.util.List

@param List<CategoryResponse> categories

<!DOCTYPE html>
<html lang="nl">
<head>
    <meta charset="UTF-8">
    <title>Categorieën — NotePlus</title>
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
            max-width: 700px;
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
        td { padding: 14px 28px; font-size: 14px; color: #1a1a1a; border-bottom: 1px solid #f3f4f6; }
        tr:last-child td { border-bottom: none; }
        .badge {
            display: inline-block;
            padding: 2px 10px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 500;
            background: #e0e7ff;
            color: #3730a3;
        }
        .empty { padding: 40px 28px; text-align: center; color: #9ca3af; font-size: 14px; }
    </style>
</head>
<body>
<div class="card">
    <div class="card-header">
        <h1>Categorieën</h1>
        <a class="btn" href="/categories/new">+ Nieuwe categorie</a>
    </div>

    @if(categories.isEmpty())
        <div class="empty">Nog geen categorieën aangemaakt.</div>
    @else
        <table>
            <thead>
                <tr>
                    <th>Titel</th>
                    <th>Kleur</th>
                    <th>Status</th>
                    <th>Parent</th>
                </tr>
            </thead>
            <tbody>
                @for(CategoryResponse cat : categories)
                    <tr>
                        <td>${cat.title()}</td>
                        <td>
                            @if(cat.color() != null)
                                <span class="badge">${cat.color()}</span>
                            @endif
                        </td>
                        <td>${cat.status() != null ? cat.status() : "—"}</td>
                        <td>${cat.parentTitle() != null ? cat.parentTitle() : "—"}</td>
                    </tr>
                @endfor
            </tbody>
        </table>
    @endif
</div>
</body>
</html>
