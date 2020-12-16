package ru.kos.neb_viewer;

/**
 *
 * @author kos
 */
import java.applet.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import org.piccolo2d.PLayer;
import org.piccolo2d.PNode;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.nodes.PPath;


public class FlashColor extends
  Applet implements Runnable
{
  Thread m_AnimateTask = null;
  public long time1 = 100;
  public long time2 = 100;

  private PPath node;
  private Color color1;
  private Color color2;

  private Random random = new Random();

  public FlashColor(PPath node, Color color1, Color color2)
  {
//      System.out.println("Set up flashing");
      this.color1=color1;
      this.color2=color2;
      this.node=node;
  }

    public void run()
    {
        while (true)
        {
            double dolja=Math.abs(random.nextDouble());
            node.setPaint(color1);
            node.repaint();
            try { Thread.sleep((long)(time1*dolja)); } catch (InterruptedException e) {}
            node.setPaint(color2);
            node.repaint();
            try { Thread.sleep((long)(time2*dolja)); } catch (InterruptedException e) {}

        }
    }

    public void start()
    {
        if (m_AnimateTask == null)
        {
            m_AnimateTask = new Thread(this);
            m_AnimateTask.start();
        }
    }

    public void stop()
    {
        if (m_AnimateTask != null)
        {
            node.setPaint(color1);
//            System.out.println("End!!!");
            m_AnimateTask.stop();
            m_AnimateTask = null;
        }
    }
}
