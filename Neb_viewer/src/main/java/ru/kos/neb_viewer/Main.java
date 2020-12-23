package ru.kos.neb_viewer;

/**
 *
 * @author kos
 */
import java.awt.*;
import java.awt.print.*;
//import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.*;
import javax.swing.JPopupMenu;
import java.net.URL;
import org.piccolo2d.PCanvas;
import org.piccolo2d.PLayer;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PDragSequenceEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.PCamera;
import org.piccolo2d.util.PBounds;

import org.piccolo2d.extras.PFrame;
import java.io.File;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.kos.neb.neb_lib.GetSnmp;
import static ru.kos.neb_viewer.Utils.SetPositionToNode;

public class Main extends PFrame {

    public static String neb_server = "10.120.63.3";
//    public static String neb_server = "localhost";
    public static String neb_server_port = "8080";
    public static String user = "init";
    public static String passwd = "init";
    public static Main m_Main = null;
    public static Map cfg = new HashMap();
    public static Map cfg_server = new HashMap();
    public static Utils utils = new Utils();
    public static PLayer nodeLayer;
    public static PLayer flashLayer;
    public static PLayer interfnameLayer;
    public static PLayer edgeLayer;
    public static PLayer textLayer;
    public static PLayer textCustomLayer;
    public static PLayer selectLayer;
    public static PCamera camera;
    public static PText tooltipNode;
    public static PText tooltipNode1;
    public static PCanvas canvas;
    public static PPath leftLineSelectNode;
    public static PPath rightLineSelectNode;
    public static PPath topLineSelectNode;
    public static PPath bottomLineSelectNode;
    public static double view_scale;
    public static JPopupMenu contextNodeMenu;
    public static JPopupMenu contextNodeMenuCustom;
    public static JPopupMenu contextNodeMenuView;
    public static JPopupMenu contextLinkMenu;
    public static JPopupMenu contextLinkMenuCustom;
    public static JPopupMenu contextLinkMenuView;
    public static JPopupMenu contextCanvasMenu;
    public static JPopupMenu contextTextMenu;
    public static JPopupMenu contextTextCustomMenu;
    public static PNode node_sel;
    public static boolean isChanged = false;
    public static boolean isBusy = true;
    public static JButton button_save;
    public static String image_path_relative = "images";
    public static String image_path = image_path_relative;
    public static String default_image = image_path_relative + "/default.png";
    public static String path = "";
    public static boolean isModeEdit = false;
    public static String map_file = "neb.map";
    public static String map_filename = "neb.map";
//    public static String map_base = "neb.map";
//    public static String monitor_cfg = "monitor.cfg";
    public static String neb_cfg = "neb_viewer.cfg";
    public static String nebserver_cfg = "neb.cfg";
    public static String history_path = "history";
    public static String history_path_short = "history";
//    private static String indexLocation = "Index";
    public static String canvas_color = "255,255,255";
    public static String dump = "dump.tmp";
//    public static String position_file = "tmp/position.tmp";
    public static ArrayList<String[]> history_list;
    public static JPopupMenu jPopupMenuPrev = new javax.swing.JPopupMenu();
    public static JPopupMenu jPopupMenuNext = new javax.swing.JPopupMenu();
    public static ControlPanel control_panel;
    public static TimeMachineForm time_machine;
    public static PImage pbackground_image;
    public static String home = "";
    public static boolean run_config_form = false;
    public static int origRadiusFlashLink = 12;
    public static FlashColor m_FlashColor1;
    public static FlashColor m_FlashColor2;
    public static long timeout = 2 * 1000;
    public static long timeout_walk = 5 * 1000;

    public static ProcessPaintFlashLinks m_ProcessPaintFlashLinks = null;
    public static ProcessHideFlashLinks m_ProcessHideFlashLinks = null;

    public static Point2D link_point1 = null;
    public static Point2D link_point2 = null;
    public static PNode link_node1 = null;
    public static PNode link_node2 = null;
    public static boolean paint_flash_running = false;
    public static long mouse_entered_edge_prev = 0;
    public static long mouse_entered_edge_cur = 0;
    public static ArrayList sensors = new ArrayList();
    public static boolean not_map_viewer = false;
    public static boolean run_map_viewer = false;
    public static String support_file = "tmp/support.tmp";

    public static Logger logger;
    public static GetSnmp snmp_get = new GetSnmp();

    public static JTextField find_field;
    public static JRadioButton ipButton;
    public static JRadioButton macButton;
    public static JRadioButton sysnameButton;
    public static JTree jTree1;
    public static JList full_text_search_result;
    public static JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
    public static DefaultListModel<String> dlm = new DefaultListModel<String>();
    public static Map<Integer, String[]> index_area_node = new HashMap();
    public static DefaultMutableTreeNode root;
    public static JButton toggle;
    public static JButton search_button;

    private Map<String, String> file_found = new HashMap<>();
    public static Map INFORMATION = new HashMap<>();
    public static ArrayList<ArrayList<String>> find_result_list = new ArrayList();

    public Main() {
        this(null);
    }

