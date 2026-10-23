package com.zed.nullshell;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Unified Navigation & Sessions Drawer: Quick-switch between Workspace Apps (Terminal,
 * Code Editor, Files, Browser, Network, Desktop VNC, Settings) and manages all active terminal sessions.
 */
final class SessionsPanel extends LinearLayout {
    interface Host {
        void onSelect(KaliSession s);
        void onRename(KaliSession s);
        void onClose(KaliSession s);
        void onNew();
        void onNavigate(String target);
    }

    private final Host host;
    private final LinearLayout sessionsList;
    private final TextView sessionsCountBadge;
    private final TextView terminalSessionBadge;

    SessionsPanel(Context c, Host host) {
        super(c);
        this.host = host;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.SURFACE);
        setClickable(true);                 // swallow touches so the terminal below does not react

        // --- Brand Header ---
        LinearLayout header = new LinearLayout(c);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(c, 16), Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 10));

        // Terminal icon pill
        TextView badgeIcon = new TextView(c);
        badgeIcon.setText(">_");
        badgeIcon.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        badgeIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        badgeIcon.setTextColor(Ui.ON_ACCENT);
        badgeIcon.setBackground(Ui.rounded(c, Ui.ACCENT, 8));
        badgeIcon.setPadding(Ui.dp(c, 7), Ui.dp(c, 3), Ui.dp(c, 7), Ui.dp(c, 3));
        header.addView(badgeIcon);

        LinearLayout titleCol = new LinearLayout(c);
        titleCol.setOrientation(VERTICAL);
        titleCol.setPadding(Ui.dp(c, 10), 0, 0, 0);

        TextView title = new TextView(c);
        title.setText("RawDog");
        title.setTextColor(Ui.TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        titleCol.addView(title);

        TextView subtitle = new TextView(c);
        subtitle.setText("Terminal & Linux Workspace");
        subtitle.setTextColor(Ui.DIM);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        titleCol.addView(subtitle);

        header.addView(titleCol, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnSettings = Ui.iconButton(c, "⚙");
        btnSettings.setTextColor(Ui.DIM);
        btnSettings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        btnSettings.setOnClickListener(v -> host.onNavigate("settings"));
        header.addView(btnSettings);

        addView(header, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View headerLine = new View(c);
        headerLine.setBackgroundColor(Ui.LINE);
        addView(headerLine, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // --- Scrollable Body ---
        ScrollView scroll = new ScrollView(c);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout content = new LinearLayout(c);
        content.setOrientation(VERTICAL);
        content.setPadding(Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 16));

        // Section: Workspace Tools
        content.addView(sectionHeader(c, "WORKSPACE APPS", null));

        // 1. Terminal (Active screen)
        terminalSessionBadge = badgeView(c, "Active");
        content.addView(navRow(c, ">_", "Terminal", "CLI shell & sessions", true, terminalSessionBadge, () -> host.onNavigate("terminal")));

        // 2. Code Editor
        content.addView(navRow(c, "</>", "Code Editor", "Sora editor & LSP", false, null, () -> host.onNavigate("editor")));

        // 3. File Manager
        content.addView(navRow(c, "📁", "Files", "Kali & Android storage", false, null, () -> host.onNavigate("files")));

        // 4. Web Browser
        content.addView(navRow(c, "🌐", "Browser", "Web & proxy tools", false, null, () -> host.onNavigate("browser")));

        // 5. Network Scanner
        content.addView(navRow(c, "⚡", "Network", "LAN scanner & ports", false, null, () -> host.onNavigate("network")));

        // 6. Desktop VNC
        content.addView(navRow(c, "🖥", "Desktop VNC", "X11 graphical environment", false, null, () -> host.onNavigate("vnc")));

        // Section Divider
        View div = new View(c);
        div.setBackgroundColor(Ui.LINE);
        LayoutParams divLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divLp.setMargins(Ui.dp(c, 6), Ui.dp(c, 12), Ui.dp(c, 6), Ui.dp(c, 6));
        content.addView(div, divLp);

        // Section: Terminal Sessions
        sessionsCountBadge = badgeView(c, "0");
        sessionsCountBadge.setTextColor(Ui.DIM);
        sessionsCountBadge.setBackground(Ui.rounded(c, Ui.SURFACE2, 6));

        TextView btnAddSession = Ui.iconButton(c, "+");
        btnAddSession.setTextColor(Ui.ACCENT);
        btnAddSession.setOnClickListener(v -> host.onNew());

        LinearLayout sessionsHead = new LinearLayout(c);
        sessionsHead.setOrientation(HORIZONTAL);
        sessionsHead.setGravity(Gravity.CENTER_VERTICAL);
        sessionsHead.setPadding(Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 4));

        TextView sTitle = new TextView(c);
        sTitle.setText("SESSIONS");
        sTitle.setTextColor(Ui.DIM);
        sTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        sTitle.setLetterSpacing(0.08f);
        sessionsHead.addView(sTitle);

        sessionsHead.addView(sessionsCountBadge);
        sessionsHead.addView(new View(c), new LayoutParams(0, 1, 1f));
        sessionsHead.addView(btnAddSession);
        content.addView(sessionsHead);

        // Sessions List Container
        sessionsList = new LinearLayout(c);
        sessionsList.setOrientation(VERTICAL);
        content.addView(sessionsList);

        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Footer Settings Link
        View footLine = new View(c);
        footLine.setBackgroundColor(Ui.LINE);
        addView(footLine, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout footer = new LinearLayout(c);
        footer.setOrientation(HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(Ui.dp(c, 14), Ui.dp(c, 10), Ui.dp(c, 14), Ui.dp(c, 10));
        footer.setBackground(Ui.ripple(c, 0, 0));
        footer.setOnClickListener(v -> host.onNavigate("settings"));

        TextView footIcon = new TextView(c);
        footIcon.setText("⚙");
        footIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        footIcon.setTextColor(Ui.ACCENT);
        footIcon.setPadding(0, 0, Ui.dp(c, 10), 0);
        footer.addView(footIcon);

        TextView footText = new TextView(c);
        footText.setText("Settings & Appearance");
        footText.setTextColor(Ui.TEXT);
        footText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        footer.addView(footText);

        addView(footer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private TextView sectionHeader(Context c, String text, View extra) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(Ui.DIM);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setLetterSpacing(0.08f);
        t.setPadding(Ui.dp(c, 8), Ui.dp(c, 6), 0, Ui.dp(c, 4));
        return t;
    }

    private TextView badgeView(Context c, String text) {
        TextView b = new TextView(c);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        b.setTextColor(Ui.ACCENT);
        b.setBackground(Ui.rounded(c, Ui.SURFACE2, 6));
        b.setPadding(Ui.dp(c, 6), Ui.dp(c, 2), Ui.dp(c, 6), Ui.dp(c, 2));
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(c, 6);
        b.setLayoutParams(lp);
        return b;
    }

    private View navRow(Context c, String iconStr, String titleStr, String descStr, boolean active, View badge, Runnable onClick) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(c, 10), Ui.dp(c, 7), Ui.dp(c, 10), Ui.dp(c, 7));
        row.setBackground(Ui.ripple(c, active ? Ui.SURFACE2 : 0, 10));
        row.setClickable(true);
        row.setOnClickListener(v -> onClick.run());

        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(c, 2);
        row.setLayoutParams(lp);

        TextView icon = new TextView(c);
        icon.setText(iconStr);
        icon.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        icon.setTextColor(active ? Ui.ACCENT : Ui.DIM);
        icon.setGravity(Gravity.CENTER);
        icon.setLayoutParams(new LayoutParams(Ui.dp(c, 26), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(icon);

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(VERTICAL);
        texts.setPadding(Ui.dp(c, 8), 0, 0, 0);

        TextView name = new TextView(c);
        name.setText(titleStr);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(active ? Ui.ACCENT : Ui.TEXT);
        name.setTypeface(Typeface.create("sans-serif-medium", active ? Typeface.BOLD : Typeface.NORMAL));
        texts.addView(name);

        TextView sub = new TextView(c);
        sub.setText(descStr);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sub.setTextColor(Ui.DIM);
        texts.addView(sub);

        row.addView(texts, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (badge != null) {
            row.addView(badge);
        }

        return row;
    }

    void refresh(List<KaliSession> all, KaliSession active) {
        Context c = getContext();
        sessionsList.removeAllViews();

        int liveCount = 0;
        for (KaliSession s : all) {
            if (!s.isConsole()) liveCount++;
            sessionsList.addView(sessionRow(c, s, s == active));
        }

        sessionsCountBadge.setText(String.valueOf(liveCount));
        terminalSessionBadge.setText(liveCount > 0 ? liveCount + " open" : "Idle");
    }

    private View sessionRow(Context c, KaliSession s, boolean active) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(c, 10), 0, Ui.dp(c, 2), 0);
        row.setBackground(Ui.ripple(c, active ? Ui.SURFACE2 : 0, 10));
        row.setClickable(true);
        row.setOnClickListener(v -> host.onSelect(s));
        if (!s.isConsole()) row.setOnLongClickListener(v -> { host.onRename(s); return true; });

        LayoutParams rp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 50));
        rp.bottomMargin = Ui.dp(c, 2);
        row.setLayoutParams(rp);

        TextView dot = new TextView(c);
        dot.setText("●");
        dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        dot.setTextColor(s.isConsole() ? Ui.HINT : s.isAlive() ? Ui.ACCENT : Ui.DIM);
        dot.setPadding(0, 0, Ui.dp(c, 8), 0);
        row.addView(dot);

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(VERTICAL);
        TextView name = new TextView(c);
        name.setText(s.title());
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(active ? Ui.TEXT : s.isEnded() ? Ui.DIM : Ui.TEXT);
        name.setTypeface(Typeface.create("sans-serif-medium", active ? Typeface.BOLD : Typeface.NORMAL));
        texts.addView(name);

        TextView sub = new TextView(c);
        sub.setText(state(s));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sub.setTextColor(Ui.DIM);
        sub.setSingleLine(true);
        texts.addView(sub);
        row.addView(texts, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (!s.isConsole()) {
            TextView rename = Ui.iconButton(c, "✎");
            rename.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            rename.setTextColor(Ui.DIM);
            rename.setOnClickListener(v -> host.onRename(s));
            row.addView(rename);
            TextView close = Ui.iconButton(c, "×");
            close.setTextColor(Ui.DIM);
            close.setOnClickListener(v -> host.onClose(s));
            row.addView(close);
        }
        return row;
    }

    /** One-line state under a session's name. */
    static String state(KaliSession s) {
        if (s.isConsole()) return "Log & installer";
        if (s.isAlive()) return "Running";
        if (s.isEnded()) return "Ended";
        return "Starting…";
    }
}
