@param String error
@param String passwordReset

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 16px;
        }
        .card {
            background: white;
            width: 100%;
            max-width: 400px;
            padding: 40px 36px;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .logo { font-size: 24px; font-weight: 700; color: #1a1a1a; margin-bottom: 6px; }
        .subtitle { font-size: 14px; color: #6b7280; margin-bottom: 28px; }
        label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #374151;
            margin-top: 16px;
            margin-bottom: 4px;
        }
        input {
            width: 100%;
            padding: 10px 12px;
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
            margin-bottom: 8px;
        }
        button {
            width: 100%;
            margin-top: 24px;
            padding: 11px;
            background: #2563eb;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
        }
        button:hover { background: #1d4ed8; }
        .footer { margin-top: 20px; text-align: center; font-size: 13px; color: #6b7280; }
        .footer a { color: #2563eb; text-decoration: none; }
        .forgot { display: block; text-align: right; font-size: 12px; color: #2563eb;
                  text-decoration: none; margin-top: 6px; }
    </style>
</head>
<body>
<div class="card">
    <div class="logo">NotePlus</div>
    <div class="subtitle">Sign in to your account</div>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    @if(passwordReset.equals("true"))
        <div style="background:#f0fdf4;color:#16a34a;padding:10px 14px;border-radius:6px;font-size:13px;margin-bottom:14px;">
            Password reset successfully. You can now sign in.
        </div>
    @endif

    <form method="post" action="/login">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" required autocomplete="username">

        <label for="password">Password</label>
        <input type="password" id="password" name="password" required autocomplete="current-password">
        <a class="forgot" href="/forgot-password">Forgot password?</a>

        <button type="submit">Sign in</button>
    </form>

    <div class="footer">
        Don't have an account? <a href="/register">Register</a>
    </div>
</div>
</body>
</html>
