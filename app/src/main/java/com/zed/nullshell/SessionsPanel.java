package com.zed.nullshell;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Sessions Drawer: Lists all active and background terminal sessions, allows switching,
 * renaming, starting fresh sessions, and managing the workspace.
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

    SessionsPanel(Context c, Host host) {
        super(c);
        this.host = host;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.SURFACE);
        setClickable(true);

        // Header
        LinearLayout header = new LinearLayout(c);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(c, 16), Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 10));

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
        subtitle.setText("Terminal Sessions");
        subtitle.setTextColor(Ui.DIM);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        titleCol.addView(subtitle);

        header.addView(titleCol, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnAdd = Ui.iconButton(c, "+");
        btnAdd.setTextColor(Ui.ACCENT);
        btnAdd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        btnAdd.setOnClickListener(v -> host.onNew());
        header.addView(btnAdd);

        addView(header, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View headerLine = new View(c);
        headerLine.setBackgroundColor(Ui.LINE);
        addView(headerLine, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // Subheader with session count
        LinearLayout sessionsHead = new LinearLayout(c);
        sessionsHead.setOrientation(HORIZONTAL);
        sessionsHead.setGravity(Gravity.CENTER_VERTICAL);
        sessionsHead.setPadding(Ui.dp(c, 16), Ui.dp(c, 12), Ui.dp(c, 16), Ui.dp(c, 6));

        TextView sTitle = new TextView(c);
        sTitle.setText("ACTIVE SESSIONS");
        sTitle.setTextColor(Ui.DIM);
        sTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        sTitle.setLetterSpacing(0.08f);
        sessionsHead.addView(sTitle);

        sessionsCountBadge = new TextView(c);
        sessionsCountBadge.setText("0");
        sessionsCountBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        sessionsCountBadge.setTextColor(Ui.ACCENT);
        sessionsCountBadge.setBackground(Ui.rounded(c, Ui.SURFACE2, 6));
        sessionsCountBadge.setPadding(Ui.dp(c, 6), Ui.dp(c, 2), Ui.dp(c, 6), Ui.dp(c, 2));
        LayoutParams countLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        countLp.leftMargin = Ui.dp(c, 8);
        sessionsCountBadge.setLayoutParams(countLp);
        sessionsHead.addView(sessionsCountBadge);

        addView(sessionsHead, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Scrollable Sessions List
        ScrollView scroll = new ScrollView(c);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sessionsList = new LinearLayout(c);
        sessionsList.setOrientation(VERTICAL);
        sessionsList.setPadding(Ui.dp(c, 8), 0, Ui.dp(c, 8), Ui.dp(c, 16));
        scroll.addView(sessionsList, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Footer Settings Link
        View footLine = new View(c);
        footLine.setBackgroundColor(Ui.LINE);
        addView(footLine, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout footer = new LinearLayout(c);
        footer.setOrientation(HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(Ui.dp(c, 16), Ui.dp(c, 12), Ui.dp(c, 16), Ui.dp(c, 12));
        footer.setBackground(Ui.ripple(c, 0, 0));
        footer.setOnClickListener(v -> host.onNavigate("settings"));

        TextView footText = new TextView(c);
        footText.setText("Settings & Appearance");
        footText.setTextColor(Ui.TEXT);
        footText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        footer.addView(footText, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView footArrow = new TextView(c);
        footArrow.setText(">");
        footArrow.setTextColor(Ui.DIM);
        footArrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        footer.addView(footArrow);

        addView(footer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
    }

    private View sessionRow(Context c, KaliSession s, boolean active) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 6), 0);
        row.setBackground(Ui.ripple(c, active ? Ui.SURFACE2 : 0, 10));
        row.setClickable(true);
        row.setOnClickListener(v -> host.onSelect(s));
        if (!s.isConsole()) row.setOnLongClickListener(v -> { host.onRename(s); return true; });

        LayoutParams rp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 52));
        rp.bottomMargin = Ui.dp(c, 2);
        row.setLayoutParams(rp);

        TextView dot = new TextView(c);
        dot.setText("[*]");
        dot.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
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
            TextView rename = Ui.iconButton(c, "Edit");
            rename.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            rename.setTextColor(Ui.DIM);
            rename.setOnClickListener(v -> host.onRename(s));
            row.addView(rename);

            TextView close = Ui.iconButton(c, "x");
            close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            close.setTextColor(Ui.DIM);
            close.setOnClickListener(v -> host.onClose(s));
            row.addView(close);
        }
        return row;
    }

    static String state(KaliSession s) {
        if (s.isConsole()) return "Log & installer";
        if (s.isAlive()) return "Running";
        if (s.isEnded()) return "Ended";
        return "Starting...";
    }
}