    public Main(PCanvas aCanvas) {
        super("NEB map viewer", false, aCanvas);
//        ImageIcon m_ImageIcon = new ImageIcon();
//        URL url = m_ImageIcon.getClass().getResource("/images/idle.gif");
        URL url = Main.class.getResource("/images/idle.gif");
        setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url));
    }

    @Override
    public void beforeInitialize() {
        this.setExtendedState(PFrame.MAXIMIZED_BOTH);
    }

    @Override
    @SuppressWarnings("static-access")
    public void initialize() {
        UserPasswd.createUserPasswdDialog();
        history_list = utils.GetListMapFiles();

        String[] rgb = canvas_color.split(",");
        if (rgb.length == 3) {
            this.getCanvas().setBackground(new Color(Integer.valueOf(rgb[0]), Integer.valueOf(rgb[1]), Integer.valueOf(rgb[2])));
        }

        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                formMouseWheelMoved(evt);
            }
        });

        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        JToolBar toolbar = new JToolBar();
        toolbar.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        toolbar.setOpaque(true);
        toolbar.setBackground(new java.awt.Color(220, 220, 190));
        c.add(toolbar, BorderLayout.NORTH);

        canvas = getCanvas();
        toolbar.add(control_panel);

        history_list = utils.GetListMapFiles();
        time_machine = new TimeMachineForm();
        TimeMachineForm.selector = history_list.size() - 1;
        Utils.SetTimeMachine(history_list, TimeMachineForm.selector, ControlPanel.area_select);
//        toolbar.addSeparator();
        toolbar.add(time_machine);
//        toolbar.addSeparator();

        CheckerMapFile checkerMapFile = new CheckerMapFile();
        checkerMapFile.start();

        ///////////////////// adding collapse pannel 
        final JXCollapsiblePane cp
                = new JXCollapsiblePane(JXCollapsiblePane.Direction.LEFT);

        // JXCollapsiblePane can be used like any other container
        cp.setLayout(new BorderLayout());
        cp.setPreferredSize(new Dimension(300, c.getHeight()));

        // the Controls panel with a textfield to filter the tree
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        find_field = new JFormattedTextField();
        find_field.setColumns(17);
        find_field.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField1MouseClicked(evt);
            }
        });
        find_field.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField1KeyTyped(evt);
            }
        });

        find_field.setDocument(new HexDocument());

        searchPanel.add(find_field);
        search_button = new JButton("Find");

        search_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FindAction();
            }
        });

        searchPanel.add(search_button);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        ipButton = new JRadioButton("Ip / Name", true);
        ipButton.addActionListener(new ListenerActionRadioButton());
        buttonPanel.add(ipButton);
        macButton = new JRadioButton("Mac", false);
        macButton.addActionListener(new ListenerActionRadioButton());
        buttonPanel.add(macButton);
        sysnameButton = new JRadioButton("FullText", false);
        sysnameButton.addActionListener(new ListenerActionRadioButton());
        buttonPanel.add(sysnameButton);

        JPanel group_search_pannel = new JPanel(new BorderLayout());
        group_search_pannel.setBorder(BorderFactory.createLineBorder(new Color(184, 207, 229), 2, true));
        group_search_pannel.add(searchPanel, BorderLayout.NORTH);
        group_search_pannel.add(buttonPanel, BorderLayout.CENTER);

        JPanel result_pannel = new JPanel(new BorderLayout());
        result_pannel.setBorder(BorderFactory.createLineBorder(new Color(184, 207, 229), 2, true));
//        JScrollPane jScrollPane1 = new javax.swing.JScrollPane();

        root = new DefaultMutableTreeNode();
        jTree1 = new JTree(root);
        ToolTipManager.sharedInstance().registerComponent(jTree1);
        jTree1.setRootVisible(false);
        FindTreeCellRenderer treeCellRenderer = new FindTreeCellRenderer();
        jTree1.setCellRenderer(treeCellRenderer);
        jTree1.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent evt) throws javax.swing.tree.ExpandVetoException {
            }

            public void treeWillExpand(javax.swing.event.TreeExpansionEvent evt) throws javax.swing.tree.ExpandVetoException {
                jTree1TreeWillExpand(evt);
            }
        });

        jTree1.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                find_field.requestFocus();
            }
        });

        jTree1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTree1MouseClicked(evt);
            }
        });

        jScrollPane1.setViewportView(jTree1);
        
        // full text search JList
        full_text_search_result = new JList<String>(dlm);  
