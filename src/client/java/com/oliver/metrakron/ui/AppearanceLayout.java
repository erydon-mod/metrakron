package com.oliver.metrakron.ui;

/** Shared bounds for native menu hit targets and their custom artwork. */
record AppearanceLayout(int top, int header, int controlX, int controlWidth, int firstControlY,
                        boolean showPreview) {
    static AppearanceLayout fit(int width, int height) {
        boolean preview = height >= 240;
        int header = height >= 296 && width >= 248 ? 56 : 0;
        int controlWidth = Math.max(48, Math.min(216, width - 32));
        int top = preview ? Math.max(8, (height - 226 - header) / 2) : Math.max(8, (height - 141) / 2);
        return new AppearanceLayout(top, header, (width - controlWidth) / 2, controlWidth,
                top + header + (preview ? 103 : 18), preview);
    }

    int panelX() { return controlX - 8; }
    int panelY() { return top - 5; }
    int panelWidth() { return controlWidth + 16; }
    int panelHeight() { return (showPreview ? 233 : 148) + header; }
    int footerWidth() { return (controlWidth - 12) / 3; }
    int titleCenterX() { return controlX + controlWidth / 2; }
    int titleCenterY() { return top + header + 8; }
    int titleWidth() { return Math.max(1, controlWidth - (header == 0 ? 40 : 0)); }
}
