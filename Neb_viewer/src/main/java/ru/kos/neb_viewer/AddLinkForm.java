package ru.kos.neb_viewer;

/**
 *
 * @author kos
 */
import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import javax.swing.JPanel;
import java.net.URL;

import org.piccolo2d.nodes.PText;
import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;

@SuppressWarnings({"unchecked", "serial"})
class AddLinkForm extends JPanel
{
    static private JFrame frame;
    
    static private JTextField fromIp;
    static private JTextField fromPort;
    static private JTextField toIp;
    static private JTextField toPort;
    
//    JLabel result;
//    String currentPattern;
    
    public AddLinkForm(String from_ip)
    {
        initComponents(from_ip);
    }
        
    private void initComponents(String from_ip)
    {
        JPanel jPanel1 = new javax.swing.JPanel();
        fromIp = new javax.swing.JTextField();
        fromPort = new javax.swing.JTextField();
        JLabel jLabel1 = new javax.swing.JLabel();
        JLabel jLabel2 = new javax.swing.JLabel();
        JPanel jPanel2 = new javax.swing.JPanel();
        toIp = new javax.swing.JTextField();
        toPort = new javax.swing.JTextField();
        JLabel jLabel3 = new javax.swing.JLabel();
        JLabel jLabel4 = new javax.swing.JLabel();
        JPanel jPanel3 = new javax.swing.JPanel();
        JButton jButton1 = new javax.swing.JButton();
        JButton jButton2 = new javax.swing.JButton();

        setBackground(new java.awt.Color(220, 220, 190));
        jPanel1.setBackground(new java.awt.Color(220, 220, 190));
        jPanel2.setBackground(new java.awt.Color(220, 220, 190));
        jPanel3.setBackground(new java.awt.Color(220, 220, 190));
        jButton1.setBackground(new java.awt.Color(220, 220, 190));
        jButton2.setBackground(new java.awt.Color(220, 220, 190));

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        getAccessibleContext().setAccessibleName("");
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("From: "));

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel1.setText("From Ip address: ");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel2.setText("From port name: ");

