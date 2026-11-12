import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class FlutterUI {
    // Soft FinTech Canvas & Pastel Surfaces
    public static final Color BG_CANVAS   = new Color(244, 247, 250);
    public static final Color RAIL_BG     = new Color(17, 24, 39);
    public static final Color RAIL_ACTIVE = new Color(56, 189, 248);

    // Pastel Card Gradient & Surface Colors (Matching FinTech Screenshot)
    public static final Color PASTEL_MINT_1 = new Color(216, 243, 220);
    public static final Color PASTEL_MINT_2 = new Color(183, 228, 199);
    public static final Color PASTEL_GOLD   = new Color(254, 240, 138);
    public static final Color PASTEL_LILAC  = new Color(233, 213, 255);
    public static final Color PASTEL_SKY_1  = new Color(216, 241, 250);
    public static final Color PASTEL_SKY_2  = new Color(186, 230, 253);
    public static final Color PASTEL_ROSE_1 = new Color(255, 228, 230);
    public static final Color PASTEL_ROSE_2 = new Color(254, 205, 211);
    public static final Color PASTEL_AMBER  = new Color(254, 243, 199);

    // Core Text & Accent Colors
    public static final Color SLATE_900   = new Color(15, 23, 42);
    public static final Color SLATE_800   = new Color(30, 41, 59);
    public static final Color SLATE_700   = new Color(51, 65, 85);
    public static final Color SLATE_500   = new Color(100, 116, 139);
    public static final Color SLATE_200   = new Color(226, 232, 240);
    public static final Color SLATE_50    = new Color(248, 250, 252);
    public static final Color INDIGO_600  = new Color(99, 102, 241);
    public static final Color BLUE_600    = new Color(14, 165, 233);
    public static final Color EMERALD_700 = new Color(4, 120, 87);
    public static final Color EMERALD_100 = new Color(209, 250, 229);
    public static final Color ROSE_700    = new Color(190, 18, 60);
    public static final Color ROSE_100    = new Color(255, 228, 230);
    public static final Color AMBER_700   = new Color(180, 83, 9);
    public static final Color AMBER_100   = new Color(254, 243, 199);

    // Borderless Modern FinTech Table (No harsh vertical gridlines)
    public static JTable createStyledTable(DefaultTableModel model, final int statusColIdx) {
        JTable table = new JTable(model) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int r = rowAtPoint(e.getPoint()), c = columnAtPoint(e.getPoint());
                if (r >= 0 && c >= 0) {
                    Object v = getValueAt(r, c);
                    return v != null ? v.toString() : null;
                }
                return null;
            }
        };
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(PASTEL_SKY_2);
        table.setSelectionForeground(SLATE_900);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer hdr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                l.setBackground(new Color(248, 250, 252));
                l.setForeground(SLATE_500);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, SLATE_200),
                    new EmptyBorder(8, 10, 8, 10)
                ));
                l.setOpaque(true);
                return l;
            }
        };
        table.getTableHeader().setDefaultRenderer(hdr);
        table.getTableHeader().setPreferredSize(new Dimension(100, 38));

        DefaultTableCellRenderer cleanRowRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel comp = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                comp.setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!s) {
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(250, 252, 255));
                    comp.setForeground(SLATE_900);
                }
                return comp;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (i != statusColIdx) table.getColumnModel().getColumn(i).setCellRenderer(cleanRowRenderer);
        }
        if (statusColIdx >= 0 && statusColIdx < model.getColumnCount()) {
            table.getColumnModel().getColumn(statusColIdx).setCellRenderer(new StatusBadgeRenderer());
        }
        return table;
    }

    // 24px Ultra-Rounded Soft Card with Subtle Shadow
    public static class RoundedCard extends JPanel {
        private final Color bgColor;
        public RoundedCard(Color bgColor) {
            this.bgColor = bgColor;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Soft outer elevation shadow
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fill(new RoundRectangle2D.Float(2, 3, getWidth() - 4, getHeight() - 4, 24, 24));
            // Card body
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 3, getHeight() - 4, 24, 24));
            g2.setColor(new Color(226, 232, 240, 180));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 3, getHeight() - 4, 24, 24));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Pastel Gradient FinTech Card (Matches the Yellow/Lavender & Mint Cards in Screenshot)
    public static class PastelGradientCard extends JPanel {
        private final Color c1, c2;
        private final String badgeSymbol;
        public PastelGradientCard(Color c1, Color c2, String badgeSymbol) {
            this.c1 = c1;
            this.c2 = c2;
            this.badgeSymbol = badgeSymbol;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 3, h = getHeight() - 4;

            // Soft drop shadow
            g2.setColor(new Color(15, 23, 42, 14));
            g2.fill(new RoundRectangle2D.Float(2, 4, w, h, 26, 26));

            // Smooth diagonal pastel gradient
            GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 26, 26));

            // Decorative soft translucent circle in top-right (FinTech style)
            g2.setColor(new Color(255, 255, 255, 140));
            g2.fill(new Ellipse2D.Float(w - 44, 12, 32, 32));

            g2.setColor(SLATE_800);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - 44) + (32 - fm.stringWidth(badgeSymbol)) / 2;
            int ty = 12 + ((32 - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(badgeSymbol, tx, ty);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Circular Avatar Icon for Account Rows (Like the Transactions List in Screenshot)
    public static class CircleAvatar extends JComponent {
        private final String text;
        private final Color bg, fg;
        public CircleAvatar(String text, Color bg, Color fg) {
            this.text = text;
            this.bg = bg;
            this.fg = fg;
            setPreferredSize(new Dimension(38, 38));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new Ellipse2D.Float(1, 1, 35, 35));
            g2.setColor(fg);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int x = 1 + (35 - fm.stringWidth(text)) / 2;
            int y = 1 + ((35 - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

    // Pill-Shaped FinTech Button
    public static class FlutterButton extends JButton {
        private final Color baseBg;
        private boolean hovered = false;
        public FlutterButton(String text, Color bg, Color fg) {
            super(text);
            this.baseBg = bg;
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? baseBg.brighter() : baseBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class NavRailButton extends JPanel {
        private boolean selected = false, hovered = false;
        private final JLabel titleLbl, subLbl;
        public NavRailButton(String title, String subtitle) {
            setLayout(new BorderLayout(0, 2));
            setOpaque(false);
            setBorder(new EmptyBorder(12, 16, 12, 16));
            setMaximumSize(new Dimension(210, 62));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            titleLbl = new JLabel(title);
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLbl.setForeground(Color.WHITE);
            subLbl = new JLabel(subtitle);
            subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            subLbl.setForeground(new Color(148, 163, 184));
            add(titleLbl, BorderLayout.NORTH);
            add(subLbl, BorderLayout.SOUTH);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }
        public void setSelected(boolean sel) {
            this.selected = sel;
            titleLbl.setForeground(sel ? SLATE_900 : Color.WHITE);
            subLbl.setForeground(sel ? SLATE_800 : new Color(148, 163, 184));
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) {
                GradientPaint gp = new GradientPaint(0, 0, PASTEL_MINT_1, getWidth(), getHeight(), PASTEL_SKY_2);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
            } else if (hovered) {
                g2.setColor(SLATE_800);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class ScrollableCardPanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, f, r, c);
            String st = String.valueOf(val);
            l.setHorizontalAlignment(CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 11));
            l.setOpaque(true);
            if (!sel) {
                if ("ELIGIBLE".equals(st)) { l.setBackground(PASTEL_MINT_1); l.setForeground(EMERALD_700); }
                else if ("NOT ELIGIBLE".equals(st) || "NOT FOUND".equals(st)) { l.setBackground(PASTEL_ROSE_1); l.setForeground(ROSE_700); }
                else { l.setBackground(PASTEL_AMBER); l.setForeground(AMBER_700); }
            }
            return l;
        }
    }
}