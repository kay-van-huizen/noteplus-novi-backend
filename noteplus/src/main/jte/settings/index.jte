@param String error
@param String success

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Settings — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            padding: 40px 16px;
        }
        .container { max-width: 480px; margin: 0 auto; }
        h1 { font-size: 22px; color: #1a1a1a; margin-bottom: 24px; }
        .card {
            background: white;
            padding: 28px 32px;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
        }
        h2 { font-size: 16px; margin: 0 0 18px; color: #374151; }
        label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #555;
            margin-top: 14px;
            margin-bottom: 3px;
        }
        input {
            width: 100%;
            padding: 9px 12px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 14px;
        }
        input:focus {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
        }
        .error {
            background: #fef2f2;
            color: #dc2626;
            padding: 10px 14px;
            border-radius: 6px;
            font-size: 13px;
            margin-bottom: 14px;
        }
        .success {
            background: #f0fdf4;
            color: #16a34a;
            padding: 10px 14px;
            border-radius: 6px;
            font-size: 13px;
            margin-bottom: 14px;
        }
        .hint { font-size: 11px; color: #9ca3af; margin-top: 3px; }
        .actions { display: flex; gap: 10px; margin-top: 22px; }
        .btn-primary {
            padding: 10px 22px;
            background: #2563eb;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
        }
        .btn-primary:hover { background: #1d4ed8; }
        a.back { font-size: 13px; color: #6b7280; text-decoration: none; line-height: 2.4; }
    </style>
</head>
<body>
<div class="container">
    <h1>Settings</h1>

    <div class="card">
        <h2>Change Password</h2>

        @if(error != null)
            <div class="error">${error}</div>
        @endif

        @if(success != null)
            <div class="success">${success}</div>
        @endif

        <form method="post" action="/settings/password">
            <label for="currentPassword">Current password</label>
            <input type="password" id="currentPassword" name="currentPassword"
                   required autocomplete="current-password">

            <label for="newPassword">New password</label>
            <input type="password" id="newPassword" name="newPassword"
                   required minlength="8" autocomplete="new-password">
            <p class="hint">At least 8 characters</p>

            <label for="confirmPassword">Confirm new password</label>
            <input type="password" id="confirmPassword" name="confirmPassword"
                   required minlength="8" autocomplete="new-password">

            <div class="actions">
                <button class="btn-primary" type="submit">Change Password</button>
                <a class="back" href="/notes">← Back</a>
            </div>
        </form>
    </div>
</div>
</body>
</html>