//        full_text_search_result.setCellRenderer(new CustomListRenderer());

        full_text_search_result.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int lastIndex = e.getLastIndex();
                if(index_area_node.get(lastIndex) != null && index_area_node.get(lastIndex).length == 3) {
                    String item = index_area_node.get(lastIndex)[0];
                    String area = index_area_node.get(lastIndex)[1];
                    String node = index_area_node.get(lastIndex)[2];
    //                System.out.println(lastIndex+" - "+item+" "+area+" "+node);
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            new Utils().LoadAndPositionMap(area, node);
                        }
                    });
                    t.start(); 
    //                new Utils().LoadAndPositionMap(area, node);
                }

            }
        });        
        //////////////////////////////////////////////////        
        result_pannel.add(jScrollPane1);

        JPanel controls = new JPanel(new BorderLayout());
        controls.add(group_search_pannel, BorderLayout.NORTH);
        controls.add(result_pannel, BorderLayout.CENTER);
        controls.setBorder(new TitledBorder("Filters"));

        cp.add("Center", controls);

        // Show/hide the "Controls"
        toggle = new JButton(cp.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
        toggle.setText("-");
        toggle.setPreferredSize(new Dimension(0, c.getSize().height));

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add("Center", toggle);
        panel.add("West", cp);

        c.add("West", panel);
        toggle.doClick();
        ////////////////////////////////////////////////////////////

        setVisible(true);

    }

    private void formMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
        int notches = evt.getWheelRotation();
        if (notches < 0) {
            Utils.ZoomOut();
        } else {
            Utils.ZoomIn();
        }
    }

    private void jTextField1KeyTyped(java.awt.event.KeyEvent evt) {
        if (String.valueOf(evt.getKeyChar()).equals("\n")) {
//            System.out.println("Enter!!!");
            FindAction();
        }
    }

    private void FindAction() {
        Main.isBusy = true;
        WaitCircleApplicationIcon waitCircleApplicationIcon = new WaitCircleApplicationIcon();
        waitCircleApplicationIcon.end = false;
        waitCircleApplicationIcon.start(); 
        Main.control_panel.SetDisable();
        Main.time_machine.SetDisable();

        String find_str = find_field.getText();
        DefaultTreeModel model = (DefaultTreeModel) jTree1.getModel();
        DefaultMutableTreeNode root_node = (DefaultMutableTreeNode) model.getRoot();
        root_node.removeAllChildren();
        model.reload();
        
        find_field.setBackground(Color.WHITE);
        find_field.selectAll();
        find_field.requestFocus();

        //=========================================================
        // Now search
        //=========================================================
        boolean full_text_search = false;
        try {
            if (ipButton.isSelected()) {
                find_str = find_str.trim();
                find_field.setText(find_str);
                if (new Utils().VerifyIpAdress(find_str)) {
                    find_str = find_str.replace(":", "");
                } else {
                    String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/ipbyname?name=" + find_str;
                    find_str = utils.HTTPRequestGET(url);

                    if (!find_str.equals("")) {
                        find_field.setText(find_str);
                    } else {
                        find_field.setBackground(new Color(255, 150, 150));
                        find_field.selectAll();
                        find_field.requestFocus();                        
                    }
                }
            } else if (macButton.isSelected()) {
                find_str = find_str.toLowerCase();
            } else if (sysnameButton.isSelected()) {
//                find_str = find_str.replace(":", "\\:");
                full_text_search = true;
            }
            if(!full_text_search) {
                String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/find?key=" + find_str;
                String result = utils.HTTPRequestGET(url);
                jScrollPane1.setViewportView(jTree1);
                
                if (!result.equals("")) {
                    find_result_list = (ArrayList<ArrayList<String>>) utils.StrToObject(result);

                    for (ArrayList<String> item : find_result_list) {
                        DefaultMutableTreeNode tree_node = new DefaultMutableTreeNode(item.get(0));
                        DefaultMutableTreeNode tree_node_leaf = new DefaultMutableTreeNode();
                        tree_node.add(tree_node_leaf);
                        root.add(tree_node);
                    }
                    jTree1.setRootVisible(true);
                    jTree1.expandRow(0);
                    jTree1.setRootVisible(false);
                    jTree1.expandRow(0);
                    jTree1.repaint();
                }
            } else {
                Main.neb_server = "localhost";
                String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/find_full_text?key=" + URLEncoder.encode(find_str, "UTF-8");
                String result = utils.HTTPRequestGET(url);
                dlm.clear();
                index_area_node = new HashMap();
                jScrollPane1.setViewportView(full_text_search_result);
                if (!result.equals("")) {
                    String[] lines = result.split("\n");
                    int i = 0;
                    for(String line : lines) {
                        String[] mas = line.split(";", -1);
                        if(mas.length == 4) {
                            dlm.addElement(mas[0]+" - "+mas[3]+"("+mas[2]+")");
                            index_area_node.put(i, new String[] { mas[0], mas[1], mas[2] });
                            i = i + 1;
                        }
                        
                    }
//                    System.out.println(result);
                }
            }
        } catch (Exception ee) {
            System.out.println("Error searching " + find_str + " : " + ee.getMessage());
        }
        waitCircleApplicationIcon.end = true;
        Main.control_panel.SetEnable();
        Main.time_machine.SetEnable();
        Main.isBusy = false;
    }

    private void jTree1TreeWillExpand(javax.swing.event.TreeExpansionEvent evt) throws javax.swing.tree.ExpandVetoException {
        find_field.requestFocus();
        for (int i = 0; i < jTree1.getRowCount(); i++) {
            jTree1.collapseRow(i);
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) evt.getPath().getLastPathComponent();
        if (node.getChildAt(0).toString().equals("")) {
            node.remove(0);
        }
        if (node.getUserObject() != null) {
            String item_name = node.getUserObject().toString();
            for (ArrayList<String> item : find_result_list) {
                if (item.get(0).equals(item_name)) {
                    String out_str = item.get(3) + " ---> " + item.get(4) + " " + item.get(5);
                    DefaultMutableTreeNode leaf_node = new DefaultMutableTreeNode(out_str);
                    node.add(leaf_node);
                }
            }
            jTree1.repaint();

        }
    }

    private void jTextField1MouseClicked(java.awt.event.MouseEvent evt) {
        find_field.selectAll();
    }

    private void jTree1MouseClicked(java.awt.event.MouseEvent evt) {
        int selRow = jTree1.getRowForLocation(evt.getX(), evt.getY());
        TreePath selPath = jTree1.getSelectionPath();
        if (selPath != null && selPath.getPath().length == 3) {
            if (selRow != -1) {
                if (evt.getClickCount() == 2) {
                    new Utils().LoadAndPositionMapFromTree(selPath);
                }
            }
        }
        find_field.requestFocus();
    }
    
    private void full_text_search_result_MouseClicked(java.awt.event.MouseEvent evt) {
        
        System.out.println("11111111");

    }            

