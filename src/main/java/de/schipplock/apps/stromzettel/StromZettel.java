/*
 * Copyright 2023 Andreas Schipplock
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schipplock.apps.stromzettel;

import de.schipplock.apps.stromzettel.model.ElectricityMeter;
import de.schipplock.apps.stromzettel.model.Reading;
import de.schipplock.gui.swing.dialogs.AboutDialog;
import de.schipplock.gui.swing.dialogs.FormDialog;
import de.schipplock.gui.swing.svgicon.SvgIconManager;
import de.schipplock.gui.swing.svgicon.SvgIcons;
import de.schipplock.apps.stromzettel.dao.ElectricityMeterDAO;
import de.schipplock.settings.TomlSettings;

import java.awt.*;
import java.awt.event.*;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.ResourceBundle;

import de.schipplock.gui.swing.lafmanager.LAFManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import static java.lang.String.format;

public class StromZettel extends JFrame {

    @Serial
    private static final long serialVersionUID = 7485473991756153091L;

    private enum Settings { LANGUAGE, THEME }
    
    private static final Path settingsFilePath = Path.of(System.getProperty("user.home"), ".strmzttl", "settings.toml");
    
    private static final de.schipplock.settings.Settings settings = TomlSettings.forUri(settingsFilePath.toUri());

    public static final ElectricityMeterDAO electricityMeterDAO = new ElectricityMeterDAO();

    private final JTree tree;

    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");

    private static final Dimension iconDimension = new Dimension(15, 15);

    public static final String COLOR_BLACK = "#000000";

    public static final String COLOR_GREEN = "#19bf17";

    public static final String COLOR_RED = "#ba0000";

    public static void createAndShowGui() {
        new StromZettel(localize("application.title")).setVisible(true);
    }

    public static String localize(String key) {
        ResourceBundle bundle = ResourceBundle.getBundle("i18n/MessagesBundle");
        return bundle.getString(key);
    }

    public static void centerWindow(Window frame) {
        GraphicsDevice screen = MouseInfo.getPointerInfo().getDevice();
        Rectangle r = screen.getDefaultConfiguration().getBounds();
        int x = (r.width - frame.getWidth()) / 2 + r.x;
        int y = (r.height - frame.getHeight()) / 2 + r.y;
        frame.setLocation(x, y);
    }

    public static void main(String[] args) {
        if (Files.notExists(settingsFilePath)) {
            settings.setValue(Settings.LANGUAGE.name(), "de");
            settings.setValue(Settings.THEME.name(), "FlatLaf IntelliJ");
        }

        LAFManager.create().setLookAndFeelByName(settings.getValue(Settings.THEME.name()));

        UIManager.put("Tree.closedIcon", SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_FOLDER2, iconDimension, COLOR_BLACK));
        UIManager.put("Tree.openIcon", SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_FOLDER2_OPEN, iconDimension, COLOR_BLACK));
        UIManager.put("Tree.leafIcon", SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_LIGHTNING, iconDimension, COLOR_GREEN));

        if ("de".equals(settings.getValue(Settings.LANGUAGE.name()))) {
            Locale.setDefault(Locale.GERMANY);
        } else {
            Locale.setDefault(Locale.forLanguageTag(settings.getValue(Settings.LANGUAGE.name())));
        }

        javax.swing.SwingUtilities.invokeLater(StromZettel::createAndShowGui);
    }

    public StromZettel(String title) {
        super(title);

        setupWindow();
        createMenu();

        tree = createTree();

        createMainPanel();

        setupListeners();

        pack();
        centerWindow(this);
    }

    private JPopupMenu createNewMeterPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem newMeterMenuItem = menu.add(new JMenuItem(localize("newMeterMenuItem")));
        newMeterMenuItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_PLUS_CIRCLE, iconDimension, COLOR_GREEN));
        newMeterMenuItem.addActionListener(e -> showNewElectricityMeterDialog());

        return menu;
    }

    private JPopupMenu createMeterPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem newReadingMenuItem = menu.add(new JMenuItem(localize("newReadingMenuItem")));
        newReadingMenuItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_PLUS_CIRCLE, iconDimension, COLOR_GREEN));
        newReadingMenuItem.addActionListener(e -> showNewReadingDialog());

        menu.addSeparator();

        JMenuItem editMeterMenuItem = menu.add(new JMenuItem(localize("editMeterMenuItem")));
        editMeterMenuItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_SLIDERS2, iconDimension, COLOR_BLACK));
        editMeterMenuItem.addActionListener(e -> showEditElectricityMeterDialog());

        JMenuItem deleteMeterMenuItem = menu.add(new JMenuItem(localize("deleteMeterMenuItem")));
        deleteMeterMenuItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_DASH_CIRCLE, iconDimension, COLOR_RED));
        deleteMeterMenuItem.addActionListener(e -> showDeleteElectricityMeterDialog());

        return menu;
    }

    private JPopupMenu createReadingPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem displayReadingMenuItem = menu.add(new JMenuItem(localize("displayReadingMenuItem")));
        displayReadingMenuItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_EYE, iconDimension, COLOR_GREEN));
        displayReadingMenuItem.addActionListener(e -> showReadingDialog());

        menu.addSeparator();

        JMenuItem editReadingMenuItem = menu.add(new JMenuItem(localize("editReadingMenuItem")));
        editReadingMenuItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_SLIDERS2, iconDimension, COLOR_BLACK));
        editReadingMenuItem.addActionListener(e -> showEditReadingDialog());

        JMenuItem deleteReadingMenuItem = menu.add(new JMenuItem(localize("deleteReadingMenuItem")));
        deleteReadingMenuItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_DASH_CIRCLE, iconDimension, COLOR_RED));
        deleteReadingMenuItem.addActionListener(e -> showDeleteReadingDialog());

        return menu;
    }

    private MouseAdapter createTreeMouseListener(JTree tree) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // select tree node on right mouse button
                if (SwingUtilities.isRightMouseButton(e)) {
                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    tree.setSelectionPath(selPath);
                    if (selRow > -1) {
                        tree.setSelectionRow(selRow);
                        tree.requestFocus();
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (e.getClickCount() == 2 && node.getUserObject() instanceof Reading) {
                    showReadingDialog();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                if (e.isPopupTrigger()) {
                    if (node == null) {
                        createNewMeterPopupMenu().show(e.getComponent(), e.getX(), e.getY());
                    } else if (node.getUserObject() instanceof ElectricityMeter) {
                        createMeterPopupMenu().show(e.getComponent(), e.getX(), e.getY());
                    } else if (node.getUserObject() instanceof Reading) {
                        createReadingPopupMenu().show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        };
    }

    private JTree createTree() {
        JTree meterTree = new JTree(rootNode);
        meterTree.setRootVisible(false);
        meterTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        electricityMeterDAO.findAll().forEach(meter -> {
            DefaultMutableTreeNode meterNode = new DefaultMutableTreeNode(meter);
            meter.getReadings().forEach(reading -> meterNode.add(new DefaultMutableTreeNode(reading)));
            rootNode.add(meterNode);
        });

        ((DefaultTreeModel) meterTree.getModel()).reload(rootNode);
        for (int i = 0; i < meterTree.getRowCount(); i++) meterTree.expandRow(i);

        meterTree.addMouseListener(createTreeMouseListener(meterTree));

        return meterTree;
    }

    private void createMainPanel() {
        JPanel panel = new JPanel(new MigLayout());
        panel.add(new JScrollPane(tree), "span, pushx, growx, pushy, growy");
        getContentPane().add(panel);
    }

    private void setupListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                electricityMeterDAO.em.close();

                try {
                    DriverManager.getConnection(
                            "jdbc:derby:;shutdown=true");
                } catch (SQLException ex) {
                    if (ex.getErrorCode() == 50000) {
                        // 50000 means = shutdown was a success, yea right
                        return;
                    }
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void setupWindow() {
        setPreferredSize(new Dimension(240, 220));
        setMinimumSize(new Dimension(240, 200));
        setIconImages(SvgIconManager.getWindowIconImages("images/logo.svg"));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu(localize("window.main.menu.help"));
        menu.setMnemonic(KeyEvent.VK_H);
        menu.getAccessibleContext().setAccessibleDescription("The Help Menu");
        JMenuItem settingsItem = new JMenuItem(localize("window.main.menu.help.settings"), KeyEvent.VK_S);
        settingsItem.getAccessibleContext().setAccessibleDescription(localize("window.main.menu.help.settings.description"));
        settingsItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_SLIDERS2, iconDimension, COLOR_BLACK));
        settingsItem.addActionListener(e -> showSettingsDialog());
        JMenuItem aboutItem = new JMenuItem(localize("window.main.menu.help.about"), KeyEvent.VK_A);
        aboutItem.getAccessibleContext().setAccessibleDescription(localize("window.main.menu.help.about.description"));
        aboutItem.setIcon(SvgIconManager.getBuiltinIcon(SvgIcons.SVGICON_INFO_CIRCLE, iconDimension, COLOR_BLACK));
        aboutItem.addActionListener(e -> new AboutDialog(this, true)
                .size(new Dimension(350, 220))
                .icon("images/logo.svg")
                .title("StromZettel", "#2b3d30")
                .text(localize("about"), "#101411")
                .copyright("2023 Andreas Schipplock", "#333634")
                .website("https://schipplock.de", "https://schipplock.de", "#333634")
                .center()
                .setVisible(true)
        );
        menu.add(settingsItem);
        menu.add(aboutItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);
    }

    private void showSettingsDialog() {
        String[] themes = LAFManager.create().getInstalledLookAndFeelNames();
        String[] locales = new String[] { Locale.GERMAN.toLanguageTag(), Locale.US.toLanguageTag() };

        new FormDialog(this, true)
                .title(localize("settings.title"))
                .confirmButton()
                .cancelButton()
                .beginGroup(localize("settings.global"))
                .combobox(Settings.LANGUAGE.name(), localize("settings.language"), 150, settings.getValue(Settings.LANGUAGE.name()), locales, values -> settings.setValue(Settings.LANGUAGE.name(), values.get(Settings.LANGUAGE.name())))
                .combobox(Settings.THEME.name(), localize("settings.theme"), 150, settings.getValue(Settings.THEME.name()), themes, values -> {
                    settings.setValue(Settings.THEME.name(), values.get(Settings.THEME.name()));
                    LAFManager.create().setLookAndFeelByName(values.get(Settings.THEME.name())).redraw();
                })
                .endGroup()
                .onConfirm(values -> settings.persist())
                .center()
                .autosize()
                .setVisible(true);
    }

    private void showNewElectricityMeterDialog() {
        new FormDialog(this, true)
                .title(localize("newElectricityMeterDialog.title"))
                .confirmButton(localize("add"))
                .cancelButton(localize("cancel"))
                .textfield("NAME", localize("newElectricityMeterDialog.name.caption"), "", localize("newElectricityMeterDialog.name.tooltip"), 150, value -> !value.isBlank())
                .textfield("PRICE", localize("newElectricityMeterDialog.price.caption"), "42", localize("newElectricityMeterDialog.price.tooltip"), 150, value -> {
                    try {
                        return Double.parseDouble(value) > 0;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })
                .onConfirm(values -> {
                    var name = values.get("NAME");
                    var kwhPrice = Double.parseDouble(values.get("PRICE"));
                    ElectricityMeter meter = electricityMeterDAO.merge(new ElectricityMeter(name, kwhPrice));
                    rootNode.add(new DefaultMutableTreeNode(meter));
                    ((DefaultTreeModel) tree.getModel()).reload(rootNode);
                    for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
                })
                .autosize()
                .center()
                .setVisible(true);
    }

    private void showEditElectricityMeterDialog() {
        var node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        ElectricityMeter meter = (ElectricityMeter) node.getUserObject();
        new FormDialog(this, true)
                .title(localize("editElectricityMeterDialog.title"))
                .confirmButton(localize("editElectricityMeterDialog.confirm"))
                .cancelButton(localize("editElectricityMeterDialog.cancel"))
                .textfield("NAME", localize("editElectricityMeterDialog.name.caption"), meter.getName(), localize("editElectricityMeterDialog.name.tooltip"), 150, value -> !value.isBlank())
                .textfield("PRICE", localize("editElectricityMeterDialog.price.caption"), String.valueOf(meter.getKwhPrice()), localize("editElectricityMeterDialog.price.tooltip"), 150, value -> {
                    try {
                        return Double.parseDouble(value) > 0;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })
                .onConfirm(values -> {
                    meter.setName(values.get("NAME"));
                    meter.setKwhPrice(Double.parseDouble(values.get("PRICE")));
                    electricityMeterDAO.merge(meter);
                    ((DefaultTreeModel) tree.getModel()).reload(node);
                    for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
                })
                .autosize()
                .center()
                .setVisible(true);
    }

    public void showDeleteElectricityMeterDialog() {
        if (!forSure()) return;
        var node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        var meter = (ElectricityMeter) ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).getUserObject();
        electricityMeterDAO.delete(meter);
        ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(node);
    }

    private void showNewReadingDialog() {
        var node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        ElectricityMeter meter = (ElectricityMeter) node.getUserObject();
        new FormDialog(this, true)
                .title(localize("newReadingDialog.title"))
                .confirmButton(localize("newReadingDialog.confirm"))
                .cancelButton(localize("newReadingDialog.cancel"))
                .textfield("READING", localize("newReadingDialog.reading.caption"), "", localize("newReadingDialog.reading.tooltip"), 205, value -> {
                    try {
                        return Integer.parseInt(value) >= 0;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })
                .datetimepanel("DATETIME", localize("newReadingDialog.datetime.caption"), 205, LocalDateTime.now())
                .onConfirm(values -> {
                    var reading = new Reading(Long.parseLong(values.get("READING")), LocalDateTime.parse(values.get("DATETIME")));
                    reading.setElectricityMeter(meter);
                    meter.addReading(reading);
                    var updatedMeter = electricityMeterDAO.merge(meter);
                    node.add(new DefaultMutableTreeNode(updatedMeter.getLatestReading()));
                    ((DefaultTreeModel) tree.getModel()).reload(node);
                    for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
                })
                .center()
                .autosize()
                .setVisible(true);
    }

    private void showReadingDialog() {
        var node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        Reading reading = (Reading) node.getUserObject();
        ElectricityMeter meter = reading.getElectricityMeter();
        Reading previousReading = meter.getPreviousReading(reading);

        long diff = reading.getReadingValue() - previousReading.getReadingValue();
        double kwhPrice = meter.getKwhPrice() / 100;
        double costs = diff * kwhPrice;

        new FormDialog(this, true)
                .title(localize("reading"))
                .confirmButton(localize("ok"))
                .beginGroup(localize("costs"))
                    .label("CONSUMED", format("<html><b>%s</b></html>", localize("readingDialog.consumed.caption")), "<html>" + diff + " <font color=blue>kwH</font>", localize("readingDialog.consumed.tooltip"), 100)
                    .label("KWHPRICE", format("<html><b>%s</b></html>", localize("readingDialog.kwhprice.caption")), NumberFormat.getCurrencyInstance().format(kwhPrice), localize("readingDialog.kwhprice.tooltip"), 100)
                    .label("COSTS", format("<html><b color=green>%s</b></html>", localize("readingDialog.costs.caption")), NumberFormat.getCurrencyInstance().format(costs), localize("readingDialog.costs.tooltip"), 100)
                .endGroup()
                .center()
                .autosize()
                .setVisible(true);

    }

    private void showEditReadingDialog() {
        var node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        Reading reading = (Reading) node.getUserObject();
        ElectricityMeter meter = reading.getElectricityMeter();

        new FormDialog(this, true)
                .title(localize("editReadingDialog.title"))
                .confirmButton(localize("editReadingDialog.confirm"))
                .cancelButton(localize("editReadingDialog.cancel"))
                .textfield("READING", localize("editReadingDialog.reading.caption"), reading.getReadingValue().toString(), localize("editReadingDialog.reading.tooltip"), 205, value -> {
                    try {
                        return Integer.parseInt(value) >= 0;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })
                .datetimepanel("DATETIME", localize("editReadingDialog.datetime.caption"), 205, reading.getReadingDate())
                .onConfirm(values -> {
                    Long readingValue = Long.parseLong(values.get("READING"));
                    LocalDateTime localDateTime = LocalDateTime.parse(values.get("DATETIME"));
                    reading.setReadingValue(readingValue);
                    reading.setReadingDate(localDateTime);
                    electricityMeterDAO.merge(meter);
                    ((DefaultTreeModel) tree.getModel()).reload(node);
                    for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
                })
                .center()
                .autosize()
                .setVisible(true);
    }

    public void showDeleteReadingDialog() {
        if (!forSure()) return;
        var node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        var reading = (Reading) ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).getUserObject();
        var meter = reading.getElectricityMeter();
        meter.removeReading(reading);
        electricityMeterDAO.merge(meter);
        ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(node);
    }

    private boolean forSure() {
        var result = JOptionPane.showConfirmDialog(this, localize("confirmDialog.message"), localize("confirmDialog.title"),
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result != JOptionPane.NO_OPTION && result != JOptionPane.CANCEL_OPTION;
    }
}