        fromIp.setText(from_ip); 
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(22, 22, 22)
                        .add(fromIp, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 146, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(20, 20, 20)
                        .add(jLabel1)))
                .add(21, 21, 21)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(fromPort, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(fromPort, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(fromIp, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(77, 77, 77))
        );
        add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 30, 400, 80));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("To: "));

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel3.setText("From Ip address: ");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel4.setText("From port name: ");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(19, 19, 19)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(toIp, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 148, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .add(19, 19, 19)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(toPort, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                    .add(jLabel4))
                .add(7, 7, 7))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(jLabel4))
                .add(16, 16, 16)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(toIp, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(toPort, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 110, 400, 100));

        jButton1.setText("Ok");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                jButton1MouseClicked(evt);
            }
        });

        jButton2.setText("Cancel");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                frame.dispose();
            }
        });

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(247, Short.MAX_VALUE)
                .add(jButton1)
                .add(23, 23, 23)
                .add(jButton2)
                .add(20, 20, 20))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton2)
                    .add(jButton1))
                .add(20, 20, 20))
        );
        add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 210, 400, 40));
        
        
    }
    
    public static void createAddLinkForm(String from_ip) 
    {
        //Create and set up the window.
        frame = new JFrame("Add Link");
        
        //Create and set up the content pane.
        JComponent newContentPane = new AddLinkForm(from_ip);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
//        ImageIcon m_ImageIcon = new ImageIcon();
        URL url = Main.class.getResource("/Neb_viewer/images/idle.gif");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url));

        //Display the window.
        frame.pack();
        frame.setVisible(true);

    }
   
    
    private void jButton1MouseClicked(java.awt.event.MouseEvent evt)
    {
        int i;
        boolean isIp1_exist=false; int index1=0;
        for ( i = 0; i < Main.nodeLayer.getChildrenCount(); i++)
        {
            ArrayList texts = (ArrayList) Main.nodeLayer.getChild(i).getAttribute("text");
            if(texts != null)
            {
                PText text = (PText) texts.get(0);
                String node_name = text.getText();
                if(node_name.equals(fromIp.getText()))
                { isIp1_exist=true; break; }
            }
        }
        index1=i;
        boolean isIp2_exist=false; int index2=0;
        for ( i = 0; i < Main.nodeLayer.getChildrenCount(); i++)
        {
            ArrayList texts = (ArrayList) Main.nodeLayer.getChild(i).getAttribute("text");
            if(texts != null)
            {
                PText text = (PText) texts.get(0);
                String node_name = text.getText();
                if(node_name.equals(toIp.getText()))
                { isIp2_exist=true; break; }
            }
        }
        index2=i;
        if(!isIp1_exist)
        {
            Frame frame_dialog = Main.getFrames()[0];
            JOptionPane.showMessageDialog(frame_dialog,"Node name "+fromIp.getText()+" not found.","Error !!!",JOptionPane.WARNING_MESSAGE);
        }
        if(fromPort.getText().equals(""))
        {
            Frame frame_dialog = Main.getFrames()[0];
            JOptionPane.showMessageDialog(frame_dialog,"Enter From port","Error !!!",JOptionPane.WARNING_MESSAGE);
        }
        if(!isIp2_exist)
        {
            Frame frame_dialog = Main.getFrames()[0];
            JOptionPane.showMessageDialog(frame_dialog,"Node name "+toIp.getText()+" not found.","Error !!!",JOptionPane.WARNING_MESSAGE);
        }
        if(toPort.getText().equals(""))
        {
            Frame frame_dialog = Main.getFrames()[0];
            JOptionPane.showMessageDialog(frame_dialog,"Enter To port","Error !!!",JOptionPane.WARNING_MESSAGE);
        }
        if(isIp1_exist && isIp2_exist && !fromPort.getText().equals("") && !toPort.getText().equals(""))
        {
            PNode node1 = Main.nodeLayer.getChild(index1);
            PNode node2 = Main.nodeLayer.getChild(index2);
            if(node1 != null && node2 != null)
            {
                Point2D.Double bound1 = (Point2D.Double) node1.getFullBounds().getCenter2D();
                Point2D.Double bound2 = (Point2D.Double) node2.getFullBounds().getCenter2D();
                
                PPath edge = new PPath.Double();
                edge.moveTo((float) bound1.getX(), (float) bound1.getY());
                edge.lineTo((float) bound2.getX(), (float) bound2.getY());
                
                ArrayList tmp = (ArrayList) node1.getAttribute("edges");
                tmp.add(edge);
                tmp = (ArrayList) node2.getAttribute("edges");
                tmp.add(edge);
                tmp = new ArrayList();
                tmp.add(node1);
                tmp.add(node2);
                edge.addAttribute("nodes", tmp);
                tmp = new ArrayList();
                tmp.add(bound1);
                tmp.add(bound2);
                edge.addAttribute("coordinate", tmp);
                edge.addAttribute("tooltip", fromIp.getText()+" "+fromPort.getText()+" - "+toIp.getText()+" "+toPort.getText());
                tmp = new ArrayList();
                tmp.add(edge);
                edge.addAttribute("ppath",tmp);
                edge.addAttribute("width",1);
                edge.addAttribute("color",0.0);
                edge.addAttribute("custom","custom");
                Main.edgeLayer.addChild(edge);
                Main.edgeLayer.repaint();
                Main.isChanged=true;
                
                PText interf_name = new PText();
                tmp = new ArrayList();
                tmp.add(interf_name);
                edge.addAttribute("interf_name",tmp);
                Utils.UpdateInterfaceName(edge);
                Main.interfnameLayer.addChild(interf_name);
                
                
                ControlPanel.jButton1.setIcon(new ImageIcon(getClass().getResource("/Neb_viewer/images/save.png")));
                ControlPanel.jButton1.setVisible(true);
            }
        }
        
        frame.dispose();
    }
    
}    
