import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class FlutterUI {
    public static final Color BG_CANVAS   = new Color(241, 245, 249);
    public static final Color RAIL_BG     = new Color(15, 23, 42);
    public static final Color RAIL_ACTIVE = new Color(37, 99, 235);
    public static final Color SLATE_900   = new Color(15, 23, 42);
    public static final Color SLATE_800   = new Color(30, 41, 59);
    public static final Color SLATE_700   = new Color(51, 65, 85);
    public static final Color SLATE_500   = new Color(100, 116, 139);
    public static final Color SLATE_200   = new Color(226, 232, 240);
    public static final Color SLATE_50    = new Color(248, 250, 252);
    public static final Color INDIGO_600  = new Color(79, 70, 229);
    public static final Color BLUE_600    = new Color(37, 99, 235);
    public static final Color EMERALD_700 = new Color(4, 120, 87);
    public static final Color EMERALD_100 = new Color(209, 250, 229);
    public static final Color ROSE_700    = new Color(190, 18, 60);
    public static final Color ROSE_100    = new Color(255, 228, 230);
    public static final Color AMBER_700   = new Color(180, 83, 9);
    public static final Color AMBER_100   = new Color(254, 243, 199);

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
        table.setRowHeight(29);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(SLATE_200);
        table.setShowGrid(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer hdr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                l.setBackground(SLATE_800);
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setBorder(BorderFactory.createCompoundBorder(new LineBorder(SLATE_700, 1), new EmptyBorder(6, 6, 6, 6)));
                l.setOpaque(true);
                return l;
            }
        };
        table.getTableHeader().setDefaultRenderer(hdr);
        table.getTableHeader().setPreferredSize(new Dimension(100, 34));

        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) {
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : SLATE_50);
                    comp.setForeground(SLATE_900);
                }
                return comp;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (i != statusColIdx) table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        if (statusColIdx >= 0 && statusColIdx < model.getColumnCount()) {
            table.getColumnModel().getColumn(statusColIdx).setCellRenderer(new StatusBadgeRenderer());
        }
        return table;
    }

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
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g2.setColor(SLATE_200);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

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
            setBorder(new EmptyBorder(7, 14, 7, 14));
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
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class NavRailButton extends JPanel {
        private boolean selected = false, hovered = false;
        private final JLabel subLbl;
        public NavRailButton(String title, String subtitle) {
            setLayout(new BorderLayout(0, 2));
            setOpaque(false);
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setMaximumSize(new Dimension(210, 58));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            JLabel titleLbl = new JLabel(title);
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
            subLbl.setForeground(sel ? new Color(219, 234, 254) : new Color(148, 163, 184));
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) {
                g2.setColor(RAIL_ACTIVE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            } else if (hovered) {
                g2.setColor(SLATE_800);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
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
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            l.setOpaque(true);
            if (!sel) {
                if ("ELIGIBLE".equals(st)) { l.setBackground(EMERALD_100); l.setForeground(EMERALD_700); }
                else if ("NOT ELIGIBLE".equals(st) || "NOT FOUND".equals(st)) { l.setBackground(ROSE_100); l.setForeground(ROSE_700); }
                else { l.setBackground(AMBER_100); l.setForeground(AMBER_700); }
            }
            return l;
        }
    }
}