/////////////////// Main //////////////////////////////////////////////////
    public static void main(String[] args) {
        // add shutdown hook
//        ShutdownHook shutdownHook = new ShutdownHook();
//        Runtime.getRuntime().addShutdownHook(shutdownHook);

//        Utils utils = new Utils();
//        // Get home path
//        home = utils.GetHomePath();
//
//        // Get home path
//        String program = System.getProperty("java.class.path");
//        String str[] = program.split("\\;");
//        for (int i = 0; i < str.length; i++) {
//            String program_rep = str[i].replace("\\", "/");
//            String[] mas = program_rep.split("/");
//            String home1 = "";
//            for (int j = 0; j < mas.length - 1; j++) {
//                home1 = home1 + mas[j] + "/";
//            }
//            File neb_cfg_descr = new File(home1 + "Neb_viewer.jar");
//            if (neb_cfg_descr.exists()) {
//                home = home1;
//                break;
//            }
//        }
//
//        System.out.println("home=" + home);

//        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        System.out.println("path=" + path);

        // get images files
        utils.GetImagesFiles(image_path_relative, path);    

        // read config file
        cfg = utils.ReadConfig(neb_cfg);
        cfg_server = utils.ReadConfig(nebserver_cfg);
        map_file = (String) cfg.get("map_file");
        canvas_color = (String) cfg.get("canvas_color");
        history_path_short = (String) cfg.get("history_map");

        map_filename = map_file;
//        map_base = home + map_file;
//        indexLocation = home + "/Index";
        history_path = home + history_path_short;

//        neb_cfg = home + neb_cfg;
//        dump = home + dump;
        run_map_viewer = true;

        control_panel = new ControlPanel();

        m_Main = new Main();
        m_Main.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent ev) {
                    if (isChanged) {
                        Frame frame = Main.getFrames()[0];
                        int n = JOptionPane.showConfirmDialog(frame, "Would you like save changed ???", "Save changed ???", JOptionPane.YES_NO_OPTION);
                        if (n == 0) {
                            Utils.SaveChanged(map_file, ControlPanel.area_select);
                        }
                        System.exit(0);
                    } else {
                        System.exit(0);
                    }
                }
            }
        );
        
////////////////////////////////////////////////////////
        WatchMapFile watchMapFile = new WatchMapFile();
        watchMapFile.start();   
///////////////////////////////////////////////////////        
    }
}


class NodeEventHandler extends PDragSequenceEventHandler {

    public NodeEventHandler() {
        getEventFilter().setMarksAcceptedEventsAsHandled(true);
    }

    @Override
    public void mouseEntered(PInputEvent e) {
        if (e.getButton() == 0) {
//            e.getPickedNode().setPaint(Color.red);
            Utils.paintToolTipNode(e);
            Utils.updateConturNode(e, true);
        }
    }

