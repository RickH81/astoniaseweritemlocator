package com.rustharbor;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static SewerWorkbookCube.CubeData sewerCubeData;

    public static void main(String[] args) {
        sewerCubeData = loadSewerWorkbookCube();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sewer Item Locator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            MapPanel mapPanel = new MapPanel();
            JScrollPane scrollPane = new JScrollPane(mapPanel);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            mapPanel.attachScrollPane(scrollPane);
            frame.add(scrollPane, BorderLayout.CENTER);

            JPanel controls = buildControls(mapPanel);
            frame.add(controls, BorderLayout.SOUTH);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static SewerWorkbookCube.CubeData loadSewerWorkbookCube() {
        List<Path> candidates = List.of(
                Path.of("src", "main", "java", "com", "rustharbor", "SEWERS.xlsx"),
                Path.of("seweritemlocator", "src", "main", "java", "com", "rustharbor", "SEWERS.xlsx")
        );

        for (Path path : candidates) {
            try {
                SewerWorkbookCube.CubeData cube = SewerWorkbookCube.load(path);
                int rows = cube.sheetCount() > 0 ? cube.rowCount(0) : 0;
                int cols = cube.sheetCount() > 0 ? cube.columnCount(0) : 0;
                System.out.println("Loaded SEWERS.xlsx cube: sheets=" + cube.sheetCount() + ", rows=" + rows + ", cols=" + cols);
                return cube;
            } catch (Exception ignored) {
            }
        }

        System.out.println("SEWERS.xlsx not found or unreadable; using empty cube.");
        return new SewerWorkbookCube.CubeData(new String[0], new String[0][][]);
    }

    private static JPanel buildControls(MapPanel mapPanel) {
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);

        JLabel xLabel = new JLabel("X:");
        JTextField xInput = new JTextField(3);
        JLabel yLabel = new JLabel("Y:");
        JTextField yInput = new JTextField(3);

        Dimension coordinateFieldSize = new Dimension(44, xInput.getPreferredSize().height);
        xInput.setPreferredSize(coordinateFieldSize);
        xInput.setMinimumSize(coordinateFieldSize);
        xInput.setMaximumSize(coordinateFieldSize);
        yInput.setPreferredSize(coordinateFieldSize);
        yInput.setMinimumSize(coordinateFieldSize);
        yInput.setMaximumSize(coordinateFieldSize);

        JRadioButton nw = new JRadioButton("NW");
        JRadioButton ne = new JRadioButton("NE");
        JRadioButton sw = new JRadioButton("SW");
        JRadioButton se = new JRadioButton("SE");
        ButtonGroup group = new ButtonGroup();
        group.add(nw);
        group.add(ne);
        group.add(sw);
        group.add(se);
        ne.setSelected(true);

        JButton draw = new JButton("Draw");
        JButton reset = new JButton("Reset");
        JButton zoomIn = new JButton("Zoom In");
        JButton zoomOut = new JButton("Zoom Out");

        draw.addActionListener(event -> {
            int x;
            int y;
            try {
                x = Integer.parseInt(xInput.getText().trim());
                y = Integer.parseInt(yInput.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(controls, "Enter integer X and Y values from 0 to 255.");
                return;
            }

            if (x < 0 || x > 255 || y < 0 || y > 255) {
                JOptionPane.showMessageDialog(controls, "X and Y must be within 0 to 255.");
                return;
            }

            Direction direction;
            if (nw.isSelected()) {
                direction = Direction.NW;
            } else if (ne.isSelected()) {
                direction = Direction.NE;
            } else if (sw.isSelected()) {
                direction = Direction.SW;
            } else {
                direction = Direction.SE;
            }

            mapPanel.addEntry(x, y, direction);
        });

        reset.addActionListener(event -> mapPanel.reset());

        zoomIn.addActionListener(event -> mapPanel.zoomBy(1.25));
        zoomOut.addActionListener(event -> mapPanel.zoomBy(0.8));

        gbc.gridy = 0;
        gbc.gridx = 0;
        controls.add(xLabel, gbc);
        gbc.gridx = 1;
        controls.add(xInput, gbc);
        gbc.gridx = 2;
        controls.add(yLabel, gbc);
        gbc.gridx = 3;
        controls.add(yInput, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        controls.add(nw, gbc);
        gbc.gridx = 1;
        controls.add(ne, gbc);
        gbc.gridx = 2;
        controls.add(sw, gbc);
        gbc.gridx = 3;
        controls.add(se, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        controls.add(draw, gbc);
        gbc.gridx = 1;
        controls.add(reset, gbc);
        gbc.gridx = 2;
        controls.add(zoomIn, gbc);
        gbc.gridx = 3;
        controls.add(zoomOut, gbc);

        return controls;
    }

    private enum Direction {
        NW, NE, SW, SE
    }

    private record Entry(int x, int y, Direction direction) {
    }

    private static final class MapPanel extends JPanel {
        private static final int MAP_SIZE = 768;
        private static final int GRID_MAX = 255;
        private static final double MIN_ZOOM = 0.5;
        private static final double MAX_ZOOM = 6.0;
        private final List<Entry> entries = new ArrayList<>();
        private BufferedImage baseImage;
        private double zoom = 1.0;
        private JScrollPane scrollPane;
        private Point dragStartScreen;
        private Point dragStartViewPosition;

        private MapPanel() {
            setBackground(Color.BLACK);
            updatePreferredSize();
            baseImage = loadMapImage();

            MouseAdapter dragToPan = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (scrollPane == null || zoom <= 1.0) {
                        return;
                    }
                    dragStartScreen = e.getLocationOnScreen();
                    dragStartViewPosition = scrollPane.getViewport().getViewPosition();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (scrollPane == null || zoom <= 1.0 || dragStartScreen == null || dragStartViewPosition == null) {
                        return;
                    }

                    Point current = e.getLocationOnScreen();
                    int dx = current.x - dragStartScreen.x;
                    int dy = current.y - dragStartScreen.y;

                    Rectangle viewRect = scrollPane.getViewport().getViewRect();
                    int maxX = Math.max(0, getWidth() - viewRect.width);
                    int maxY = Math.max(0, getHeight() - viewRect.height);

                    int nextX = clamp(dragStartViewPosition.x - dx, 0, maxX);
                    int nextY = clamp(dragStartViewPosition.y - dy, 0, maxY);
                    scrollPane.getViewport().setViewPosition(new Point(nextX, nextY));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragStartScreen = null;
                    dragStartViewPosition = null;
                }
            };

            addMouseListener(dragToPan);
            addMouseMotionListener(dragToPan);
        }

        private void attachScrollPane(JScrollPane pane) {
            this.scrollPane = pane;
        }

        private void zoomBy(double factor) {
            setZoom(zoom * factor);
        }

        private void setZoom(double nextZoom) {
            double clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, nextZoom));
            if (Math.abs(clamped - zoom) < 0.0001) {
                return;
            }

            Rectangle oldView = scrollPane != null ? scrollPane.getViewport().getViewRect() : null;
            double centerXRatio = 0.5;
            double centerYRatio = 0.5;
            if (oldView != null && getWidth() > 0 && getHeight() > 0) {
                centerXRatio = (oldView.getCenterX()) / getWidth();
                centerYRatio = (oldView.getCenterY()) / getHeight();
            }

            zoom = clamped;
            updatePreferredSize();
            revalidate();
            repaint();

            if (scrollPane != null && oldView != null) {
                Rectangle newView = scrollPane.getViewport().getViewRect();
                int targetX = (int) Math.round(centerXRatio * getWidth() - (newView.width / 2.0));
                int targetY = (int) Math.round(centerYRatio * getHeight() - (newView.height / 2.0));
                int maxX = Math.max(0, getWidth() - newView.width);
                int maxY = Math.max(0, getHeight() - newView.height);
                scrollPane.getViewport().setViewPosition(new Point(
                        clamp(targetX, 0, maxX),
                        clamp(targetY, 0, maxY)
                ));
            }
        }

        private void updatePreferredSize() {
            int size = (int) Math.round(MAP_SIZE * zoom);
            setPreferredSize(new Dimension(size, size));
        }

        private void addEntry(int x, int y, Direction direction) {
            entries.add(new Entry(x, y, direction));

            repaint();
        }

        private void reset() {
            entries.clear();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            drawRotatedMap(g2);
            drawGrid(g2);
            drawSearchBounds(g2);
            drawEntries(g2);
            drawCardinalLabels(g2);
            drawAttribution(g2);

            g2.dispose();
        }

        private void drawAttribution(Graphics2D g2) {
            Rectangle anchor;
            if (scrollPane != null) {
                anchor = scrollPane.getViewport().getViewRect();
            } else {
                anchor = new Rectangle(0, 0, getWidth(), getHeight());
            }

            int x = anchor.x + 10;
            int y = anchor.y + 18;

            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(x - 6, y - 14, 240, 54, 8, 8);

            g2.setColor(new Color(255, 255, 255, 235));
            g2.drawString("Created by: Vozziks", x, y);
            g2.drawString("Last updated: March 2, 2026", x, y + 16);
            g2.drawString("Version 1.0", x, y + 32);
        }

        private void drawRotatedMap(Graphics2D g2) {
            Rectangle mapRect = getMapRect();
            int size = mapRect.width;

            if (baseImage == null) {
                g2.setColor(new Color(30, 30, 30));
                g2.fillRect(mapRect.x, mapRect.y, size, size);
                g2.setColor(new Color(220, 220, 220));
                g2.drawString("No map loaded.", mapRect.x + 12, mapRect.y + 22);
                return;
            }

            BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = canvas.createGraphics();
            cg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            cg.setColor(new Color(10, 10, 10));
            cg.fillRect(0, 0, size, size);

            AffineTransform tx = new AffineTransform();
            tx.translate(size / 2.0, size / 2.0);
            tx.rotate(Math.toRadians(45));

            double theta = Math.toRadians(45);
            double rotatedW = Math.abs(baseImage.getWidth() * Math.cos(theta)) + Math.abs(baseImage.getHeight() * Math.sin(theta));
            double rotatedH = Math.abs(baseImage.getWidth() * Math.sin(theta)) + Math.abs(baseImage.getHeight() * Math.cos(theta));
            double scale = Math.max((double) size / rotatedW, (double) size / rotatedH);

            tx.scale(scale, scale);
            tx.translate(-baseImage.getWidth() / 2.0, -baseImage.getHeight() / 2.0);

            cg.drawImage(baseImage, tx, null);
            cg.dispose();

            g2.drawImage(canvas, mapRect.x, mapRect.y, null);
        }

        private void drawGrid(Graphics2D g2) {
            Rectangle mapRect = getMapRect();
            int size = mapRect.width;
            if (baseImage == null) {
                int left = mapRect.x;
                int top = mapRect.y;
                g2.setColor(new Color(180, 220, 255, 140));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(left, top, size, size);
                return;
            }

            Point2D n = mapGridToPanel(0, 0, mapRect);
            Point2D e = mapGridToPanel(255, 0, mapRect);
            Point2D s = mapGridToPanel(255, 255, mapRect);
            Point2D w = mapGridToPanel(0, 255, mapRect);

            Path2D border = new Path2D.Double();
            border.moveTo(n.getX(), n.getY());
            border.lineTo(e.getX(), e.getY());
            border.lineTo(s.getX(), s.getY());
            border.lineTo(w.getX(), w.getY());
            border.closePath();

            g2.setColor(new Color(180, 220, 255, 140));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(border);

            g2.setColor(new Color(180, 220, 255, 55));
            g2.setStroke(new BasicStroke(1f));
            for (int i = 1; i < 16; i++) {
                double t = (255.0 * i) / 16.0;
                Point2D vStart = mapGridToPanel(t, 0, mapRect);
                Point2D vEnd = mapGridToPanel(t, 255, mapRect);
                g2.drawLine((int) Math.round(vStart.getX()), (int) Math.round(vStart.getY()),
                        (int) Math.round(vEnd.getX()), (int) Math.round(vEnd.getY()));

                Point2D hStart = mapGridToPanel(0, t, mapRect);
                Point2D hEnd = mapGridToPanel(255, t, mapRect);
                g2.drawLine((int) Math.round(hStart.getX()), (int) Math.round(hStart.getY()),
                        (int) Math.round(hEnd.getX()), (int) Math.round(hEnd.getY()));
            }
        }

        private void drawSearchBounds(Graphics2D g2) {
            if (entries.isEmpty()) {
                return;
            }

            Rectangle mapRect = getMapRect();
            g2.setColor(new Color(255, 80, 40));
            g2.setStroke(new BasicStroke(3f));
            Rectangle bounds = computeDirectionalBounds(mapRect);
            int drawX = bounds.x;
            int drawY = bounds.y;
            int drawW = Math.max(1, bounds.width);
            int drawH = Math.max(1, bounds.height);
            g2.drawRect(drawX, drawY, drawW, drawH);
        }

        private void drawEntries(Graphics2D g2) {
            Rectangle mapRect = getMapRect();
            g2.setColor(new Color(70, 255, 70));
            g2.setStroke(new BasicStroke(2f));
            if (baseImage == null) {
                int size = mapRect.width;
                int left = mapRect.x;
                int top = mapRect.y;
                double cell = size / 255.0;
                for (Entry entry : entries) {
                    int x = left + (int) Math.round(entry.x * cell);
                    int y = top + (int) Math.round(entry.y * cell);
                    g2.drawRect(x - 3, y - 3, 6, 6);
                }
                return;
            }

            for (Entry entry : entries) {
                Point2D point = mapGridToPanel(entry.x, entry.y, mapRect);
                int x = (int) Math.round(point.getX());
                int y = (int) Math.round(point.getY());
                g2.drawRect(x - 3, y - 3, 6, 6);
            }
        }

        private void drawCardinalLabels(Graphics2D g2) {
            Rectangle mapRect = getMapRect();
            g2.setColor(new Color(255, 255, 255, 230));
            int w = getWidth();
            int h = getHeight();
            int size = mapRect.width;
            if (baseImage == null) {
                int left = mapRect.x;
                int top = mapRect.y;
                g2.drawString("N", left + size / 2, Math.max(14, top - 6));
                g2.drawString("S", left + size / 2, Math.min(h - 6, top + size + 14));
                g2.drawString("W", Math.max(6, left - 16), top + size / 2);
                g2.drawString("E", Math.min(w - 14, left + size + 6), top + size / 2);
                return;
            }

            Point2D n = mapGridToPanel(0, 0, mapRect);
            Point2D e = mapGridToPanel(255, 0, mapRect);
            Point2D s = mapGridToPanel(255, 255, mapRect);
            Point2D wPt = mapGridToPanel(0, 255, mapRect);
            g2.drawString("N", (int) Math.round(n.getX()) - 4, (int) Math.round(n.getY()) - 8);
            g2.drawString("S", (int) Math.round(s.getX()) - 4, (int) Math.round(s.getY()) + 14);
            g2.drawString("W", (int) Math.round(wPt.getX()) - 14, (int) Math.round(wPt.getY()) + 4);
            g2.drawString("E", (int) Math.round(e.getX()) + 6, (int) Math.round(e.getY()) + 4);
        }

        private BufferedImage loadMapImage() {
            try (InputStream input = Main.class.getResourceAsStream("/sewer_map.png")) {
                if (input != null) {
                    return ImageIO.read(input);
                }
            } catch (IOException ex) {
            }

            try (InputStream input = Main.class.getResourceAsStream("sewer_map.png")) {
                if (input != null) {
                    return ImageIO.read(input);
                }
            } catch (IOException ex) {
            }

            return null;
        }

        private boolean loadMapFromFile(File file) {
            if (file == null || !file.exists()) {
                return false;
            }
            try {
                BufferedImage loaded = ImageIO.read(file);
                if (loaded == null) {
                    return false;
                }
                baseImage = loaded;
                repaint();
                return true;
            } catch (IOException ex) {
                return false;
            }
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private Rectangle computeDirectionalBounds(Rectangle mapRect) {
            int left = mapRect.x;
            int top = mapRect.y;
            int right = mapRect.x + mapRect.width;
            int bottom = mapRect.y + mapRect.height;

            double minPanelX = left;
            double maxPanelX = right;
            double minPanelY = top;
            double maxPanelY = bottom;

            Point2D lastPoint = null;

            for (Entry entry : entries) {
                Point2D point;
                if (baseImage == null) {
                    double cell = mapRect.width / 255.0;
                    point = new Point2D.Double(left + entry.x * cell, top + entry.y * cell);
                } else {
                    point = mapGridToPanel(entry.x, entry.y, mapRect);
                }

                lastPoint = point;

                switch (entry.direction) {
                    case NW -> {
                        maxPanelX = Math.min(maxPanelX, point.getX());
                        maxPanelY = Math.min(maxPanelY, point.getY());
                    }
                    case NE -> {
                        minPanelX = Math.max(minPanelX, point.getX());
                        maxPanelY = Math.min(maxPanelY, point.getY());
                    }
                    case SW -> {
                        maxPanelX = Math.min(maxPanelX, point.getX());
                        minPanelY = Math.max(minPanelY, point.getY());
                    }
                    case SE -> {
                        minPanelX = Math.max(minPanelX, point.getX());
                        minPanelY = Math.max(minPanelY, point.getY());
                    }
                }
            }

            if (minPanelX > maxPanelX || minPanelY > maxPanelY) {
                if (lastPoint == null) {
                    return new Rectangle(left, top, 1, 1);
                }
                int px = (int) Math.round(lastPoint.getX());
                int py = (int) Math.round(lastPoint.getY());
                return new Rectangle(px, py, 1, 1);
            }

            int x = (int) Math.floor(minPanelX);
            int y = (int) Math.floor(minPanelY);
            int width = (int) Math.ceil(maxPanelX - minPanelX);
            int height = (int) Math.ceil(maxPanelY - minPanelY);
            return new Rectangle(x, y, Math.max(1, width), Math.max(1, height));
        }

        private Point2D mapGridToPanel(double gridX, double gridY, Rectangle mapRect) {
            AffineTransform tx = getImageToCanvasTransform(mapRect.width);
            Point2D north = tx.transform(new Point2D.Double(0, 0), null);
            Point2D east = tx.transform(new Point2D.Double(baseImage.getWidth(), 0), null);
            Point2D west = tx.transform(new Point2D.Double(0, baseImage.getHeight()), null);

            double xFactor = gridX / 255.0;
            double yFactor = gridY / 255.0;

            double panelX = north.getX()
                + xFactor * (east.getX() - north.getX())
                + yFactor * (west.getX() - north.getX())
                + mapRect.x;
            double panelY = north.getY()
                + xFactor * (east.getY() - north.getY())
                + yFactor * (west.getY() - north.getY())
                + mapRect.y;

            return new Point2D.Double(panelX, panelY);
        }

        private AffineTransform getImageToCanvasTransform(int size) {
            AffineTransform tx = new AffineTransform();
            tx.translate(size / 2.0, size / 2.0);
            tx.rotate(Math.toRadians(45));

            double theta = Math.toRadians(45);
            double rotatedW = Math.abs(baseImage.getWidth() * Math.cos(theta)) + Math.abs(baseImage.getHeight() * Math.sin(theta));
            double rotatedH = Math.abs(baseImage.getWidth() * Math.sin(theta)) + Math.abs(baseImage.getHeight() * Math.cos(theta));
            double scale = Math.max((double) size / rotatedW, (double) size / rotatedH);

            tx.scale(scale, scale);
            tx.translate(-baseImage.getWidth() / 2.0, -baseImage.getHeight() / 2.0);
            return tx;
        }

        private Rectangle getMapRect() {
            int size = Math.min(getWidth(), getHeight());
            int left = (getWidth() - size) / 2;
            int top = (getHeight() - size) / 2;
            return new Rectangle(left, top, size, size);
        }
    }
}