    @Override
    public void mouseMoved(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.paintToolTipNode(e);
        }
    }

    @Override
    public void mouseExited(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.hideToolTip();
            Utils.updateConturNode(e, false);
        }
    }

    @Override
    public void drag(PInputEvent e) {
        Utils.StopAllFlashLinkProcesses();
        Utils.hideFlashLinks();
        Utils.hideToolTip();
        Utils.updateConturNode(e, true);
        PNode node = e.getPickedNode();

        node.translate(e.getDelta().width, e.getDelta().height);

        PText text = (PText) node.getAttribute("text");

        text.translate(e.getDelta().width, e.getDelta().height);
        Utils.RepaintEdge(e.getPickedNode());
        Main.isChanged = true;
        ControlPanel.jButton1.setEnabled(true);
    }

    @Override
    public void mouseClicked(PInputEvent e) {
        if (Main.isModeEdit) {
            if (e.isRightMouseButton()) {
                Point2D ePos;
                ePos = e.getCanvasPosition();
                PNode node = e.getPickedNode();
                String isCustom = null;
                if (node != null) {
                    Main.node_sel = node;
                    isCustom = (String) Main.node_sel.getAttribute("custom");
                } //                if(isCustom != null)
                //                {
                //                    new Utils().addNodeContextMenuCustom();
                //                    Main.contextNodeMenuCustom.show(Main.canvas, (int) ePos.getX(),
                //                        (int) ePos.getY());
                //                }
                else {
                    new Utils().addNodeContextMenu();
                    Main.contextNodeMenu.show(Main.canvas, (int) ePos.getX(),
                            (int) ePos.getY());

                }
                /* We don't want any other interpretations of this
                        mouse event. */
                e.setHandled(true);
            }
            if (e.isLeftMouseButton()) {
                Point2D ePos;
                ePos = e.getCanvasPosition();
                PNode node = e.getPickedNode();
                if (node != null) {
                    Main.node_sel = node;

                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (GetNodePropertiesForm.frame != null) {
                                GetNodePropertiesForm.frame.dispose();
                                GetNodePropertiesForm.frame = null;
                            }
                            GetNodePropertiesForm.createGetNodePropertiesForm();
                        }
                    });

                }

            }
        }
    }
}

class NodeEventHandlerView extends PDragSequenceEventHandler {

    public NodeEventHandlerView() {
        getEventFilter().setMarksAcceptedEventsAsHandled(true);
    }

    @Override
    public void mouseEntered(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.paintToolTipNode(e);
            Utils.updateConturNode(e, true);
        }
    }

    @Override
    public void mouseMoved(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.paintToolTipNode(e);
        }
    }

    @Override
    public void mouseExited(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.hideToolTip();
            Utils.updateConturNode(e, false);
        }
    }

    @Override
    public void mouseClicked(PInputEvent e) {
        if (e.isLeftMouseButton()) {
            Point2D ePos;
            ePos = e.getCanvasPosition();
            PNode node = e.getPickedNode();
            if (node != null) {
                Main.node_sel = node;

                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (GetNodePropertiesForm.frame != null) {
                            GetNodePropertiesForm.frame.dispose();
                            GetNodePropertiesForm.frame = null;
                        }
                        GetNodePropertiesForm.createGetNodePropertiesForm();
                    }
                });

            }
        }
        if (e.isRightMouseButton()) {
            Point2D ePos;
            ePos = e.getCanvasPosition();
            PNode node = e.getPickedNode();
            String isCustom = null;
            if (node != null) {
                Main.node_sel = node;
                isCustom = (String) Main.node_sel.getAttribute("custom");
            }
            if (isCustom == null) {
                new Utils().addNodeContextMenuView();
                Main.contextNodeMenuView.show(Main.canvas, (int) ePos.getX(),
                        (int) ePos.getY());
            }

            /* We don't want any other interpretations of this
                        mouse event. */
            e.setHandled(true);
        }

    }

}

class EdgeEventHandler extends PDragSequenceEventHandler {

    public EdgeEventHandler() {
//        getEventFilter().setMarksAcceptedEventsAsHandled(true);
    }

    @Override
    public void mouseEntered(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.paintToolTipEdge(e);
            Utils.updateConturEdge(e, true);

            PNode n = e.getInputManager().getMouseOver().getPickedNode();
            if (Main.m_ProcessHideFlashLinks != null) {
                Main.m_ProcessHideFlashLinks.stop();
                Main.m_ProcessHideFlashLinks = null;
            }
            String edge_tooltip = (String) n.getAttribute("tooltip");
            Utils.StopAllFlashLinkProcesses();
            Main.m_ProcessPaintFlashLinks = new ProcessPaintFlashLinks(e);
            Main.m_ProcessPaintFlashLinks.start();
        }
    }

    @Override
    public void mouseMoved(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturEdge(e, true);
//            updateToolTip(e);
        }
    }

    @Override
    public void mouseExited(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.hideToolTip();
            Utils.updateConturEdge(e, false);
            if (Main.m_ProcessHideFlashLinks != null) {
                Main.m_ProcessHideFlashLinks.stop();
                Main.m_ProcessHideFlashLinks = null;
            }

            Main.m_ProcessHideFlashLinks = new ProcessHideFlashLinks();
            Main.m_ProcessHideFlashLinks.start();
            Main.mouse_entered_edge_prev = Main.mouse_entered_edge_cur;
        }
    }

    @Override
    public void mouseClicked(PInputEvent e) {
        if (e.isLeftMouseButton()) {
            double w_start = Main.canvas.getCamera().getGlobalBounds().getWidth();
            double w_stop = e.getPickedNode().getGlobalBounds().getWidth();
            Main.camera.setViewScale(w_start / w_stop);
//            updateToolTip(e);
            double x = e.getPickedNode().getGlobalBounds().getX();
            double y = e.getPickedNode().getGlobalBounds().getY();
            double width = e.getPickedNode().getGlobalBounds().getWidth();
            double height = e.getPickedNode().getGlobalBounds().getHeight();
            PBounds bound = new PBounds();
            bound.setRect(x - 20, y - 20, width + 40, height + 40);
            Main.canvas.getCamera().animateViewToCenterBounds(bound, true, 500);
            Main.canvas.getCamera().repaint();
            Utils.repaintText();
            Utils.repaintInterfaceName();
            Utils.hideToolTip();
            Utils.hideFlashLinks();
//            Utils.RepaintFlash(e);
        }

        e.setHandled(true);
    }
}

class EdgeEventHandlerView extends PDragSequenceEventHandler {

    public EdgeEventHandlerView() {
//        getEventFilter().setMarksAcceptedEventsAsHandled(true);
    }

    @Override
    public void mouseEntered(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.paintToolTipEdge(e);
            Utils.updateConturEdge(e, true);
            PNode n = e.getInputManager().getMouseOver().getPickedNode();
            if (Main.m_ProcessHideFlashLinks != null) {
                Main.m_ProcessHideFlashLinks.stop();
                Main.m_ProcessHideFlashLinks = null;
            }
            String edge_tooltip = (String) n.getAttribute("tooltip");
            Utils.StopAllFlashLinkProcesses();
            Main.m_ProcessPaintFlashLinks = new ProcessPaintFlashLinks(e);
            Main.m_ProcessPaintFlashLinks.start();
        }
    }

    @Override
    public void mouseMoved(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturEdge(e, true);
        }
    }

    @Override
    public void mouseExited(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.hideToolTip();
            Utils.updateConturEdge(e, false);
            if (Main.m_ProcessHideFlashLinks != null) {
                Main.m_ProcessHideFlashLinks.stop();
                Main.m_ProcessHideFlashLinks = null;
            }

            Main.m_ProcessHideFlashLinks = new ProcessHideFlashLinks();
            Main.m_ProcessHideFlashLinks.start();
            Main.mouse_entered_edge_prev = Main.mouse_entered_edge_cur;
        }
    }

    @Override
    public void mouseClicked(PInputEvent e) {
        if (e.isLeftMouseButton()) {
            double w_start = Main.canvas.getCamera().getGlobalBounds().getWidth();
            double w_stop = e.getPickedNode().getGlobalBounds().getWidth();
            Main.camera.setViewScale(w_start / w_stop);
            double x = e.getPickedNode().getGlobalBounds().getX();
            double y = e.getPickedNode().getGlobalBounds().getY();
            double width = e.getPickedNode().getGlobalBounds().getWidth();
            double height = e.getPickedNode().getGlobalBounds().getHeight();
            PBounds bound = new PBounds();
            bound.setRect(x - 20, y - 20, width + 40, height + 40);
            Main.canvas.getCamera().animateViewToCenterBounds(bound, true, 500);
            Main.canvas.getCamera().repaint();
            Utils.repaintText();
            Utils.repaintInterfaceName();
            Utils.hideToolTip();
            Utils.hideFlashLinks();
            Utils.RepaintFlash(e);
        }
    }
}

class TextEventHandler extends PDragSequenceEventHandler {

    public TextEventHandler() {
        getEventFilter().setMarksAcceptedEventsAsHandled(true);
    }

    @Override
    public void mouseEntered(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturText(e, true);
        }
    }

    @Override
    public void mouseMoved(PInputEvent e) {
        if (e.getButton() == 0) {

        }
    }

    @Override
    public void mouseExited(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturText(e, false);
        }
    }

    @Override
    public void drag(PInputEvent e) {
        if (Main.isModeEdit) {
            Utils.updateConturText(e, true);
            PNode node = e.getPickedNode();

            node.translate(e.getDelta().width, e.getDelta().height);
            Main.isChanged = true;
            ControlPanel.jButton1.setEnabled(true);
        }
    }

    @Override
    public void mouseClicked(PInputEvent e) {
        if (Main.isModeEdit) {
            if (e.isRightMouseButton()) {
                Point2D ePos;
                ePos = e.getCanvasPosition();
                PNode node = e.getPickedNode();
                Main.node_sel = node;
                Main.contextTextMenu.show(Main.canvas, (int) ePos.getX(),
                        (int) ePos.getY());

                e.setHandled(true);
            }
        }
    }
}

class TextEventHandlerView extends PDragSequenceEventHandler {

    public TextEventHandlerView() {
        getEventFilter().setMarksAcceptedEventsAsHandled(true);
    }

    @Override
    public void mouseEntered(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturText(e, true);
        }
    }

    @Override
    public void mouseMoved(PInputEvent e) {
        if (e.getButton() == 0) {

        }
    }

    @Override
    public void mouseExited(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturText(e, false);
        }
    }
}

class TextCustomEventHandler extends PDragSequenceEventHandler {

    public TextCustomEventHandler() {
        getEventFilter().setMarksAcceptedEventsAsHandled(true);
    }

    @Override
    public void mouseEntered(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturText(e, true);
        }
    }

    @Override
    public void mouseMoved(PInputEvent e) {
        if (e.getButton() == 0) {

        }
    }

    @Override
    public void mouseExited(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturText(e, false);
        }
    }

    @Override
    public void drag(PInputEvent e) {
        if (Main.isModeEdit) {
            Utils.updateConturText(e, true);
            PNode node = e.getPickedNode();

            node.translate(e.getDelta().width, e.getDelta().height);
            Main.isChanged = true;
            ControlPanel.jButton1.setEnabled(true);
        }
    }

    @Override
    public void mouseClicked(PInputEvent e) {
        if (Main.isModeEdit) {
            if (e.isRightMouseButton()) {
                Point2D ePos;
                ePos = e.getCanvasPosition();
                PNode node = e.getPickedNode();
                Main.node_sel = node;
                Main.contextTextCustomMenu.show(Main.canvas, (int) ePos.getX(),
                        (int) ePos.getY());

                e.setHandled(true);
            }
        }
    }

}

class TextCustomEventHandlerView extends PDragSequenceEventHandler {

    public TextCustomEventHandlerView() {
        getEventFilter().setMarksAcceptedEventsAsHandled(true);
    }

    @Override
    public void mouseEntered(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturText(e, true);
        }
    }

    @Override
    public void mouseMoved(PInputEvent e) {
        if (e.getButton() == 0) {

        }
    }

    @Override
    public void mouseExited(PInputEvent e) {
        if (e.getButton() == 0) {
            Utils.updateConturText(e, false);
        }
    }
}

class PrintUtilities implements Printable {

    private Component componentToBePrinted;

    public static void printComponent(Component c) {
        new PrintUtilities(c).print();
    }

    public PrintUtilities(Component componentToBePrinted) {
        this.componentToBePrinted = componentToBePrinted;
    }

    public void print() {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        PageFormat format = printJob.defaultPage();
        format.setOrientation(PageFormat.LANDSCAPE);
        printJob.setPrintable(this, format);
        if (printJob.printDialog()) {
            try {
                printJob.print();
            } catch (PrinterException pe) {
                System.out.println("Error printing: " + pe);
            }
        }
    }

    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) {
            return (NO_SUCH_PAGE);
        } else {
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2d.scale(pageFormat.getImageableWidth() / Main.canvas.getWidth(), pageFormat.getImageableHeight() / Main.canvas.getHeight());
//      g2d.setClip((int)pageFormat.getImageableX(), (int)pageFormat.getImageableY(), (int)pageFormat.getImageableHeight(), (int)pageFormat.getImageableWidth());
            disableDoubleBuffering(componentToBePrinted);
            componentToBePrinted.paint(g2d);
            enableDoubleBuffering(componentToBePrinted);
            return (PAGE_EXISTS);
        }
    }

    public static void disableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(false);
    }

    public static void enableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(true);
    }
}

class ShutdownHook extends Thread {

    @Override
    public void run() {
//        System.out.println("run_map_viewer="+Main.run_map_viewer);
//        if(Main.run_map_viewer) Utils.DeleteFlagFile(Main.run_file);
    }
}

class ListenerActionRadioButton implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        Main.ipButton.setSelected(false);
        Main.macButton.setSelected(false);
        Main.sysnameButton.setSelected(false);

        Main.find_field.requestFocus();
//        find_field.setFormatterFactory(new DefaultFormatterFactory());
        Main.find_field.setText("");

        if (e.getActionCommand().equals("Ip / Name")) {
            Main.ipButton.setSelected(true);
        }
        if (e.getActionCommand().equals("Mac")) {
//            MaskFormatter mac_format=null;
//            try {
//                mac_format = new MaskFormatter("HH:HH:HH:HH:HH:HH");
//            } catch (ParseException ex) {
//                java.util.logging.Logger.getLogger(ListenerActionRadioButton.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            mac_format.setAllowsInvalid(true);
//            DefaultFormatterFactory factory = new DefaultFormatterFactory(mac_format);            
//            find_field.setFormatterFactory(factory);    
            Main.macButton.setSelected(true);
        }
        if (e.getActionCommand().equals("FullText")) {
            Main.sysnameButton.setSelected(true);
        }

//        System.out.println("Нажатие кнопки! От - "+ 
//                            e.getActionCommand() + "\n");
    }
}

class HexDocument extends PlainDocument {

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if (Main.macButton.isSelected()) {
//            System.out.println("str="+str+"   offs="+offs);

            if (str.length() == 1) {
                char chr = str.toCharArray()[0];
                if (offs == 2 || offs == 5 || offs == 8 || offs == 11 || offs == 14) {
                    if (chr == ' ' || chr == ':' || chr == '-') {
                        super.insertString(offs, ":", a);
                    } else {
                        java.awt.Toolkit.getDefaultToolkit().beep();
                    }
                } else {
                    if (Character.isDigit(chr) || (chr >= 'A' && chr <= 'F') || (chr >= 'a' && chr <= 'f')) {
                        super.insertString(offs, str.toUpperCase(), a);
                    } else {
                        java.awt.Toolkit.getDefaultToolkit().beep();
                    }
                }
            } else if (str.matches("^.*([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}).*$") || str.matches("^.*([0-9A-Fa-f]{2}\\s){5}([0-9A-Fa-f]{2}).*$")) {
                str = str.trim().toUpperCase().replace("-", ":").replace(" ", ":");
                super.insertString(0, str, a);
            } else if (str.matches("^.*([0-9A-Fa-f]{4}\\.){2}([0-9A-Fa-f]{4}).*$")) {
                char[] chr = str.toCharArray();
                String out = String.valueOf(chr[0]) + String.valueOf(chr[1]) + ":"
                        + String.valueOf(chr[2]) + String.valueOf(chr[3]) + ":"
                        + String.valueOf(chr[5]) + String.valueOf(chr[6]) + ":"
                        + String.valueOf(chr[7]) + String.valueOf(chr[8]) + ":"
                        + String.valueOf(chr[10]) + String.valueOf(chr[11]) + ":"
                        + String.valueOf(chr[12]) + String.valueOf(chr[13]);
                out = out.trim().toUpperCase();
                super.insertString(0, out, a);
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } else {
            super.insertString(offs, str, a);
        }
    }
}

class WatchMapFile extends Thread {
    public static String my_changed = "";
    public Utils utils = new Utils();
//    public static long prev_time_file_map = 0;
//    public static long time_file_map = 0;
//    private String filename;
    
    public WatchMapFile() {
//        this.filename = filename;
    }    
    public void run() {
        long prev_time_file_map = 0;
        String filename_prev = null;
        long start_my_changed = 0;
        long cur_my_changed = 0;
        long timeout_my_changed = 3*60*1000;
        while(true) {
//            System.out.println("my_changed = "+my_changed);
            if(!Main.isBusy) {
                if(filename_prev != null && !filename_prev.equals(Main.map_filename))
                    prev_time_file_map = 0;
                filename_prev = Main.map_filename;
                String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/getfile_attributes?file=" + Main.map_filename;
                long time_file_map = GetModifyTime(url);
//                System.out.println(prev_time_file_map+":"+time_file_map+" - "+filename_prev+" my_changed="+my_changed);
//                if (prev_time_file_map != 0 && time_file_map != prev_time_file_map)
//                    System.out.println("1111111");
                if (prev_time_file_map != 0 && time_file_map != prev_time_file_map && !my_changed.equals(Main.map_filename)) {             
//                    File file = new File(Main.map_filename);
                    try { Thread.sleep(60*1000); } catch(java.lang.InterruptedException e) {}
                    System.out.println("Start reload "+Main.map_filename+" file.");
                    Main.isBusy = true;
                    boolean selector_end = false;
                    if(TimeMachineForm.selector == Main.history_list.size()-1)
                        selector_end=true;
                    Main.history_list = utils.GetListMapFiles();
                    if(selector_end)
                        TimeMachineForm.selector = Main.history_list.size()-1;
                    Main.utils.SetTimeMachine(Main.history_list, TimeMachineForm.selector, ControlPanel.area_select);
//                    Main.utils.LoadNewMap(Main.map_filename, ControlPanel.area_select);
                    Main.isBusy = false;
//                    System.out.println("=== "+Main.map_filename+" "+ControlPanel.area_select);
//                    System.out.println("Stop reload "+Main.map_filename+" file.");
                }
                
                if(!my_changed.equals("")) {
                    if(start_my_changed == 0) {
                        start_my_changed = System.currentTimeMillis();
                        cur_my_changed = System.currentTimeMillis();
                    } else {
                        cur_my_changed = System.currentTimeMillis();
                    }
                }

                if(!my_changed.equals("") && cur_my_changed-start_my_changed > timeout_my_changed) {
                    start_my_changed = 0;
                    cur_my_changed = 0;
                    my_changed = "";
//                    System.out.println("Clear my changed.");
                }
                prev_time_file_map=time_file_map;
            } 
//            else prev_time_file_map = 0;
           

            try { Thread.sleep(60*1000); } catch(java.lang.InterruptedException e) {}
        }
    }
    
    public long GetModifyTime(String url) {
        long out = 0;
        String result = Utils.HTTPRequestGET(url);
        JSONParser parser = new JSONParser();
        if (!result.equals("")) {
            try {
                JSONObject jsonObject = (JSONObject) parser.parse(result);
                Map file_attributes = Main.utils.toMap(jsonObject);
                if(file_attributes.get("modify_time") != null)
                    out = Long.valueOf((String)file_attributes.get("modify_time"));
            } catch (NumberFormatException | ParseException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Error request.");
        } 
        return out;
    }
}

//class CustomListRenderer implements ListCellRenderer {
//
//   @Override
//   public Component getListCellRendererComponent(JList list, Object value, int index,
//        boolean isSelected, boolean cellHasFocus) {
//
//        JTextArea renderer = new JTextArea(3,10);
//        renderer.setText(value.toString());
//        renderer.setLineWrap(true);
//        return renderer;
//   }
//}